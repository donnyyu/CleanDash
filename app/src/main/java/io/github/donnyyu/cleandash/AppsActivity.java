package io.github.donnyyu.cleandash;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import java.util.List;

/** Every installed app, as a grid. Tap opens it, hold opens its App info. */
public class AppsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apps);
        findViewById(R.id.close).setOnClickListener(v -> finish());

        GridView grid = findViewById(R.id.grid);
        View loading = findViewById(R.id.loading);
        new Thread(() -> {
            List<AppList.App> apps = AppList.load(getApplicationContext());
            runOnUiThread(() -> {
                if (isFinishing()) return;
                loading.setVisibility(View.GONE);
                grid.setAdapter(new AppAdapter(this, R.layout.item_app, apps));
                grid.setOnItemClickListener((parent, view, position, id) -> open(apps.get(position)));
                grid.setOnItemLongClickListener((parent, view, position, id) -> {
                    showInfo(apps.get(position));
                    return true;
                });
            });
        }, "load-apps").start();
    }

    private void open(AppList.App app) {
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, getString(R.string.cannot_open, app.label), Toast.LENGTH_SHORT).show();
        }
    }

    private void showInfo(AppList.App app) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", app.component.getPackageName(), null)));
        } catch (ActivityNotFoundException ignored) {
            // Some firmware has no App info screen.
        }
    }
}
