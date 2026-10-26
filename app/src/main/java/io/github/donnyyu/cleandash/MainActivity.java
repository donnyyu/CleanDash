package io.github.donnyyu.cleandash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

/** Home: the clock and two rows of tiles that open the head unit's own apps. */
public class MainActivity extends Activity {

    private static final int COLUMNS = 4;

    private final View[] tileViews = new View[Tiles.ALL.length];
    private List<AppList.App> apps = Collections.emptyList();
    private View setHome;

    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApps();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setHome = findViewById(R.id.set_home);
        setHome.setOnClickListener(v -> HomeRole.request(this));
        findViewById(R.id.clock).setOnLongClickListener(v -> {
            showMenu();
            return true;
        });

        ViewGroup[] rows = {findViewById(R.id.row1), findViewById(R.id.row2)};
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < Tiles.ALL.length; i++) {
            Tiles.Tile tile = Tiles.ALL[i];
            ViewGroup row = rows[i / COLUMNS];
            View view = inflater.inflate(R.layout.item_tile, row, false);
            ((ImageView) view.findViewById(R.id.tile_icon)).setImageResource(tile.icon);
            ((TextView) view.findViewById(R.id.tile_label)).setText(tile.label);
            view.setOnClickListener(v -> open(tile));
            if (!Tiles.APPS.equals(tile.id)) {
                view.setOnLongClickListener(v -> {
                    choose(tile);
                    return true;
                });
            }
            row.addView(view);
            tileViews[i] = view;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packagesChanged, filter);
        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setHome.setVisibility(HomeRole.isHome(this) ? View.GONE : View.VISIBLE);
        refreshTiles();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // As Home there is nothing behind this screen; as an ordinary app, Back leaves it.
        if (!HomeRole.isHome(this)) super.onBackPressed();
    }

    private void loadApps() {
        Context app = getApplicationContext();
        new Thread(() -> {
            List<AppList.App> loaded = AppList.load(app);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                apps = loaded;
                refreshTiles();
            });
        }, "load-apps").start();
    }

    /** Dims the tiles whose app is not on this unit, so a missing app shows before it is tapped. */
    private void refreshTiles() {
        for (int i = 0; i < Tiles.ALL.length; i++) {
            Tiles.Tile tile = Tiles.ALL[i];
            boolean found = Tiles.APPS.equals(tile.id) || Tiles.resolve(this, tile, apps) != null;
            View view = tileViews[i];
            view.findViewById(R.id.tile_icon).setAlpha(found ? 1f : 0.35f);
            view.findViewById(R.id.tile_label).setAlpha(found ? 1f : 0.5f);
            view.findViewById(R.id.tile_hint).setVisibility(found ? View.GONE : View.VISIBLE);
        }
    }

    private void open(Tiles.Tile tile) {
        if (Tiles.APPS.equals(tile.id)) {
            startActivity(new Intent(this, AppsActivity.class));
            return;
        }
        Intent intent = Tiles.resolve(this, tile, apps);
        if (intent == null) {
            choose(tile);
            return;
        }
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, getString(R.string.cannot_open, getString(tile.label)), Toast.LENGTH_SHORT).show();
        }
    }

    /** Lets the person pick which installed app a tile opens; Automatic goes back to the built-in search. */
    private void choose(Tiles.Tile tile) {
        List<AppList.App> list = apps;
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_app, getString(tile.label)))
                .setAdapter(new AppAdapter(this, R.layout.item_app_row, list), (dialog, which) -> {
                    Tiles.setPick(this, tile, list.get(which).component);
                    refreshTiles();
                })
                .setNeutralButton(R.string.automatic, (dialog, which) -> {
                    Tiles.setPick(this, tile, null);
                    refreshTiles();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showMenu() {
        String[] items = {getString(R.string.menu_home), getString(R.string.menu_reset)};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        HomeRole.request(this);
                    } else {
                        Tiles.clearPicks(this);
                        refreshTiles();
                    }
                })
                .show();
    }
}
