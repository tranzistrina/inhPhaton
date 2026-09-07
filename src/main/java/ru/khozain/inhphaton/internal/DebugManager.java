package ru.khozain.inhphaton.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DebugManager {
    private static final Logger LOG=LoggerFactory.getLogger("inhPhaton.debug");
    private volatile boolean enabled;
    public DebugManager(boolean initial){enabled=initial;LOG.info("DebugManager initial={}",initial);}
    public boolean isEnabled(){return enabled;}
    public void setEnabled(boolean v){enabled=v;LOG.info("DebugManager toggled={}",v);}
    public void toggle(){setEnabled(!enabled);}
}