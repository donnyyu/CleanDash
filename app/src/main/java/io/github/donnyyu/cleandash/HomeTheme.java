package io.github.donnyyu.cleandash;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The Home designs. Each is one layout plus one activity theme; MainActivity finds the tiles and the
 * media card in whichever layout is chosen. The person picks a day theme and a night theme from the clock's
 * menu; the night one is shown while NightMode says it is night (headlights on), unless it is turned off.
 */
enum HomeTheme {
    GLASS(R.string.theme_glass, R.layout.home_glass, R.style.Theme_CleanDash_Glass),
    INSTRUMENT(R.string.theme_instrument, R.layout.home_instrument, R.style.Theme_CleanDash_Instrument),
    RAIL(R.string.theme_rail, R.layout.home_rail, R.style.Theme_CleanDash_Rail),
    ANALOG(R.string.theme_analog, R.layout.home_analog, R.style.Theme_CleanDash_Analog),
    HALO(R.string.theme_halo, R.layout.home_halo, R.style.Theme_CleanDash_Halo),
    CARDS(R.string.theme_cards, R.layout.home_cards, R.style.Theme_CleanDash_Cards);

    private static final String KEY = "theme";
    private static final String NIGHT_KEY = "night_theme";
    /** Stored under NIGHT_KEY when the person wants the day theme all the time. */
    private static final String OFF = "off";

    final int label;
    final int layout;
    final int style;

    HomeTheme(int label, int layout, int style) {
        this.label = label;
        this.layout = layout;
        this.style = style;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("home", Context.MODE_PRIVATE);
    }

    private static HomeTheme byName(String name, HomeTheme fallback) {
        for (HomeTheme theme : values()) {
            if (theme.name().equals(name)) return theme;
        }
        return fallback;
    }

    /** The theme to show now. */
    static HomeTheme current(Context context) {
        HomeTheme night = NightMode.isNight(context) ? night(context) : null;
        return night != null ? night : day(context);
    }

    static HomeTheme day(Context context) {
        return byName(prefs(context).getString(KEY, null), GLASS);
    }

    /** The theme for night, or null when the person turned night switching off. */
    static HomeTheme night(Context context) {
        String name = prefs(context).getString(NIGHT_KEY, null);
        if (OFF.equals(name)) return null;
        return byName(name, HALO);
    }

    void saveDay(Context context) {
        prefs(context).edit().putString(KEY, name()).apply();
    }

    /** Saves the night theme; null turns night switching off. */
    static void saveNight(Context context, HomeTheme theme) {
        prefs(context).edit().putString(NIGHT_KEY, theme == null ? OFF : theme.name()).apply();
    }
}
