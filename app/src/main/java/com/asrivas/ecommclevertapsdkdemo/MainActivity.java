package com.asrivas.ecommclevertapsdkdemo;

import android.Manifest; // Import Manifest
import android.content.pm.PackageManager; // Import PackageManager
import android.os.Build; // Import Build
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher; // Import ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts; // Import ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.clevertap.android.sdk.CleverTapAPI;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private CleverTapAPI clevertapDefaultInstance;
    private EditText editTextName, editTextEmail, editTextPhone;
    private Button buttonLogin;
    private Button buttonRecordTestEvent;
    private Button buttonRequestPushPermission; // Declare the new button

    // Declare the ActivityResultLauncher for permission request
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        try {
            clevertapDefaultInstance = CleverTapAPI.getDefaultInstance(getApplicationContext());
        } catch (Exception e) {
            System.err.println("CleverTap Initialization error: " + e.getMessage());
        }

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRecordTestEvent = findViewById(R.id.buttonRecordTestEvent);
        buttonRequestPushPermission = findViewById(R.id.buttonRequestPushPermission); // Initialize the new button

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the ActivityResultLauncher
        // This should be initialized in onCreate or as a field initializer
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Push Notifications permission granted.", Toast.LENGTH_SHORT).show();
                        // You can re-pass the FCM token to CleverTap if needed, though SDK usually handles it.
                        // Example: if (clevertapDefaultInstance != null) { clevertapDefaultInstance.pushFcmRegistrationId(yourFcmToken,true); }
                        // For now, the toast is sufficient as per assignment focus on permission request.
                    } else {
                        Toast.makeText(this, "Push Notifications permission denied.", Toast.LENGTH_SHORT).show();
                    }
                });

        pushAppLaunchedEvent();

        if (buttonLogin != null) {
            buttonLogin.setOnClickListener(v -> loginUser());
        }

        if (buttonRecordTestEvent != null) {
            buttonRecordTestEvent.setOnClickListener(v -> recordTestEvent());
        }

        // Set OnClickListener for the "Request Push Permission" button
        if (buttonRequestPushPermission != null) {
            buttonRequestPushPermission.setOnClickListener(v -> requestPushNotificationPermission());
        }
    }

    private void pushAppLaunchedEvent() {
        if (clevertapDefaultInstance != null) {
            HashMap<String, Object> appLaunchedProperties = new HashMap<>();
            appLaunchedProperties.put("Platform", "Android");
            String sdkVersion = "1.0";
            appLaunchedProperties.put("SDK Version", sdkVersion);
            clevertapDefaultInstance.pushEvent("App Launched", appLaunchedProperties);
            System.out.println("CleverTap: Pushed 'App Launched' event with SDK Version: " + sdkVersion);
        } else {
            System.out.println("CleverTap: Default instance is null, cannot push 'App Launched' event.");
        }
    }

    private void loginUser() {
        if (editTextName == null || editTextEmail == null || editTextPhone == null) {
            Toast.makeText(this, "UI elements not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Email cannot be empty for login", Toast.LENGTH_SHORT).show();
            return;
        }

        if (clevertapDefaultInstance != null) {
            HashMap<String, Object> profileUpdate = new HashMap<>();
            if (!name.isEmpty()) profileUpdate.put("Name", name);
            profileUpdate.put("Email", email);
            profileUpdate.put("Identity", email);
            if (!phone.isEmpty()) profileUpdate.put("Phone", phone);
            profileUpdate.put("UserType", "New Hire");
            profileUpdate.put("Status", "Active");
            clevertapDefaultInstance.onUserLogin(profileUpdate);
            Toast.makeText(this, "Login Profile Pushed", Toast.LENGTH_LONG).show();
            System.out.println("CleverTap: Login Profile Pushed: " + profileUpdate.toString());
        } else {
            Toast.makeText(this, "CleverTap instance not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void recordTestEvent() {
        if (clevertapDefaultInstance != null) {
            HashMap<String, Object> testEventProperties = new HashMap<>();
            testEventProperties.put("Source", "Onboarding App");
            testEventProperties.put("ItemsInCart", 3);
            testEventProperties.put("PaymentMethod", "COD");
            clevertapDefaultInstance.pushEvent("Test Event Clicked", testEventProperties);
            Toast.makeText(this, "Test Event Recorded", Toast.LENGTH_LONG).show();
            System.out.println("CleverTap: Pushed 'Test Event Clicked': " + testEventProperties.toString());
        } else {
            Toast.makeText(this, "CleverTap instance not available", Toast.LENGTH_SHORT).show();
        }
    }

    // New method to handle Push Notification permission request
    private void requestPushNotificationPermission() {
        // This is only necessary for API level 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Permission is already granted
                Toast.makeText(this, "Push Notifications permission already granted.", Toast.LENGTH_SHORT).show();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: Explain to the user why your app needs the permission.
                // Then, request the permission.
                // For this assignment, we'll directly request.
                Toast.makeText(this, "Please grant notification permission to receive updates.", Toast.LENGTH_LONG).show();
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Directly request for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Push notification permission is not required for pre-Android 13 devices
            Toast.makeText(this, "Push Notifications permission not required for this Android version.", Toast.LENGTH_LONG).show();
        }
    }
}