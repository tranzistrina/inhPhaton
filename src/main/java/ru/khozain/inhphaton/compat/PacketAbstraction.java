package ru.khozain.inhphaton.compat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApiStatus.Internal
public final class PacketAbstraction {
    private static final Logger LOG = LoggerFactory.getLogger("inhPhaton.packets");
    private static final ConcurrentHashMap<String,Object> REF_CACHE = new ConcurrentHashMap<>();

    public static void sendSpawn(@SuppressWarnings("unused") Player viewer, UUID phantomId) {
        LOG.debug("[packet] sendSpawn id={}", phantomId);
    }

    public static void sendEntityDestroy(Player viewer, UUID phantomId) {
        LOG.debug("[packet] sendEntityDestroy to {} id={}", viewer.getName(), phantomId);
        if (!hasNms()) return;
        try { invokeStatic("EntityDestroySender","destroy",viewer,phantomId); }
        catch (Throwable t) { LOG.debug("[packet] destroy failed (no-op): {}", t.getMessage()); }
    }

    public static void sendEntityTeleport(Player viewer, UUID phantomId, Location loc) {
        LOG.debug("[packet] sendEntityTeleport to {} id={} loc={},{},{}",
            viewer.getName(), phantomId, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (!hasNms()) return;
        try { invokeStatic("EntityTeleportSender","teleport",viewer,phantomId,loc); }
        catch (Throwable t) { LOG.debug("[packet] teleport failed (no-op): {}", t.getMessage()); }
    }

    private static boolean hasNms() {
        try { Class.forName("net.minecraft.world.entity.Entity"); return true; }
        catch (Throwable t) { return false; }
    }

    private static Object invokeStatic(String clsName,String method,Object... args) {
        try {
            Class<?> cls=Class.forName("ru.khozain.inhphaton.compat.nms."+clsName);
            Method m=cls.getDeclaredMethod(method,classesOf(args));
            return m.invoke(null,args);
        } catch (Throwable t) { return null; }
    }

    private static Class<?>[] classesOf(Object[] args) {
        Class<?>[] out=new Class<?>[args.length];
        for(int i=0;i<args.length;i++) out[i]=args[i].getClass();
        return out;
    }
}