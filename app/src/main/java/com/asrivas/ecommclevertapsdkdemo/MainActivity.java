package com.asrivas.ecommclevertapsdkdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
    private Button buttonRequestPushPermission;
    private Button buttonTriggerInApp; // Declare the new button

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
        buttonRequestPushPermission = findViewById(R.id.buttonRequestPushPermission);
        buttonTriggerInApp = findViewById(R.id.buttonTriggerInApp); // Initialize the new button

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Push Notifications permission granted.", Toast.LENGTH_SHORT).show();
                        updateCleverTapPushSubscription(true);
                    } else {
                        Toast.makeText(this, "Push Notifications permission denied.", Toast.LENGTH_SHORT).show();
                        updateCleverTapPushSubscription(false);
                    }
                });

        pushAppLaunchedEvent();

        if (buttonLogin != null) {
            buttonLogin.setOnClickListener(v -> loginUser());
        }

        if (buttonRecordTestEvent != null) {
            buttonRecordTestEvent.setOnClickListener(v -> recordTestEvent());
        }

        if (buttonRequestPushPermission != null) {
            buttonRequestPushPermission.setOnClickListener(v -> requestPushNotificationPermission());
        }

        // Set OnClickListener for the new "Trigger In-App Event" button
        if (buttonTriggerInApp != null) {
            buttonTriggerInApp.setOnClickListener(v -> triggerInAppEvent());
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

    private void requestPushNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Push Notifications permission already granted.", Toast.LENGTH_SHORT).show();
                updateCleverTapPushSubscription(true);
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Toast.makeText(this, "Please grant notification permission to receive updates.", Toast.LENGTH_LONG).show();
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            Toast.makeText(this, "Push Notifications permission not required for this Android version. Assuming subscribed.", Toast.LENGTH_LONG).show();
            updateCleverTapPushSubscription(true);
        }
    }

    private void updateCleverTapPushSubscription(boolean subscribed) {
        if (clevertapDefaultInstance != null) {
            HashMap<String, Object> profileUpdate = new HashMap<>();
            profileUpdate.put("MSG-push", subscribed);
            clevertapDefaultInstance.pushProfile(profileUpdate);
            Log.d("MainActivity", "CleverTap profile updated with MSG-push: " + subscribed);
        } else {
            Log.w("MainActivity", "CleverTap instance is null. Cannot update MSG-push status.");
        }
    }

    // New method to handle "Trigger In-App Event"
    private void triggerInAppEvent() {
        if (clevertapDefaultInstance != null) {
            // Raise an event named "ShowInApp" as per PDF
            // This event does not have properties specified in the PDF for this particular trigger
            clevertapDefaultInstance.pushEvent("ShowInApp");

            // Feedback (not specified in PDF, but good for testing)
            Toast.makeText(this, "'ShowInApp' event triggered", Toast.LENGTH_LONG).show();
            System.out.println("CleverTap: Pushed 'ShowInApp' event.");
        } else {
            Toast.makeText(this, "CleverTap instance not available", Toast.LENGTH_SHORT).show();
            System.out.println("CleverTap: Default instance is null, cannot push 'ShowInApp' event.");
        }
    }
}