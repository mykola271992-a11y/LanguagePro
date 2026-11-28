package ua.deutschpro.player;

import android.content.Context;

final class ThemePalette {
    final String name;
    final int bg, card, ink, muted, accent, hero1, hero2, hero3;
    private ThemePalette(String name,int bg,int card,int ink,int muted,int accent,int hero1,int hero2,int hero3){
        this.name=name;this.bg=bg;this.card=card;this.ink=ink;this.muted=muted;this.accent=accent;this.hero1=hero1;this.hero2=hero2;this.hero3=hero3;
    }
    static final ThemePalette[] ALL={
        new ThemePalette("Темна м’ятна",0xff0b1020,0xff19233b,0xfff4f6ff,0xffa4afc8,0xffa6f5d2,0xff33476a,0xff19263b,0xff154139),
        new ThemePalette("Світла",0xfff3f6fb,0xffffffff,0xff172033,0xff5e6b80,0xff167a66,0xffdce9f5,0xffe5f0ed,0xffcce8dd),
        new ThemePalette("Синя",0xff071a2e,0xff123251,0xfff3f8ff,0xffa9c0d8,0xff65c9ff,0xff1b5380,0xff123251,0xff123d50)
    };
    static ThemePalette current(Context c){int i=Store.prefs(c).getInt("theme",0);return ALL[Math.max(0,Math.min(i,ALL.length-1))];}
}
