package com.asrivas.ecommclevertapsdkdemo;

import android.app.Application;
import android.app.NotificationChannel; // Import for NotificationChannel
import android.app.NotificationManager; // Import for NotificationManager
import android.os.Build; // Import for Build class

import com.clevertap.android.sdk.ActivityLifecycleCallback;
import com.clevertap.android.sdk.CleverTapAPI;

public class MyApplication extends Application {

    // Define a constant for your notification channel ID
    // As per PDF "General Notifications"
    public static final String NOTIFICATION_CHANNEL_ID = "GeneralNotifications";


    @Override
    public void onCreate() {
        // Enable CleverTap Debugging
        CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.DEBUG);

        // Register for Activity Lifecycle events
        // This should be called before super.onCreate()
        ActivityLifecycleCallback.register(this);

        super.onCreate();

        // Create the notification channel
        createNotificationChannel();

        // You can get the default instance after initialization if needed
        // CleverTapAPI clevertapDefaultInstance = CleverTapAPI.getDefaultInstance(getApplicationContext());
        // if (clevertapDefaultInstance != null) {
        //    // clevertapDefaultInstance is ready for use
        // }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ (Android 8.0 Oreo and above)
        // because the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "General Notifications"; // Channel name as per PDF
            String description = "Channel for general app notifications"; // Channel description
            int importance = NotificationManager.IMPORTANCE_DEFAULT; // Default importance

            // Create the NotificationChannel object
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            // You can't change the importance or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                System.out.println("Notification Channel '" + NOTIFICATION_CHANNEL_ID + "' created.");
            } else {
                System.err.println("NotificationManager is null, cannot create channel.");
            }
        } else {
            System.out.println("Notification Channels not needed for API < 26.");
        }
    }
}