package io.github.donnyyu.cleandash;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.List;

/**
 * What is playing on the head unit, read from Android's media sessions. That needs notification access
 * for MediaListener; without it every getter reports nothing and hasAccess() is false.
 */
final class NowPlaying {

    private final Context context;
    private final Runnable onChange;
    private final MediaSessionManager sessions;
    private final ComponentName listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaController controller;
    private boolean started;

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged = this::choose;

    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            onChange.run();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            onChange.run();
        }

        @Override
        public void onSessionDestroyed() {
            refresh();
        }
    };

    NowPlaying(Context context, Runnable onChange) {
        this.context = context;
        this.onChange = onChange;
        sessions = context.getSystemService(MediaSessionManager.class);
        listener = new ComponentName(context, MediaListener.class);
    }

    /** NotificationManager.isNotificationListenerAccessGranted needs API 27; this reads the same setting. */
    static boolean hasAccess(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (enabled == null) return false;
        ComponentName me = new ComponentName(context, MediaListener.class);
        for (String flat : enabled.split(":")) {
            if (me.equals(ComponentName.unflattenFromString(flat))) return true;
        }
        return false;
    }

    void start() {
        started = true;
        if (hasAccess(context)) {
            try {
                sessions.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler);
            } catch (SecurityException e) {
                // Access was withdrawn between the check and the call; refresh() reports nothing playing.
            }
        }
        refresh();
    }

    void stop() {
        started = false;
        sessions.removeOnActiveSessionsChangedListener(sessionsChanged);
        attach(null);
    }

    private void refresh() {
        List<MediaController> list = null;
        if (started && hasAccess(context)) {
            try {
                list = sessions.getActiveSessions(listener);
            } catch (SecurityException e) {
                list = null;
            }
        }
        choose(list);
    }

    /**
     * Prefers a session that is playing, then the first with a track. A session with no track at all, like
     * the idle Bluetooth audio link a head unit always has, is skipped.
     */
    private void choose(List<MediaController> list) {
        MediaController best = null;
        if (list != null) {
            for (MediaController candidate : list) {
                if (candidate.getMetadata() == null) continue;
                if (isPlaying(candidate)) {
                    best = candidate;
                    break;
                }
                if (best == null) best = candidate;
            }
        }
        attach(best);
        onChange.run();
    }

    private void attach(MediaController next) {
        if (controller != null && next != null && controller.getSessionToken().equals(next.getSessionToken())) {
            return;
        }
        if (controller != null) controller.unregisterCallback(callback);
        controller = next;
        if (controller != null) controller.registerCallback(callback, handler);
    }

    MediaController controller() {
        return controller;
    }

    boolean isPlaying() {
        return controller != null && isPlaying(controller);
    }

    private static boolean isPlaying(MediaController controller) {
        PlaybackState state = controller.getPlaybackState();
        return state != null && state.getState() == PlaybackState.STATE_PLAYING;
    }

    CharSequence title() {
        MediaMetadata metadata = metadata();
        if (metadata == null) return null;
        CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        return TextUtils.isEmpty(title) ? metadata.getText(MediaMetadata.METADATA_KEY_TITLE) : title;
    }

    /** "Artist · Album", or whichever of the two the app gives. */
    CharSequence subtitle() {
        MediaMetadata metadata = metadata();
        if (metadata == null) return null;
        CharSequence display = metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
        if (!TextUtils.isEmpty(display)) return display;
        CharSequence artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
        CharSequence album = metadata.getText(MediaMetadata.METADATA_KEY_ALBUM);
        if (TextUtils.isEmpty(artist)) return album;
        if (TextUtils.isEmpty(album)) return artist;
        return artist + " · " + album;
    }

    Bitmap art() {
        MediaMetadata metadata = metadata();
        if (metadata == null) return null;
        for (String key : new String[]{MediaMetadata.METADATA_KEY_ALBUM_ART, MediaMetadata.METADATA_KEY_ART,
                MediaMetadata.METADATA_KEY_DISPLAY_ICON}) {
            Bitmap bitmap = metadata.getBitmap(key);
            if (bitmap != null) return bitmap;
        }
        return null;
    }

    /** How far through the track, 0 to 1, or -1 when the app doesn't say (radio, most Bluetooth phones). */
    float progress() {
        MediaMetadata metadata = metadata();
        PlaybackState state = controller == null ? null : controller.getPlaybackState();
        if (metadata == null || state == null) return -1;
        long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        long position = state.getPosition();
        if (duration <= 0 || position < 0) return -1;
        if (state.getState() == PlaybackState.STATE_PLAYING) {
            position += (long) ((SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime())
                    * state.getPlaybackSpeed());
        }
        return Math.max(0f, Math.min(1f, position / (float) duration));
    }

    private MediaMetadata metadata() {
        return controller == null ? null : controller.getMetadata();
    }
}
