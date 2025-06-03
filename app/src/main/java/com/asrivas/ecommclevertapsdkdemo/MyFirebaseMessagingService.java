package com.asrivas.ecommclevertapsdkdemo;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.pushnotification.NotificationInfo; // Import NotificationInfo
import com.clevertap.android.sdk.pushnotification.fcm.CTFcmMessageHandler; // Import CTFcmMessageHandler
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map; // Import Map

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM token: " + token);

        try {
            CleverTapAPI clevertapInstance = CleverTapAPI.getDefaultInstance(getApplicationContext());
            if (clevertapInstance != null) {
                clevertapInstance.pushFcmRegistrationId(token, true);
                Log.d(TAG, "FCM token passed to CleverTap: " + token);
            } else {
                Log.w(TAG, "CleverTap instance is null in onNewToken. FCM token not passed yet.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error passing FCM token to CleverTap", e);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM Message Id: " + remoteMessage.getMessageId());
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "FCM Notification Title: " + remoteMessage.getNotification().getTitle());
            Log.d(TAG, "FCM Notification Body: " + remoteMessage.getNotification().getBody());
        }
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "FCM Data Message: " + remoteMessage.getData());
        }

        try {
            // Check if the message has data payload
            if (remoteMessage.getData().size() > 0) {
                // Create a Bundle from the RemoteMessage data
                Bundle extras = new Bundle();
                for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
                    extras.putString(entry.getKey(), entry.getValue());
                }

                // Check if the notification is from CleverTap
                NotificationInfo info = CleverTapAPI.getNotificationInfo(extras);

                if (info.fromCleverTap) {
                    Log.d(TAG, "Message is from CleverTap. Handing over to CTFcmMessageHandler.");
                    // Let CleverTap's FCM message handler process the message
                    // This will create the notification and track impressions, clicks, etc.
                    new CTFcmMessageHandler().createNotification(getApplicationContext(), remoteMessage);
                } else {
                    Log.d(TAG, "Non-CleverTap notification received. Handle separately if needed.");
                    // Handle non-CleverTap push notifications here if your app uses other FCM services
                    // For this assignment, we are focused on CleverTap pushes.
                }
            } else {
                Log.d(TAG, "Received FCM message without data payload. Might be a display notification handled by system tray.");
                // If it's a "notification" message without a "data" payload, FCM itself might display it
                // when the app is in the background.
            }
        } catch (Throwable t) {
            // Added Throwable to catch more potential issues during parsing, though specific exceptions are better.
            Log.e(TAG, "Error processing FCM message", t);
        }
    }
}