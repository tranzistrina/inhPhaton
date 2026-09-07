package ru.khozain.inhphaton.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.khozain.inhphaton.InhPhatonPlugin;
import ru.khozain.inhphaton.phantom.PhantomInstance;
import ru.khozain.inhphaton.phantom.PhantomSpec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

public final class GodCommand implements CommandExecutor, TabCompleter {
    private static final Logger LOG = LoggerFactory.getLogger("inhPhaton.cmd");
    private final InhPhatonPlugin plugin; private final String label="god";
    private final List<String> aliases=Arrays.asList("gods","боги");
    public GodCommand(InhPhatonPlugin plugin){this.plugin=plugin;}
    public void register(){PluginCommand cmd=plugin.getCommand(label);if(cmd==null){cmd=(PluginCommand)Bukkit.getCommandMap().getCommand(label);}if(cmd!=null){cmd.setExecutor(this);cmd.setTabCompleter(this);}else LOG.warn("Command /{} is not registered in plugin.yml",label);}
    public void unregister(){PluginCommand cmd=plugin.getCommand(label);if(cmd!=null){cmd.setExecutor(null);cmd.setTabCompleter(null);}}
    @Override public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command,@NotNull String alias,@NotNull String[] args){
        if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_ADMIN)){sender.sendMessage("§cНет прав god.admin");return true;}
        if(args.length==0){sendHelp(sender);return true;} String sub=args[0].toLowerCase(Locale.ROOT);
        try{switch(sub){case "phantom"->handlePhantom(sender,Arrays.copyOfRange(args,1,args.length));case "visibility"->handleVisibility(sender,Arrays.copyOfRange(args,1,args.length));case "debug"->handleDebug(sender);case "reload"->handleReload(sender);case "list"->handleList(sender);default->sendHelp(sender);}}catch(Throwable t){sender.sendMessage("§cОшибка: "+t.getMessage());LOG.warn("command error: {}",t.getMessage(),t);}return true;
    }
    private void handlePhantom(CommandSender sender,String[] args){if(args.length==0){sender.sendMessage("§e/god phantom <create|remove|move|show|hide>");return;}if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_PHANTOM)){sender.sendMessage("§cНет прав god.phantom");return;}String sub=args[0].toLowerCase(Locale.ROOT);switch(sub){case "create"->phantomCreate(sender,Arrays.copyOfRange(args,1,args.length));case "remove"->phantomRemove(sender,Arrays.copyOfRange(args,1,args.length));case "move"->phantomMove(sender,Arrays.copyOfRange(args,1,args.length));case "show"->phantomShow(sender,Arrays.copyOfRange(args,1,args.length));case "hide"->phantomHide(sender,Arrays.copyOfRange(args,1,args.length));default->sender.sendMessage("§e/god phantom <create|remove|move|show|hide>");}}
    private void phantomCreate(CommandSender sender,String[] args){if(args.length<4){sender.sendMessage("§eИспользование: /god phantom create <type> <x> <y> <z> [world] [ttl]");return;}EntityType type;try{type=EntityType.valueOf(args[0].toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){sender.sendMessage("§cНеизвестный тип: "+args[0]);return;}double x,y,z;try{x=Double.parseDouble(args[1]);y=Double.parseDouble(args[2]);z=Double.parseDouble(args[3]);}catch(Exception ex){sender.sendMessage("§cКоординаты должны быть числами");return;}org.bukkit.World world;if(args.length>=5){world=Bukkit.getWorld(args[4]);if(world==null){sender.sendMessage("§cМир не найден: "+args[4]);return;}}else if(sender instanceof Player p){world=p.getWorld();}else{sender.sendMessage("§cНужно указать мир (5-й аргумент)");return;}long ttl=args.length>=6?parseLong(args[5],plugin.config().getDefaultTtlSeconds()):plugin.config().getDefaultTtlSeconds();UUID owner=sender instanceof Player p?p.getUniqueId():null;PhantomSpec spec=PhantomSpec.builder().entityType(type).location(new Location(world,x,y,z)).ttlSeconds(ttl).ownerId(owner).build();PhantomInstance inst=plugin.phantoms().create(spec);if(inst==null){sender.sendMessage("§cЛимит фантомов");return;}if(sender instanceof Player p)plugin.phantoms().showTo(inst.id(),p.getUniqueId());sender.sendMessage("§aФантом создан: §e"+inst.id());}
    private void phantomRemove(CommandSender sender,String[] args){if(args.length<1){sender.sendMessage("§e/god phantom remove <id>");return;}UUID id=parseUuid(args[0]);if(id==null){sender.sendMessage("§cUUID невалиден");return;}boolean ok=plugin.phantoms().remove(id);sender.sendMessage(ok?"§aУдалён":"§cНе найден");}
    private void phantomMove(CommandSender sender,String[] args){if(args.length<4){sender.sendMessage("§e/god phantom move <id> <x> <y> <z>");return;}UUID id=parseUuid(args[0]);if(id==null){sender.sendMessage("§cUUID невалиден");return;}try{double x=Double.parseDouble(args[1]),y=Double.parseDouble(args[2]),z=Double.parseDouble(args[3]);PhantomInstance inst=plugin.phantoms().get(id);if(inst==null){sender.sendMessage("§cНе найден");return;}Location loc=inst.spec().getInitialLocation().clone().add(x,y,z);plugin.phantoms().move(id,loc);sender.sendMessage("§aПеремещён: "+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ());}catch(Exception ex){sender.sendMessage("§cОшибка координат");}}
    private void phantomShow(CommandSender sender,String[] args){if(args.length<2){sender.sendMessage("§e/god phantom show <id> <player>");return;}UUID id=parseUuid(args[0]);if(id==null){sender.sendMessage("§cUUID невалиден");return;}Player p=Bukkit.getPlayerExact(args[1]);if(p==null){sender.sendMessage("§cИгрок не найден");return;}plugin.phantoms().showTo(id,p.getUniqueId());sender.sendMessage("§aПоказан "+p.getName());}
    private void phantomHide(CommandSender sender,String[] args){if(args.length<2){sender.sendMessage("§e/god phantom hide <id> <player>");return;}UUID id=parseUuid(args[0]);if(id==null){sender.sendMessage("§cUUID невалиден");return;}Player p=Bukkit.getPlayerExact(args[1]);if(p==null){sender.sendMessage("§cИгрок не найден");return;}plugin.phantoms().hideFrom(id,p.getUniqueId());sender.sendMessage("§aСкрыт от "+p.getName());}
    private void handleVisibility(CommandSender sender,String[] args){if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_VISIBILITY)){sender.sendMessage("§cНет прав god.visibility");return;}if(args.length<3){sender.sendMessage("§e/god visibility <only|except> <phantom-id> <player1,player2,...>");return;}String mode=args[0].toLowerCase(Locale.ROOT);UUID pid=parseUuid(args[1]);if(pid==null){sender.sendMessage("§cUUID невалиден");return;}Set<UUID> players=Arrays.stream(args[2].split(",")).map(Bukkit::getPlayerExact).filter(java.util.Objects::nonNull).map(Player::getUniqueId).collect(Collectors.toCollection(LinkedHashSet::new));if("only".equals(mode)){plugin.gods().showOnlyTo(pid,players);sender.sendMessage("§aONLY → "+players.size()+" players");}else if("except".equals(mode)){plugin.phantoms().showTo(pid,new HashSet<>());sender.sendMessage("§aEXCEPT → "+players.size()+" players hidden");}else sender.sendMessage("§cРежим: only|except");}
    private void handleDebug(CommandSender sender){if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_DEBUG)){sender.sendMessage("§cНет прав god.debug");return;}plugin.debug().toggle();sender.sendMessage("§aDebug = "+plugin.debug().isEnabled());}
    private void handleReload(CommandSender sender){if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_RELOAD)){sender.sendMessage("§cНет прав god.reload");return;}plugin.reload();sender.sendMessage("§aКонфигурация перечитана");}
    private void handleList(CommandSender sender){sender.sendMessage("§6=== inhPhaton: active phantoms ("+plugin.phantoms().size()+"/"+plugin.config().getGlobalPhantomLimit()+") ===");for(PhantomInstance p:plugin.phantoms().all())sender.sendMessage(String.format("§e- §f%s §7type=%s observers=%d owner=%s",p.id(),p.spec().getEntityType(),p.observers().size(),p.spec().getOwnerId()));}
    private void sendHelp(CommandSender sender){sender.sendMessage("§6=== inhPhaton — Боги ===");sender.sendMessage("§e/god phantom <create|remove|move|show|hide>");sender.sendMessage("§e/god visibility <only|except>");sender.sendMessage("§e/god debug");sender.sendMessage("§e/god reload");sender.sendMessage("§e/god list");}
    private static UUID parseUuid(String s){try{return UUID.fromString(s);}catch(IllegalArgumentException e){return null;}}
    private static long parseLong(String s,long d){try{return Long.parseLong(s);}catch(Exception e){return d;}}
    @Override public List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command command,@NotNull String alias,@NotNull String[] args){if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_ADMIN))return List.of();if(args.length==1)return Arrays.asList("phantom","visibility","debug","reload","list");if(args.length==2&&args[0].equalsIgnoreCase("phantom"))return Arrays.asList("create","remove","move","show","hide");if(args.length==2&&args[0].equalsIgnoreCase("visibility"))return Arrays.asList("only","except");if(args.length>=3&&args[0].equalsIgnoreCase("phantom")&&args[1].equalsIgnoreCase("create")){if(args.length==3)return Arrays.stream(EntityType.values()).filter(t->t.isSpawnable()&&t.isAlive()).map(t->t.name().toLowerCase(Locale.ROOT)).collect(Collectors.toList());if(args.length>=7)return List.of("[ttl-sec]");}if(args.length>=3&&args[0].equalsIgnoreCase("phantom")&&(args[1].equalsIgnoreCase("show")||args[1].equalsIgnoreCase("hide")))return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());return List.of();}
}