package io.github.donnyyu.cleandash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.reflect.Method;

/**
 * Whether it is night, i.e. the car's headlights are on. Android's own night mode counts, but Topway units
 * (TS18) keep that locked off: their ILL wire reaches apps only as two broadcasts from com.tw.service, sent
 * when the lights change. They carry no state, so at start the launcher asks the unit's own API, TWUtil, which
 * answers message 0x204 with arg1 1 for night, 0 for day. TWUtil is only in Topway firmware, so it is reached
 * by reflection and anything missing just means no answer.
 */
final class NightMode {

    static final String TOPWAY_NIGHT = "com.zjinnova.zlink.action.OUT_DARK_START";
    static final String TOPWAY_DAY = "com.zjinnova.zlink.action.OUT_DARK_STOP";

    private static final String TWUTIL = "android.tw.john.TWUtil";
    private static final int TW_NIGHT = 0x204;
    /** Written with a TWUtil id, asks for its current value. */
    private static final int TW_QUERY = 0xff;
    private static final long TW_TIMEOUT_MS = 3000;
    /** A message of our own, posted to give up on an unanswered question. */
    private static final int TW_TIMEOUT = -1;

    /** What the head unit last said about the headlights; null until it says something. */
    private static Boolean headlights;

    private NightMode() {
    }

    static boolean isNight(Context context) {
        int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (uiMode == Configuration.UI_MODE_NIGHT_YES) return true;
        return Boolean.TRUE.equals(headlights);
    }

    /**
     * Calls onChange, on the main thread, whenever the head unit reports the headlights going on or off, and
     * once if asking a Topway unit finds them different from what was assumed.
     */
    static BroadcastReceiver register(Context context, Runnable onChange) {
        // Once per process: after that the broadcasts keep it current, even across recreate().
        if (headlights == null) ask(onChange);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                headlights = TOPWAY_NIGHT.equals(intent.getAction());
                onChange.run();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(TOPWAY_NIGHT);
        filter.addAction(TOPWAY_DAY);
        // The sender is another app, so on Android 13+ the receiver must be exported to hear it. Older
        // versions ignore the flag.
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        return receiver;
    }

    /** Asks a Topway unit whether the headlights are on now. */
    private static void ask(Runnable onChange) {
        try {
            Class<?> twUtil = Class.forName(TWUTIL);
            Object tw = twUtil.getConstructor().newInstance();
            if ((Integer) twUtil.getMethod("open", short[].class).invoke(tw, (Object) new short[]{TW_NIGHT}) != 0) {
                return;
            }
            Method close = twUtil.getMethod("close");
            Handler handler = new Handler(Looper.getMainLooper()) {
                private boolean done;

                @Override
                public void handleMessage(Message msg) {
                    if (done) return;
                    if (msg.what == TW_NIGHT) {
                        Boolean before = headlights;
                        headlights = msg.arg1 == 1;
                        if (!headlights.equals(before)) onChange.run();
                    } else if (msg.what != TW_TIMEOUT) {
                        return;
                    }
                    done = true;
                    try {
                        close.invoke(tw);
                    } catch (ReflectiveOperationException ignored) {
                        // Already closed; nothing to free.
                    }
                }
            };
            twUtil.getMethod("start").invoke(tw);
            twUtil.getMethod("addHandler", String.class, Handler.class).invoke(tw, "cleandash", handler);
            twUtil.getMethod("write", int.class, int.class).invoke(tw, TW_NIGHT, TW_QUERY);
            handler.sendEmptyMessageDelayed(TW_TIMEOUT, TW_TIMEOUT_MS);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // Not a Topway unit, or its firmware differs: the broadcasts still work.
        }
    }
}
