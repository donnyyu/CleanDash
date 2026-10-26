package io.github.donnyyu.cleandash;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import java.util.List;

/**
 * The Home tiles and how each one finds the app it opens. Head units ship their own radio, music and
 * Bluetooth apps under vendor package names, so every tile tries, in order:
 * the app the person picked, known vendor screens, known vendor packages, any app whose name matches,
 * and finally a generic Android intent. A tile that finds nothing is shown dimmed; holding it picks an app.
 */
final class Tiles {

    static final String APPS = "apps";
    static final String MUSIC = "music";

    static final class Tile {
        final String id;
        final int label;
        final int icon;
        /** Exact screens, "package/class". For vendor screens with no launcher entry (EQ, car settings). */
        final String[] components;
        final String[] packages;
        /** Words looked for in installed apps' names, e.g. "carplay" finds "ZLink CarPlay". */
        final String[] keywords;
        /** Generic Android intent tried last, or null. */
        final Intent fallback;

        Tile(String id, int label, int icon, String[] components, String[] packages, String[] keywords,
             Intent fallback) {
            this.id = id;
            this.label = label;
            this.icon = icon;
            this.components = components;
            this.packages = packages;
            this.keywords = keywords;
            this.fallback = fallback;
        }
    }

    private static final String[] NONE = {};

    // Topway TS10/TS18 (com.tw.*, DoFun) first, then FYT (com.syu.*) and Microntek. Topway names follow
    // cbkii's TS18 notes (github.com/cbkii/openlauncher-TW, ts18-nativeness-devkit); check them on the unit.
    static final Tile[] ALL = {
            new Tile("radio", R.string.tile_radio, R.drawable.ic_radio,
                    NONE,
                    new String[]{"com.tw.radio", "com.syu.radio", "com.microntek.radio", "com.navimods.radio"},
                    new String[]{"radio", "fm"},
                    null),
            new Tile(MUSIC, R.string.tile_music, R.drawable.ic_music,
                    NONE,
                    new String[]{"com.tw.music", "com.tw.media", "com.syu.music", "com.microntek.music"},
                    new String[]{"music"},
                    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)),
            new Tile("bluetooth", R.string.tile_bluetooth, R.drawable.ic_bluetooth,
                    NONE,
                    new String[]{"com.tw.bt", "com.syu.bt", "com.microntek.bluetooth"},
                    new String[]{"bluetooth"},
                    new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)),
            new Tile("maps", R.string.tile_maps, R.drawable.ic_map,
                    NONE,
                    new String[]{"com.google.android.apps.maps", "com.waze", "app.organicmaps", "net.osmand.plus",
                            "net.osmand", "ru.yandex.yandexnavi", "com.sygic.aura"},
                    new String[]{"maps", "navigation", "navi"},
                    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MAPS)),
            new Tile("carplay", R.string.tile_carplay, R.drawable.ic_car,
                    NONE,
                    new String[]{"com.zjinnova.zlink"},
                    new String[]{"carplay", "zlink", "autokit", "carlink", "carbit", "android auto"},
                    null),
            new Tile("settings", R.string.tile_settings, R.drawable.ic_settings,
                    new String[]{"com.dofun.carsetting/com.dofun.carsetting.ui.MainActivity"},
                    new String[]{"com.dofun.carsetting"},
                    NONE,
                    new Intent(Settings.ACTION_SETTINGS)),
            new Tile("eq", R.string.tile_eq, R.drawable.ic_eq,
                    new String[]{"com.tw.eq/com.tw.eq.EQActivity"},
                    new String[]{"com.tw.eq", "com.syu.eq"},
                    new String[]{"eq", "equalizer", "dsp"},
                    null),
            new Tile(APPS, R.string.tile_apps, R.drawable.ic_apps, NONE, NONE, NONE, null),
    };

    private Tiles() {}

    /** The tile with this id, as written in a Home layout's android:tag, or null. */
    static Tile byId(String id) {
        for (Tile tile : ALL) {
            if (tile.id.equals(id)) return tile;
        }
        return null;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("tiles", Context.MODE_PRIVATE);
    }

    static void setPick(Context context, Tile tile, ComponentName component) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (component == null) {
            editor.remove(tile.id);
        } else {
            editor.putString(tile.id, component.flattenToString());
        }
        editor.apply();
    }

    static void clearPicks(Context context) {
        prefs(context).edit().clear().apply();
    }

    /** The intent this tile opens right now, or null when nothing on the unit fits. */
    static Intent resolve(Context context, Tile tile, List<AppList.App> apps) {
        PackageManager pm = context.getPackageManager();

        String picked = prefs(context).getString(tile.id, null);
        if (picked != null) {
            Intent intent = component(pm, ComponentName.unflattenFromString(picked));
            if (intent != null) return intent;
        }
        for (String flat : tile.components) {
            Intent intent = component(pm, ComponentName.unflattenFromString(flat));
            if (intent != null) return intent;
        }
        for (String pkg : tile.packages) {
            Intent intent = pm.getLaunchIntentForPackage(pkg);
            if (intent != null) return intent;
        }
        for (AppList.App app : apps) {
            if (Keywords.matches(app.label, tile.keywords)) {
                return new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(app.component);
            }
        }
        // Not checked for export: with several handlers this resolves to Android's own chooser.
        if (tile.fallback != null && pm.resolveActivity(tile.fallback, 0) != null) {
            return new Intent(tile.fallback);
        }
        return null;
    }

    private static Intent component(PackageManager pm, ComponentName component) {
        if (component == null) return null;
        Intent intent = new Intent(Intent.ACTION_MAIN).setComponent(component);
        // A vendor screen that is not exported throws SecurityException when started.
        ResolveInfo info = pm.resolveActivity(intent, 0);
        return info != null && info.activityInfo != null && info.activityInfo.exported ? intent : null;
    }
}
