package ru.khozain.inhphaton.phantom;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PhantomSpec {
    private final UUID id; private final EntityType entityType; private final Location initialLocation;
    private final float yaw,pitch; private final String customName; private final boolean customNameVisible,silent,glowing,invulnerable,gravity;
    private final long createdAtMs,ttlSeconds; private final UUID ownerId; private final Map<String,Object> meta;
    private PhantomSpec(Builder b){id=b.id;entityType=b.entityType;initialLocation=b.location;yaw=b.yaw;pitch=b.pitch;customName=b.customName;customNameVisible=b.customNameVisible;silent=b.silent;glowing=b.glowing;invulnerable=b.invulnerable;gravity=b.gravity;createdAtMs=b.createdAtMs;ttlSeconds=b.ttlSeconds;ownerId=b.ownerId;meta=new LinkedHashMap<>(b.meta);}
    public static Builder builder(){return new Builder();} public UUID getId(){return id;} public EntityType getEntityType(){return entityType;} public Location getInitialLocation(){return initialLocation.clone();}
    public float getYaw(){return yaw;} public float getPitch(){return pitch;} public @Nullable String getCustomName(){return customName;} public boolean isCustomNameVisible(){return customNameVisible;}
    public boolean isSilent(){return silent;} public boolean isGlowing(){return glowing;} public boolean isInvulnerable(){return invulnerable;} public boolean hasGravity(){return gravity;}
    public long getCreatedAtMs(){return createdAtMs;} public long getTtlSeconds(){return ttlSeconds;} public @Nullable UUID getOwnerId(){return ownerId;} public Map<String,Object> getMeta(){return meta;}
    public boolean isExpired(long nowMs){return ttlSeconds>0&&(nowMs-createdAtMs)>ttlSeconds*1000L;}
    public static final class Builder{
        private final UUID id=UUID.randomUUID(); private EntityType entityType=EntityType.ZOMBIE; private Location location; private float yaw,pitch; private String customName;
        private boolean customNameVisible=true,silent=false,glowing=false,invulnerable=true,gravity=false; private long createdAtMs=System.currentTimeMillis(),ttlSeconds; private UUID ownerId; private final Map<String,Object> meta=new LinkedHashMap<>();
        public Builder entityType(@NotNull EntityType t){entityType=t;return this;} public Builder location(@NotNull Location l){location=l;yaw=l.getYaw();pitch=l.getPitch();return this;}
        public Builder yaw(float v){yaw=v;return this;} public Builder pitch(float v){pitch=v;return this;} public Builder customName(@Nullable String s){customName=s;return this;}
        public Builder customNameVisible(boolean v){customNameVisible=v;return this;} public Builder silent(boolean v){silent=v;return this;} public Builder glowing(boolean v){glowing=v;return this;}
        public Builder invulnerable(boolean v){invulnerable=v;return this;} public Builder gravity(boolean v){gravity=v;return this;} public Builder ttlSeconds(long v){ttlSeconds=v;return this;}
        public Builder ownerId(@Nullable UUID id){ownerId=id;return this;} public Builder meta(@NotNull String k,@NotNull Object v){meta.put(k,v);return this;}
        public PhantomSpec build(){if(location==null)throw new IllegalStateException("PhantomSpec.location required");return new PhantomSpec(this);}
    }
}