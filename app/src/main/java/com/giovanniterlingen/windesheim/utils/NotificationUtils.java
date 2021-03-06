/**
 * Copyright (c) 2019 Giovanni Terlingen
 * <p/>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 **/
package com.giovanniterlingen.windesheim.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.giovanniterlingen.windesheim.ApplicationLoader;
import com.giovanniterlingen.windesheim.R;
import com.giovanniterlingen.windesheim.view.ScheduleActivity;

/**
 * A schedule app for students and teachers of Windesheim
 *
 * @author Giovanni Terlingen
 */
public class NotificationUtils {

    public static final int SERVICE_NOTIFICATION_ID = 2;
    private static final String PERSISTENT_NOTIFICATION_CHANNEL = "com.giovanniterlingen.windesheim.notification.persistent";
    private static final String PUSH_NOTIFICATION_CHANNEL = "com.giovanniterlingen.windesheim.notification.push";
    private static final String SERVICE_NOTIFICATION_CHANNEL = "com.giovanniterlingen.windesheim.notification.service";
    private static final int LESSON_NOTIFICATION_ID = 0;
    private static final int SCHEDULE_CHANGED_NOTIFICATION_ID = 1;
    private static volatile NotificationUtils Instance = null;
    private final NotificationManager mNotificationManager;

    private NotificationUtils() {
        this.mNotificationManager = (NotificationManager) ApplicationLoader.applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public static NotificationUtils getInstance() {
        NotificationUtils localInstance = Instance;
        if (localInstance == null) {
            synchronized (NotificationUtils.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new NotificationUtils();
                }
            }
        }
        return localInstance;
    }

    public void createScheduleChangedNotification() {
        Intent intent = new Intent(ApplicationLoader.applicationContext, ScheduleActivity.class);
        intent.putExtra("notification", true);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(ApplicationLoader.applicationContext, (int) System.currentTimeMillis(),
                        intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                ApplicationLoader.applicationContext, PUSH_NOTIFICATION_CHANNEL)
                .setContentTitle(ApplicationLoader.applicationContext.getResources()
                        .getString(R.string.app_name))
                .setContentText(ApplicationLoader.applicationContext.getResources()
                        .getString(R.string.schedule_changed))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notifybar)
                .setOngoing(false)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(ApplicationLoader.applicationContext.getResources()
                                .getString(R.string.schedule_changed)))
                .setColor(ContextCompat.getColor(ApplicationLoader.applicationContext,
                        R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        mNotificationManager.notify(SCHEDULE_CHANGED_NOTIFICATION_ID, mBuilder.build());
    }

    public void createNotification(String notificationText, boolean onGoing, boolean headsUp) {
        Intent intent = new Intent(ApplicationLoader.applicationContext,
                ScheduleActivity.class);
        intent.putExtra("notification", true);
        PendingIntent pendingIntent = PendingIntent
                .getActivity(ApplicationLoader.applicationContext,
                        (int) System.currentTimeMillis(),
                        intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                ApplicationLoader.applicationContext, headsUp ? PUSH_NOTIFICATION_CHANNEL :
                PERSISTENT_NOTIFICATION_CHANNEL)
                .setContentTitle(ApplicationLoader.applicationContext.getResources()
                        .getString(R.string.app_name))
                .setContentText(notificationText)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notifybar)
                .setOngoing(onGoing)
                .setAutoCancel(!onGoing)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationText))
                .setColor(ContextCompat.getColor(ApplicationLoader.applicationContext,
                        R.color.colorPrimary));
        if (headsUp) {
            mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
            mBuilder.setDefaults(Notification.DEFAULT_ALL);
        } else if (onGoing) {
            mBuilder.setPriority(NotificationCompat.PRIORITY_MIN);
        }
        mNotificationManager.notify(LESSON_NOTIFICATION_ID, mBuilder.build());
    }

    public void clearNotification() {
        mNotificationManager.cancel(LESSON_NOTIFICATION_ID);
    }

    public void initNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel pushChannel = new NotificationChannel(PUSH_NOTIFICATION_CHANNEL,
                    ApplicationLoader.applicationContext.getResources()
                            .getString(R.string.push_notification),
                    NotificationManager.IMPORTANCE_HIGH);

            pushChannel.setDescription(ApplicationLoader.applicationContext.getResources()
                    .getString(R.string.push_notification_description));
            pushChannel.enableLights(true);
            pushChannel.enableVibration(true);
            pushChannel.setShowBadge(true);
            pushChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationChannel persistentChannel = new NotificationChannel(
                    PERSISTENT_NOTIFICATION_CHANNEL, ApplicationLoader.applicationContext.getResources()
                    .getString(R.string.persistent_notification),
                    NotificationManager.IMPORTANCE_MIN);

            persistentChannel.setDescription(ApplicationLoader.applicationContext.getResources()
                    .getString(R.string.persistent_notification_description));
            persistentChannel.enableLights(false);
            persistentChannel.enableVibration(false);
            persistentChannel.setShowBadge(false);
            persistentChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            persistentChannel.setSound(null, null);

            NotificationChannel serviceChannel = new NotificationChannel(
                    SERVICE_NOTIFICATION_CHANNEL, ApplicationLoader.applicationContext.getResources()
                    .getString(R.string.service_notification),
                    NotificationManager.IMPORTANCE_MIN);

            serviceChannel.setDescription(ApplicationLoader.applicationContext.getResources()
                    .getString(R.string.service_notification_description));
            serviceChannel.enableLights(false);
            serviceChannel.enableVibration(false);
            serviceChannel.setShowBadge(false);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            serviceChannel.setSound(null, null);

            NotificationManager manager = (NotificationManager) ApplicationLoader
                    .applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) {
                return;
            }
            if (manager.getNotificationChannel(PUSH_NOTIFICATION_CHANNEL) == null) {
                manager.createNotificationChannel(pushChannel);
            }
            if (manager.getNotificationChannel(PERSISTENT_NOTIFICATION_CHANNEL) == null) {
                manager.createNotificationChannel(persistentChannel);
            }
            if (manager.getNotificationChannel(SERVICE_NOTIFICATION_CHANNEL) == null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @RequiresApi(api = 26)
    public Notification getServiceNotification() {
        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, ApplicationLoader.applicationContext.getPackageName());
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, SERVICE_NOTIFICATION_CHANNEL);

        PendingIntent pendingIntent = PendingIntent
                .getActivity(ApplicationLoader.applicationContext, (int) System.currentTimeMillis(),
                        intent, 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                ApplicationLoader.applicationContext, SERVICE_NOTIFICATION_CHANNEL)
                .setContentTitle(ApplicationLoader.applicationContext.getResources()
                        .getString(R.string.app_name))
                .setContentText(ApplicationLoader.applicationContext.getResources()
                        .getString(R.string.disable_notification_description))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(ApplicationLoader.applicationContext.getResources()
                                .getString(R.string.disable_notification_description)))
                .setOngoing(true)
                .setSmallIcon(R.drawable.notifybar)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent);

        return mBuilder.build();
    }
}