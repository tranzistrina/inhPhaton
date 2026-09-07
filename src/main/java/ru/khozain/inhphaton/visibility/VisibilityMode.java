package ru.khozain.inhphaton.visibility;

import java.util.EnumSet;
import java.util.Locale;

public enum VisibilityMode {
    VISIBLE,HIDDEN,ONLY,EXCEPT;
    public static VisibilityMode fromString(String s,VisibilityMode fallback){if(s==null)return fallback;try{return valueOf(s.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){return fallback;}}
    public static VisibilityMode parse(String s){return fromString(s,HIDDEN);}
    public static EnumSet<VisibilityMode> all(){return EnumSet.allOf(VisibilityMode.class);}
}