package com.asrivas.ecommclevertapsdkdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.displayunits.DisplayUnitListener;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnitContent;


import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements DisplayUnitListener {

    private CleverTapAPI clevertapDefaultInstance;
    private EditText editTextName, editTextEmail, editTextPhone, editTextGender;
    private Button buttonLogin, buttonRecordTestEvent, buttonRequestPushPermission;
    private Button buttonTriggerInApp, buttonOpenAppInbox, buttonUpdateProfile;
    private Button buttonLoadNativeAd;

    private LinearLayout nativeDisplayContainer;
    private ImageView nativeImageView;
    private TextView nativeTitleTextView;
    private TextView nativeMessageTextView;

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

        if (clevertapDefaultInstance != null) {
            clevertapDefaultInstance.initializeInbox();
            clevertapDefaultInstance.setDisplayUnitListener(this); // Register listener for Native Display
            Log.d("MainActivity", "CleverTap App Inbox initialized & Native Display listener registered.");
        }

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextGender = findViewById(R.id.editTextGender);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonUpdateProfile = findViewById(R.id.buttonUpdateProfile);
        buttonRecordTestEvent = findViewById(R.id.buttonRecordTestEvent);
        buttonRequestPushPermission = findViewById(R.id.buttonRequestPushPermission);
        buttonTriggerInApp = findViewById(R.id.buttonTriggerInApp);
        buttonOpenAppInbox = findViewById(R.id.buttonOpenAppInbox);

        nativeDisplayContainer = findViewById(R.id.nativeDisplayContainer);
        nativeImageView = findViewById(R.id.nativeImageView);
        nativeTitleTextView = findViewById(R.id.nativeTitleTextView);
        nativeMessageTextView = findViewById(R.id.nativeMessageTextView);
        buttonLoadNativeAd = findViewById(R.id.buttonLoadNativeAd);


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

        if (buttonLogin != null) buttonLogin.setOnClickListener(v -> loginUser());
        if (buttonUpdateProfile != null) buttonUpdateProfile.setOnClickListener(v -> updateGenderProfileProperty());
        if (buttonRecordTestEvent != null) buttonRecordTestEvent.setOnClickListener(v -> recordTestEvent());
        if (buttonRequestPushPermission != null) buttonRequestPushPermission.setOnClickListener(v -> requestPushNotificationPermission());
        if (buttonTriggerInApp != null) buttonTriggerInApp.setOnClickListener(v -> triggerInAppEvent());
        if (buttonOpenAppInbox != null) buttonOpenAppInbox.setOnClickListener(v -> openAppInbox());
        if (buttonLoadNativeAd != null) buttonLoadNativeAd.setOnClickListener(v -> loadNativeDisplayUnit());
    }

    @Override
    public void onDisplayUnitsLoaded(ArrayList<CleverTapDisplayUnit> units) {
        Log.d("MainActivity", "Native Display Units Loaded: " + units.size());
        if (units.isEmpty()) {
            Toast.makeText(this, "No Native Display units found for the trigger event.", Toast.LENGTH_SHORT).show();
            if (nativeDisplayContainer != null) nativeDisplayContainer.setVisibility(View.GONE);
            return;
        }

        final CleverTapDisplayUnit unit = units.get(0); // Using the first available unit

        if (unit.getContents() != null && !unit.getContents().isEmpty()) {
            CleverTapDisplayUnitContent content = unit.getContents().get(0);

            String title = content.getTitle();
            String message = content.getMessage();
            String imageUrl = content.getMedia();

            if (nativeTitleTextView != null) nativeTitleTextView.setText(title);
            if (nativeMessageTextView != null) nativeMessageTextView.setText(message);

            if (nativeImageView != null && imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this).load(imageUrl).into(nativeImageView);
            }

            if (nativeDisplayContainer != null) {
                nativeDisplayContainer.setVisibility(View.VISIBLE);
                nativeDisplayContainer.setOnClickListener(view -> {
                    if (clevertapDefaultInstance != null) {
                        clevertapDefaultInstance.pushDisplayUnitClickedEventForID(unit.getUnitID());
                        Log.d("MainActivity", "Native Display Unit Clicked: " + unit.getUnitID());
                        Toast.makeText(MainActivity.this, "Native Ad Clicked!", Toast.LENGTH_SHORT).show();
                    }
                    // Handle deep link if available from content.getActionUrl() or unit.getCustomExtras()
                });
            }

            if (clevertapDefaultInstance != null) {
                clevertapDefaultInstance.pushDisplayUnitViewedEventForID(unit.getUnitID());
                Log.d("MainActivity", "Native Display Unit Viewed: " + unit.getUnitID());
            }

        } else {
            Log.w("MainActivity", "Native Display unit content is empty.");
            if (nativeDisplayContainer != null) nativeDisplayContainer.setVisibility(View.GONE);
        }
    }

    // Method called when "Load Native Ad" button is clicked
    private void loadNativeDisplayUnit() {
        if (clevertapDefaultInstance != null) {
            // Push a custom event that can be used to trigger the Native Display campaign
            clevertapDefaultInstance.pushEvent("LoadNativeAdClicked");
            Log.d("MainActivity", "Pushed 'LoadNativeAdClicked' event.");
            Toast.makeText(this, "Triggered event for Native Display...", Toast.LENGTH_SHORT).show();

            // The SDK will fetch display units based on campaigns triggered by this event (or other triggers).
            // The onDisplayUnitsLoaded callback will be invoked if units are available.
            // Calling getAllDisplayUnits() here can also prompt a refresh if needed,
            // but the event trigger is the primary mechanism for campaign delivery.
            // clevertapDefaultInstance.getAllDisplayUnits(); // You can keep or remove this based on observed behavior
        } else {
            Toast.makeText(this, "CleverTap instance not available", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Other methods (pushAppLaunchedEvent, loginUser, etc. remain unchanged) ---
    private void pushAppLaunchedEvent() {
        if (clevertapDefaultInstance != null) {
            HashMap<String, Object> appLaunchedProperties = new HashMap<>();
            appLaunchedProperties.put("Platform", "Android");
            String sdkVersion = "1.0";
            appLaunchedProperties.put("SDK Version", sdkVersion);
            clevertapDefaultInstance.pushEvent("App Launched", appLaunchedProperties);
            Log.d("MainActivity", "CleverTap: Pushed 'App Launched' event with SDK Version: " + sdkVersion);
        } else {
            Log.w("MainActivity", "CleverTap: Default instance is null, cannot push 'App Launched' event.");
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
            Log.d("MainActivity", "CleverTap: Login Profile Pushed: " + profileUpdate.toString());
        } else {
            Toast.makeText(this, "CleverTap instance not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateGenderProfileProperty() {
        if (editTextGender == null) {
            Toast.makeText(this, "Gender field not initialized.", Toast.LENGTH_SHORT).show();
            return;
        }
        String gender = editTextGender.getText().toString().trim();

        if (gender.isEmpty()) {
            Toast.makeText(this, "Please enter your gender.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (clevertapDefaultInstance != null) {
            HashMap<String, Object> profileUpdate = new HashMap<>();
            profileUpdate.put("Gender", gender);

            clevertapDefaultInstance.pushProfile(profileUpdate);

            Toast.makeText(this, "Profile Updated with Gender", Toast.LENGTH_LONG).show();
            Log.d("MainActivity", "CleverTap: Profile updated with Gender: " + gender);
        } else {
            Toast.makeText(this, "CleverTap instance not available", Toast.LENGTH_SHORT).show();
            Log.w("MainActivity", "CleverTap: Default instance is null. Cannot update Gender.");
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
            Log.d("MainActivity", "CleverTap: Pushed 'Test Event Clicked': " + testEventProperties.toString());
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

    private void triggerInAppEvent() {
        if (clevertapDefaultInstance != null) {
            clevertapDefaultInstance.pushEvent("ShowInApp");
            Toast.makeText(this, "'ShowInApp' event triggered", Toast.LENGTH_LONG).show();
            Log.d("MainActivity", "CleverTap: Pushed 'ShowInApp' event.");
        } else {
            Toast.makeText(this, "CleverTap instance not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppInbox() {
        if (clevertapDefaultInstance != null) {
            clevertapDefaultInstance.showAppInbox();
            Log.d("MainActivity", "Attempting to show App Inbox with no arguments.");
        } else {
            Toast.makeText(this, "CleverTap instance not available", Toast.LENGTH_SHORT).show();
            Log.w("MainActivity", "CleverTap: Default instance is null, cannot open App Inbox.");
        }
    }
}