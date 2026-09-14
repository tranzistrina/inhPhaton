package ru.inh.gods;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class GodsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final String PREFIX = ChatColor.DARK_PURPLE + "[Gods] " + ChatColor.RESET;
    private PhantomManager manager;
    private GodsApi api;
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new PhantomManager(this);
        manager.reloadSettings();
        debug = getConfig().getBoolean("logging.debug", false);
        api = new GodsApi(manager);
        getServer().getServicesManager().register(GodsApi.class, api, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(this, this);
        registerGodCommand();
        manager.start();
        getLogger().info("Gods включён: персональная видимость и фантомы готовы.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.stop();
        getServer().getServicesManager().unregister(GodsApi.class, this);
    }

    public GodsApi api() {
        return api;
    }

    /**
     * Paper 26.2 запускает Paper plugins без Bukkit YAML-команд.
     * Регистрируем команду через официальный Paper API; игровая логика
     * по-прежнему использует только Bukkit-сущности, события и сервисы.
     */
    private void registerGodCommand() {
        registerCommand("god", List.of("gods", "inhPhaton"), new BasicCommand() {
            @Override
            public void execute(CommandSourceStack source, String[] args) {
                GodsPlugin.this.onCommand(source.getSender(), null, "god", args);
            }

            @Override
            public java.util.Collection<String> suggest(CommandSourceStack source, String[] args) {
                return GodsPlugin.this.onTabComplete(source.getSender(), null, "god", args);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(this, () -> manager.refreshPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTask(this, () -> manager.refreshPlayer(event.getPlayer()));
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Bukkit.getScheduler().runTask(this, () -> manager.refreshPlayer(event.getPlayer()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender, label);
            return true;
        }
        String root = args[0].toLowerCase(Locale.ROOT);
        switch (root) {
            case "phantom" -> phantomCommand(sender, args);
            case "visibility" -> visibilityCommand(sender, args);
            case "debug" -> debugCommand(sender, args);
            case "reload" -> reloadCommand(sender);
            case "list" -> listCommand(sender);
            default -> help(sender, label);
        }
        return true;
    }

    private void phantomCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("god.phantom")) {
            deny(sender);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "phantom create|remove|move|show|hide|showonly|hideall");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> createCommand(sender, args);
            case "remove" -> removeCommand(sender, args);
            case "move" -> moveCommand(sender, args);
            case "show", "hide" -> showHideCommand(sender, args, args[1].equalsIgnoreCase("show"));
            case "showonly" -> showOnlyCommand(sender, args);
            case "hideall" -> hideAllCommand(sender, args);
            default -> sender.sendMessage(PREFIX + ChatColor.YELLOW + "Неизвестная операция phantom.");
        }
    }

    private void createCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "Создание из консоли пока требует игрока-источника.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "/god phantom create <тип> [наблюдатель] [ttl-сек]");
            return;
        }
        EntityType type = parseEntityType(args[2]);
        if (type == null || !type.isAlive()) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Неизвестный живой тип сущности: " + args[2]);
            return;
        }
        Player observer = args.length >= 4 ? Bukkit.getPlayerExact(args[3]) : player;
        if (observer == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Игрок-наблюдатель не найден.");
            return;
        }
        long ttl = 0;
        if (args.length >= 5) {
            try { ttl = Long.parseLong(args[4]); } catch (NumberFormatException exception) {
                sender.sendMessage(PREFIX + ChatColor.RED + "TTL должен быть целым числом секунд.");
                return;
            }
        }
        try {
            UUID id = manager.createPhantom(type, player.getLocation(), player.getUniqueId(),
                    Set.of(observer.getUniqueId()), ttl, null);
            sender.sendMessage(PREFIX + ChatColor.GREEN + "Фантом создан: " + id);
        } catch (RuntimeException exception) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Не удалось создать фантом: " + exception.getMessage());
            if (debug) getLogger().warning(exception.toString());
        }
    }

    private void removeCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "/god phantom remove <id>");
            return;
        }
        UUID id = parseUuid(args[2]);
        if (id == null || !manager.removePhantom(id, "command")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Фантом не найден.");
            return;
        }
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Фантом удалён.");
    }

    private void moveCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 6) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "/god phantom move <id> <x> <y> <z> (из игры)");
            return;
        }
        UUID id = parseUuid(args[2]);
        Location location;
        try {
            location = new Location(player.getWorld(), Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]),
                    player.getLocation().getYaw(), player.getLocation().getPitch());
        } catch (NumberFormatException exception) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Координаты должны быть числами.");
            return;
        }
        sender.sendMessage(manager.movePhantom(id, location)
                ? PREFIX + ChatColor.GREEN + "Фантом перемещён."
                : PREFIX + ChatColor.RED + "Фантом не найден или перемещение не удалось.");
    }

    private void showHideCommand(CommandSender sender, String[] args, boolean show) {
        if (args.length < 4) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "/god phantom " + (show ? "show" : "hide") + " <id> <игрок>");
            return;
        }
        UUID id = parseUuid(args[2]);
        Player player = Bukkit.getPlayerExact(args[3]);
        if (player == null || id == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Фантом или игрок не найден.");
            return;
        }
        boolean changed = show ? manager.showTo(id, player.getUniqueId()) : manager.hideFrom(id, player.getUniqueId());
        sender.sendMessage(changed ? PREFIX + ChatColor.GREEN + "Готово." : PREFIX + ChatColor.RED + "Фантом не найден.");
    }

    private void showOnlyCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "/god phantom showonly <id> <игрок>");
            return;
        }
        UUID id = parseUuid(args[2]);
        Player player = Bukkit.getPlayerExact(args[3]);
        sender.sendMessage(id != null && player != null && manager.showOnlyTo(id, Set.of(player.getUniqueId()))
                ? PREFIX + ChatColor.GREEN + "Теперь фантом виден только этому игроку."
                : PREFIX + ChatColor.RED + "Фантом или игрок не найден.");
    }

    private void hideAllCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "/god phantom hideall <id>");
            return;
        }
        UUID id = parseUuid(args[2]);
        sender.sendMessage(id != null && manager.hideFromAll(id)
                ? PREFIX + ChatColor.GREEN + "Фантом скрыт от всех."
                : PREFIX + ChatColor.RED + "Фантом не найден.");
    }

    private void visibilityCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("god.visibility")) {
            deny(sender);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "/god visibility only|except|hidden <entity-id> [игрок]");
            return;
        }
        Entity entity = resolveEntity(args[2]);
        if (entity == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Сущность не найдена.");
            return;
        }
        if (args[1].equalsIgnoreCase("hidden")) {
            for (Player player : Bukkit.getOnlinePlayers()) player.hideEntity(this, entity);
            sender.sendMessage(PREFIX + ChatColor.GREEN + "Сущность скрыта от всех.");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "Укажи игрока.");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[3]);
        if (target == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Игрок не найден.");
            return;
        }
        if (args[1].equalsIgnoreCase("only")) {
            for (Player player : Bukkit.getOnlinePlayers()) player.hideEntity(this, entity);
            target.showEntity(this, entity);
            sender.sendMessage(PREFIX + ChatColor.GREEN + "Сущность видна только " + target.getName() + ".");
        } else if (args[1].equalsIgnoreCase("except")) {
            for (Player player : Bukkit.getOnlinePlayers()) player.showEntity(this, entity);
            target.hideEntity(this, entity);
            sender.sendMessage(PREFIX + ChatColor.GREEN + "Сущность скрыта от " + target.getName() + ".");
        } else {
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "Режим: only, except или hidden.");
        }
    }

    private void debugCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("god.debug")) {
            deny(sender);
            return;
        }
        debug = args.length >= 2 ? Boolean.parseBoolean(args[1]) : !debug;
        getConfig().set("logging.debug", debug);
        saveConfig();
        manager.reloadSettings();
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Debug: " + debug + "; активных фантомов: " + manager.activeCount());
    }

    private void reloadCommand(CommandSender sender) {
        if (!sender.hasPermission("god.reload")) {
            deny(sender);
            return;
        }
        reloadConfig();
        manager.reloadSettings();
        debug = getConfig().getBoolean("logging.debug", false);
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Конфигурация перезагружена; активные фантомы сохранены: " + manager.activeCount());
    }

    private void listCommand(CommandSender sender) {
        if (!sender.hasPermission("god.debug")) {
            deny(sender);
            return;
        }
        sender.sendMessage(PREFIX + ChatColor.AQUA + "Активных фантомов: " + manager.activeCount());
        for (Phantom phantom : manager.all()) {
            sender.sendMessage(ChatColor.GRAY + "- " + phantom.id + " " + phantom.type + " observers=" + phantom.observers.size()
                    + " at=" + phantom.location.getWorld().getName() + " " + phantom.location.getBlockX() + ","
                    + phantom.location.getBlockY() + "," + phantom.location.getBlockZ());
        }
    }

    private Entity resolveEntity(String token) {
        UUID uuid = parseUuid(token);
        if (uuid != null) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) return entity;
        }
        try {
            int numericId = Integer.parseInt(token);
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity.getEntityId() == numericId) return entity;
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private EntityType parseEntityType(String input) {
        try { return EntityType.valueOf(input.toUpperCase(Locale.ROOT).replace("MINECRAFT:", "")); }
        catch (IllegalArgumentException exception) { return null; }
    }

    private UUID parseUuid(String input) {
        try { return UUID.fromString(input); } catch (Exception ignored) { return null; }
    }

    private void deny(CommandSender sender) { sender.sendMessage(PREFIX + ChatColor.RED + "Недостаточно прав."); }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(PREFIX + ChatColor.AQUA + "Gods: phantom, visibility, debug, reload, list");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " phantom create <тип> [игрок] [ttl]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " phantom remove|move|show|hide|showonly|hideall ...");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " visibility only|except|hidden <entity-id> [игрок]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return partial(args[0], List.of("phantom", "visibility", "debug", "reload", "list"));
        if (args.length == 2 && args[0].equalsIgnoreCase("phantom")) {
            return partial(args[1], List.of("create", "remove", "move", "show", "hide", "showonly", "hideall"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("visibility")) return partial(args[1], List.of("only", "except", "hidden"));
        if (args.length == 3 && args[0].equalsIgnoreCase("phantom") && args[1].equalsIgnoreCase("create")) {
            return partial(args[2], Arrays.stream(EntityType.values()).filter(EntityType::isAlive).map(EntityType::name).toList());
        }
        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> values) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).sorted().toList();
    }
}
