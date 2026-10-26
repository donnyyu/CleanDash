package io.github.donnyyu.cleandash;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/** The installed apps that have a launcher entry, sorted by name. Labels and icons load off the main thread. */
final class AppList {

    static final class App {
        final ComponentName component;
        final String label;
        final Drawable icon;

        App(ComponentName component, String label, Drawable icon) {
            this.component = component;
            this.label = label;
            this.icon = icon;
        }
    }

    private AppList() {}

    /** Slow on a head unit (every label and icon is read from its APK): call from a background thread. */
    static List<App> load(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(main, 0);
        List<App> apps = new ArrayList<>(infos.size());
        for (ResolveInfo info : infos) {
            if (context.getPackageName().equals(info.activityInfo.packageName)) continue;
            ComponentName component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            apps.add(new App(component, String.valueOf(info.loadLabel(pm)), info.loadIcon(pm)));
        }
        Collator collator = Collator.getInstance();
        apps.sort((a, b) -> collator.compare(a.label, b.label));
        return apps;
    }
}
