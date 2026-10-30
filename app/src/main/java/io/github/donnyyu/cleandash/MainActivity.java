package io.github.donnyyu.cleandash;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.session.MediaController;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Home: the clock, what's playing, and tiles that open the head unit's own apps, drawn in the chosen
 * HomeTheme. A layout marks each tile with android:tag set to a Tiles id, and may show a tile twice; the
 * tile's icon, label and "not found" hint are tagged "icon", "label" and "hint".
 */
public class MainActivity extends Activity {

    private static final long TICK_MS = 1000;
    /** Tags on the parts of a tile, so that tiles repeated in one layout don't share view ids. */
    private static final String ICON = "icon";
    private static final String LABEL = "label";
    private static final String HINT = "hint";

    private final List<View> tileViews = new ArrayList<>();
    /** The theme this activity was created with; a different HomeTheme.current() means recreate. */
    private HomeTheme shownTheme;
    private BroadcastReceiver headlightsChanged;
    private List<AppList.App> apps = Collections.emptyList();
    private View setHome;
    private NowPlaying nowPlaying;
    private ColorStateList artTint;
    private int artPadding;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            bindProgress();
            handler.postDelayed(this, TICK_MS);
        }
    };

    private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApps();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        HomeTheme theme = HomeTheme.current(this);
        shownTheme = theme;
        setTheme(theme.style);
        super.onCreate(savedInstanceState);
        setContentView(theme.layout);

        setHome = findViewById(R.id.set_home);
        setHome.setOnClickListener(v -> HomeRole.request(this));
        findViewById(R.id.clock).setOnLongClickListener(v -> {
            showMenu();
            return true;
        });

        findTiles(findViewById(android.R.id.content));
        nowPlaying = new NowPlaying(this, this::bindMedia);
        setUpMedia();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packagesChanged, filter);
        headlightsChanged = NightMode.register(this, this::showCurrentTheme);
        loadApps();
    }

    @Override
    protected void onStart() {
        super.onStart();
        nowPlaying.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setHome.setVisibility(HomeRole.isHome(this) ? View.GONE : View.VISIBLE);
        refreshTiles();
        // Notification access may have just been granted in Settings.
        bindMedia();
    }

    @Override
    protected void onStop() {
        nowPlaying.stop();
        handler.removeCallbacks(tick);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packagesChanged);
        unregisterReceiver(headlightsChanged);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // uiMode is in the manifest's configChanges, so Android's night mode lands here instead of recreating.
        showCurrentTheme();
    }

    /** Switches to the day or night theme if the one showing is no longer right. */
    private void showCurrentTheme() {
        if (HomeTheme.current(this) != shownTheme) recreate();
    }

    @Override
    public void onBackPressed() {
        // As Home there is nothing behind this screen; as an ordinary app, Back leaves it.
        if (!HomeRole.isHome(this)) super.onBackPressed();
    }

    private void findTiles(View view) {
        Object tag = view.getTag();
        Tiles.Tile tile = tag instanceof String ? Tiles.byId((String) tag) : null;
        if (tile != null) {
            ImageView icon = view.findViewWithTag(ICON);
            if (icon != null) icon.setImageResource(tile.icon);
            TextView label = view.findViewWithTag(LABEL);
            if (label != null) label.setText(tile.label);
            view.setContentDescription(getText(tile.label));
            view.setOnClickListener(v -> open(tile));
            if (!Tiles.APPS.equals(tile.id)) {
                view.setOnLongClickListener(v -> {
                    choose(tile);
                    return true;
                });
            }
            tileViews.add(view);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                findTiles(group.getChildAt(i));
            }
        }
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

    /**
     * Names each tile after the app picked for it, if any, and dims the tiles whose app is not on this unit,
     * so a missing app shows before it is tapped.
     */
    private void refreshTiles() {
        for (View view : tileViews) {
            Tiles.Tile tile = Tiles.byId((String) view.getTag());
            boolean found = Tiles.APPS.equals(tile.id) || Tiles.resolve(this, tile, apps) != null;
            CharSequence name = name(tile);
            view.setContentDescription(name);
            View icon = view.findViewWithTag(ICON);
            if (icon != null) icon.setAlpha(found ? 1f : 0.35f);
            TextView label = view.findViewWithTag(LABEL);
            if (label != null) {
                label.setText(name);
                label.setAlpha(found ? 1f : 0.5f);
            }
            View hint = view.findViewWithTag(HINT);
            if (hint != null) hint.setVisibility(found ? View.GONE : View.VISIBLE);
        }
    }

    /** The picked app's name; the tile's own name on Automatic, or when the picked app is gone. */
    private CharSequence name(Tiles.Tile tile) {
        ComponentName picked = Tiles.pick(this, tile);
        if (picked != null) {
            for (AppList.App app : apps) {
                if (app.component.equals(picked)) return app.label;
            }
        }
        return getText(tile.label);
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

    private void setUpMedia() {
        View card = findViewById(R.id.media_card);
        if (card == null) return;
        card.setOnClickListener(v -> openMedia());
        ImageView art = findViewById(R.id.media_art);
        if (art != null) {
            artTint = art.getImageTintList();
            artPadding = art.getPaddingTop();
            art.setClipToOutline(true);
        }
        onClick(R.id.media_play, v -> control(c -> {
            if (nowPlaying.isPlaying()) {
                c.pause();
            } else {
                c.play();
            }
        }));
        onClick(R.id.media_prev, v -> control(MediaController.TransportControls::skipToPrevious));
        onClick(R.id.media_next, v -> control(MediaController.TransportControls::skipToNext));
    }

    private void onClick(int id, View.OnClickListener listener) {
        View view = findViewById(id);
        if (view != null) view.setOnClickListener(listener);
    }

    /** Sends a command to the playing app. With nothing playing, a button does what tapping the card does. */
    private void control(Consumer<MediaController.TransportControls> command) {
        MediaController controller = nowPlaying.controller();
        if (controller == null) {
            openMedia();
        } else {
            command.accept(controller.getTransportControls());
        }
    }

    /** Opens the app that is playing, the Music tile's app when nothing is, or the access screen. */
    private void openMedia() {
        if (!NowPlaying.hasAccess(this)) {
            try {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
            } catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this, R.string.media_no_settings, Toast.LENGTH_LONG).show();
            }
            return;
        }
        MediaController controller = nowPlaying.controller();
        if (controller != null) {
            PendingIntent session = controller.getSessionActivity();
            try {
                if (session != null) {
                    session.send();
                    return;
                }
                Intent launch = getPackageManager().getLaunchIntentForPackage(controller.getPackageName());
                if (launch != null) {
                    startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    return;
                }
            } catch (PendingIntent.CanceledException | ActivityNotFoundException | SecurityException e) {
                // Falls through to the Music tile.
            }
        }
        open(Tiles.byId(Tiles.MUSIC));
    }

    private void bindMedia() {
        if (findViewById(R.id.media_card) == null) return;
        MediaController controller = nowPlaying.controller();
        boolean playing = nowPlaying.isPlaying();
        CharSequence state = null;
        CharSequence source = getText(R.string.media_source);
        CharSequence title;
        CharSequence subtitle;
        if (!NowPlaying.hasAccess(this)) {
            title = getText(R.string.media_allow);
            subtitle = getText(R.string.media_allow_hint);
        } else if (controller == null) {
            title = getText(R.string.media_nothing);
            subtitle = getText(R.string.media_open_music);
        } else {
            state = getText(playing ? R.string.media_playing : R.string.media_paused);
            source = appLabel(controller.getPackageName());
            title = nowPlaying.title();
            if (TextUtils.isEmpty(title)) title = source;
            subtitle = nowPlaying.subtitle();
        }
        setText(R.id.media_state, state);
        setText(R.id.media_source, source);
        setText(R.id.media_title, title);
        setText(R.id.media_artist, subtitle);

        ImageView play = findViewById(R.id.media_play);
        if (play != null) play.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        bindArt(controller == null ? null : nowPlaying.art());

        handler.removeCallbacks(tick);
        if (playing) {
            handler.post(tick);
        } else {
            bindProgress();
        }
    }

    private void bindArt(Bitmap bitmap) {
        ImageView art = findViewById(R.id.media_art);
        if (art == null) return;
        if (bitmap != null) {
            art.setImageTintList(null);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);
            art.setPadding(0, 0, 0, 0);
            art.setImageBitmap(bitmap);
        } else {
            art.setImageTintList(artTint);
            art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            art.setPadding(artPadding, artPadding, artPadding, artPadding);
            art.setImageResource(R.drawable.ic_music);
        }
    }

    private void bindProgress() {
        View bar = findViewById(R.id.media_progress);
        if (bar == null) return;
        bar.setPivotX(0);
        bar.setScaleX(Math.max(0f, nowPlaying.progress()));
    }

    private void setText(int id, CharSequence text) {
        TextView view = findViewById(id);
        if (view == null) return;
        view.setText(text);
        view.setVisibility(TextUtils.isEmpty(text) ? View.INVISIBLE : View.VISIBLE);
    }

    private CharSequence appLabel(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            return getText(R.string.media_source);
        }
    }

    private void showMenu() {
        String[] items = {getString(R.string.menu_theme), getString(R.string.menu_night_theme),
                getString(R.string.menu_home), getString(R.string.menu_reset)};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        chooseTheme();
                    } else if (which == 1) {
                        chooseNightTheme();
                    } else if (which == 2) {
                        HomeRole.request(this);
                    } else {
                        Tiles.clearPicks(this);
                        refreshTiles();
                    }
                })
                .show();
    }

    private void chooseTheme() {
        HomeTheme[] themes = HomeTheme.values();
        String[] names = new String[themes.length];
        for (int i = 0; i < themes.length; i++) {
            names[i] = getString(themes[i].label);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_theme)
                .setSingleChoiceItems(names, HomeTheme.day(this).ordinal(), (dialog, which) -> {
                    dialog.dismiss();
                    themes[which].saveDay(this);
                    showCurrentTheme();
                })
                .show();
    }

    /** "Off" first, then the themes. */
    private void chooseNightTheme() {
        HomeTheme[] themes = HomeTheme.values();
        String[] names = new String[themes.length + 1];
        names[0] = getString(R.string.night_off);
        for (int i = 0; i < themes.length; i++) {
            names[i + 1] = getString(themes[i].label);
        }
        HomeTheme night = HomeTheme.night(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_night_theme)
                .setSingleChoiceItems(names, night == null ? 0 : night.ordinal() + 1, (dialog, which) -> {
                    dialog.dismiss();
                    HomeTheme.saveNight(this, which == 0 ? null : themes[which - 1]);
                    showCurrentTheme();
                })
                .show();
    }
}
