package ru.khozain.inhphaton.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public final class GodCommand implements BasicCommand {
    private static final Logger LOG=LoggerFactory.getLogger("inhPhaton.cmd");
    private final InhPhatonPlugin plugin;
    public GodCommand(InhPhatonPlugin plugin){this.plugin=plugin;}

    public void register(){
        plugin.registerCommand("god",this);
        plugin.registerCommand("gods",this);
        plugin.registerCommand("боги",this);
    }
    public void unregister(){}

    @Override public void execute(@NotNull CommandSourceStack source,@NotNull String[] args){
        CommandSender sender=source.getSender();
        if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_ADMIN)){sender.sendMessage("§cНет прав god.admin");return;}
        try{
            if(args.length==0){sendHelp(sender);return;}
            switch(args[0].toLowerCase(Locale.ROOT)){
                case "phantom"->handlePhantom(sender,Arrays.copyOfRange(args,1,args.length));
                case "visibility"->handleVisibility(sender,Arrays.copyOfRange(args,1,args.length));
                case "debug"->handleDebug(sender);
                case "reload"->handleReload(sender);
                case "list"->handleList(sender);
                default->sendHelp(sender);
            }
        }catch(Throwable t){sender.sendMessage("§cОшибка: "+String.valueOf(t.getMessage()));LOG.warn("command error",t);}
    }

    @Override public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source,@NotNull String[] args){
        if(!source.getSender().hasPermission(InhPhatonPlugin.PERMISSION_ADMIN))return List.of();
        if(args.length==0)return List.of("phantom","visibility","debug","reload","list");
        if(args.length==1)return Arrays.asList("phantom","visibility","debug","reload","list").stream().filter(s->s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        if(args.length==2&&args[0].equalsIgnoreCase("phantom"))return List.of("create","remove","move","show","hide");
        if(args.length==2&&args[0].equalsIgnoreCase("visibility"))return List.of("only","except");
        if(args.length>=3&&args[0].equalsIgnoreCase("phantom")&&args[1].equalsIgnoreCase("create")&&args.length==3)return Arrays.stream(EntityType.values()).filter(t->t.isSpawnable()&&t.isAlive()).map(t->t.name().toLowerCase(Locale.ROOT)).toList();
        if(args.length>=3&&args[0].equalsIgnoreCase("phantom")&&(args[1].equalsIgnoreCase("show")||args[1].equalsIgnoreCase("hide")))return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
    @Override public String permission(){return InhPhatonPlugin.PERMISSION_ADMIN;}

    private void handlePhantom(CommandSender sender,String[] args){
        if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_PHANTOM)){sender.sendMessage("§cНет прав god.phantom");return;}
        if(args.length==0){sender.sendMessage("§e/god phantom <create|remove|move|show|hide>");return;}
        switch(args[0].toLowerCase(Locale.ROOT)){
            case "create"->phantomCreate(sender,Arrays.copyOfRange(args,1,args.length));
            case "remove"->phantomRemove(sender,args);
            case "move"->phantomMove(sender,args);
            case "show"->phantomShow(sender,args);
            case "hide"->phantomHide(sender,args);
            default->sender.sendMessage("§e/god phantom <create|remove|move|show|hide>");
        }
    }
    private void phantomCreate(CommandSender sender,String[] args){
        if(args.length<4){sender.sendMessage("§eИспользование: /god phantom create <type> <x> <y> <z> [world] [ttl]");return;}
        EntityType type;try{type=EntityType.valueOf(args[0].toUpperCase(Locale.ROOT));}catch(IllegalArgumentException e){sender.sendMessage("§cНеизвестный тип: "+args[0]);return;}
        try{
            double x=Double.parseDouble(args[1]),y=Double.parseDouble(args[2]),z=Double.parseDouble(args[3]);
            org.bukkit.World world=args.length>=5?Bukkit.getWorld(args[4]):sender instanceof Player p?p.getWorld():null;
            if(world==null){sender.sendMessage("§cМир не найден");return;}
            long ttl=args.length>=6?parseLong(args[5],plugin.config().getDefaultTtlSeconds()):plugin.config().getDefaultTtlSeconds();
            UUID owner=sender instanceof Player p?p.getUniqueId():null;
            PhantomSpec spec=PhantomSpec.builder().entityType(type).location(new Location(world,x,y,z)).ttlSeconds(ttl).ownerId(owner).build();
            PhantomInstance inst=plugin.phantoms().create(spec);
            if(inst==null){sender.sendMessage("§cЛимит фантомов");return;}
            if(sender instanceof Player p)plugin.phantoms().showTo(inst.id(),p.getUniqueId());
            sender.sendMessage("§aФантом создан: §e"+inst.id());
        }catch(Exception e){sender.sendMessage("§cОшибка параметров: "+e.getMessage());}
    }
    private void phantomRemove(CommandSender sender,String[] args){if(args.length<2){sender.sendMessage("§e/god phantom remove <id>");return;}UUID id=parseUuid(args[1]);if(id==null){sender.sendMessage("§cUUID невалиден");return;}sender.sendMessage(plugin.phantoms().remove(id)?"§aУдалён":"§cНе найден");}
    private void phantomMove(CommandSender sender,String[] args){if(args.length<5){sender.sendMessage("§e/god phantom move <id> <x> <y> <z>");return;}UUID id=parseUuid(args[1]);if(id==null){sender.sendMessage("§cUUID невалиден");return;}try{PhantomInstance inst=plugin.phantoms().get(id);if(inst==null){sender.sendMessage("§cНе найден");return;}Location base=inst.currentLocation();Location loc=new Location(base.getWorld(),Double.parseDouble(args[2]),Double.parseDouble(args[3]),Double.parseDouble(args[4]),base.getYaw(),base.getPitch());plugin.phantoms().move(id,loc);sender.sendMessage("§aПеремещён");}catch(Exception e){sender.sendMessage("§cОшибка координат");}}
    private void phantomShow(CommandSender sender,String[] args){if(args.length<3){sender.sendMessage("§e/god phantom show <id> <player>");return;}UUID id=parseUuid(args[1]);Player p=Bukkit.getPlayerExact(args[2]);if(id==null||p==null){sender.sendMessage("§cUUID или игрок не найден");return;}plugin.phantoms().showTo(id,p.getUniqueId());sender.sendMessage("§aПоказан "+p.getName());}
    private void phantomHide(CommandSender sender,String[] args){if(args.length<3){sender.sendMessage("§e/god phantom hide <id> <player>");return;}UUID id=parseUuid(args[1]);Player p=Bukkit.getPlayerExact(args[2]);if(id==null||p==null){sender.sendMessage("§cUUID или игрок не найден");return;}plugin.phantoms().hideFrom(id,p.getUniqueId());sender.sendMessage("§aСкрыт от "+p.getName());}
    private void handleVisibility(CommandSender sender,String[] args){if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_VISIBILITY)){sender.sendMessage("§cНет прав god.visibility");return;}if(args.length<3){sender.sendMessage("§e/god visibility <only|except> <phantom-id> <player1,player2,...>");return;}String mode=args[0].toLowerCase(Locale.ROOT);UUID id=parseUuid(args[1]);if(id==null){sender.sendMessage("§cUUID невалиден");return;}Set<UUID> selected=Arrays.stream(args[2].split(",")).map(Bukkit::getPlayerExact).filter(java.util.Objects::nonNull).map(Player::getUniqueId).collect(Collectors.toCollection(LinkedHashSet::new));if("only".equals(mode)){for(Player p:Bukkit.getOnlinePlayers())plugin.phantoms().hideFrom(id,p.getUniqueId());for(UUID u:selected)plugin.phantoms().showTo(id,u);sender.sendMessage("§aONLY → "+selected.size()+" players");}else if("except".equals(mode)){for(Player p:Bukkit.getOnlinePlayers())plugin.phantoms().showTo(id,p.getUniqueId());for(UUID u:selected)plugin.phantoms().hideFrom(id,u);sender.sendMessage("§aEXCEPT → "+selected.size()+" players hidden");}else sender.sendMessage("§cРежим: only|except");}
    private void handleDebug(CommandSender sender){if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_DEBUG)){sender.sendMessage("§cНет прав god.debug");return;}plugin.debug().toggle();sender.sendMessage("§aDebug = "+plugin.debug().isEnabled());}
    private void handleReload(CommandSender sender){if(!sender.hasPermission(InhPhatonPlugin.PERMISSION_RELOAD)){sender.sendMessage("§cНет прав god.reload");return;}plugin.reload();sender.sendMessage("§aКонфигурация перечитана");}
    private void handleList(CommandSender sender){sender.sendMessage("§6=== inhPhaton: active phantoms ("+plugin.phantoms().size()+"/"+plugin.config().getGlobalPhantomLimit()+") ===");for(PhantomInstance p:plugin.phantoms().all())sender.sendMessage(String.format("§e- §f%s §7type=%s observers=%d owner=%s",p.id(),p.spec().getEntityType(),p.observers().size(),p.spec().getOwnerId()));}
    private void sendHelp(CommandSender sender){sender.sendMessage("§6=== inhPhaton — Боги ===");sender.sendMessage("§e/god phantom <create|remove|move|show|hide>");sender.sendMessage("§e/god visibility <only|except>");sender.sendMessage("§e/god debug");sender.sendMessage("§e/god reload");sender.sendMessage("§e/god list");}
    private static UUID parseUuid(String s){try{return UUID.fromString(s);}catch(IllegalArgumentException e){return null;}}
    private static long parseLong(String s,long d){try{return Long.parseLong(s);}catch(Exception e){return d;}}
}