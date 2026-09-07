package ru.khozain.inhphaton.effect;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PersonalEffect {
    private final Type type; private final Particle particle; private final int particleCount; private final Color particleColor;
    private final Sound sound; private final float volume,pitch; private final String title,subtitle; private final int fadeIn,stay,fadeOut; private final String chatMessage;
    public PersonalEffect(Type type){this.type=type;particle=null;particleCount=0;particleColor=null;sound=null;volume=1f;pitch=1f;title=null;subtitle=null;fadeIn=0;stay=0;fadeOut=0;chatMessage=null;}
    private PersonalEffect(@NotNull Builder b){type=b.type;particle=b.particle;particleCount=b.particleCount;particleColor=b.particleColor;sound=b.sound;volume=b.volume;pitch=b.pitch;title=b.title;subtitle=b.subtitle;fadeIn=b.fadeIn;stay=b.stay;fadeOut=b.fadeOut;chatMessage=b.chatMessage;}
    public enum Type{PARTICLES,SOUND,TITLE,CHAT,COMBO}
    public void play(@NotNull Player viewer){switch(type){
        case PARTICLES->{if(particle!=null){if(particleColor!=null&&particle==Particle.DUST)viewer.spawnParticle(particle,viewer.getLocation(),particleCount,.5,.5,.5,0,particleColor);else viewer.spawnParticle(particle,viewer.getLocation(),particleCount);}}
        case SOUND->{if(sound!=null)viewer.playSound(viewer.getLocation(),sound,volume,pitch);}
        case TITLE->viewer.sendTitle(title==null?"":title,subtitle==null?"":subtitle,fadeIn,stay,fadeOut);
        case CHAT->{if(chatMessage!=null)viewer.sendMessage(chatMessage);}
        case COMBO->{if(chatMessage!=null)viewer.sendMessage(chatMessage);if(sound!=null)viewer.playSound(viewer.getLocation(),sound,volume,pitch);if(particle!=null)viewer.spawnParticle(particle,viewer.getLocation(),particleCount);if(title!=null||subtitle!=null)viewer.sendTitle(title==null?"":title,subtitle==null?"":subtitle,fadeIn,stay,fadeOut);}
    }}
    public Type getType(){return type;} public @Nullable Particle getParticle(){return particle;} public int getParticleCount(){return particleCount;} public @Nullable Color getParticleColor(){return particleColor;}
    public @Nullable Sound getSound(){return sound;} public float getVolume(){return volume;} public float getPitch(){return pitch;} public @Nullable String getTitle(){return title;} public @Nullable String getSubtitle(){return subtitle;}
    public int getFadeIn(){return fadeIn;} public int getStay(){return stay;} public int getFadeOut(){return fadeOut;} public @Nullable String getChatMessage(){return chatMessage;}
    public static Builder builder(Type type){return new Builder(type);}
    public static final class Builder{
        private final Type type; private Particle particle; private int particleCount=16; private Color particleColor; private Sound sound; private float volume=1f,pitch=1f;
        private String title,subtitle; private int fadeIn=10,stay=40,fadeOut=10; private String chatMessage;
        public Builder(Type type){this.type=type;} public Builder particle(Particle p,int count){particle=p;particleCount=count;return this;} public Builder particleColor(Color c){particleColor=c;return this;}
        public Builder sound(Sound s){sound=s;return this;} public Builder volume(float v){volume=v;return this;} public Builder pitch(float v){pitch=v;return this;}
        public Builder title(String t,String s){title=t;subtitle=s;return this;} public Builder timing(int in,int stay,int out){fadeIn=in;this.stay=stay;fadeOut=out;return this;}
        public Builder chat(String m){chatMessage=m;return this;} public PersonalEffect build(){return new PersonalEffect(this);}
    }
}