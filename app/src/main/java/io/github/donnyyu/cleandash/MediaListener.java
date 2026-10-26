package io.github.donnyyu.cleandash;

import android.service.notification.NotificationListenerService;

/**
 * Android only lets an app read other apps' media sessions (NowPlaying) while it holds notification
 * access, and that access is granted to a listener service. This is that service. It ignores the
 * notifications themselves, and CleanDash has no internet permission to send anything anywhere.
 */
public class MediaListener extends NotificationListenerService {
}
