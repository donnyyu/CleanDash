package io.github.donnyyu.cleandash;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;

/**
 * Making CleanDash the Home screen. The HOME entry (HomeAlias in the manifest) ships disabled, so the
 * launcher is tried as an ordinary app first and only takes over after the person asks it to. The stock
 * launcher stays installed: Android Settings > Apps > Default apps > Home app switches back.
 */
final class HomeRole {

    private HomeRole() {}

    private static ComponentName alias(Context context) {
        return new ComponentName(context, context.getPackageName() + ".HomeAlias");
    }

    static boolean isHome(Context context) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = context.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
        return info != null && info.activityInfo != null
                && context.getPackageName().equals(info.activityInfo.packageName);
    }

    /** Turns the HOME entry on and opens the system screen that picks the Home app. */
    static void request(Activity activity) {
        activity.getPackageManager().setComponentEnabledSetting(alias(activity),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = activity.getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                try {
                    activity.startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), 1);
                    return;
                } catch (ActivityNotFoundException ignored) {
                    // Some head-unit firmware strips the role dialog; fall through to the settings screen.
                }
            }
        }
        try {
            activity.startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (ActivityNotFoundException e) {
            // Last resort: pressing Home now offers every launcher, CleanDash included.
            activity.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
