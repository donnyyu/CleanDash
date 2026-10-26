package io.github.donnyyu.cleandash;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * The Home designs. Each is one layout plus one activity theme; MainActivity finds the tiles and the
 * media card in whichever layout is chosen. The person switches from the clock's menu.
 */
enum HomeTheme {
    GLASS(R.string.theme_glass, R.layout.home_glass, R.style.Theme_CleanDash_Glass),
    INSTRUMENT(R.string.theme_instrument, R.layout.home_instrument, R.style.Theme_CleanDash_Instrument),
    RAIL(R.string.theme_rail, R.layout.home_rail, R.style.Theme_CleanDash_Rail);

    private static final String KEY = "theme";

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

    static HomeTheme current(Context context) {
        String name = prefs(context).getString(KEY, null);
        for (HomeTheme theme : values()) {
            if (theme.name().equals(name)) return theme;
        }
        return GLASS;
    }

    void save(Context context) {
        prefs(context).edit().putString(KEY, name()).apply();
    }
}
