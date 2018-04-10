/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.androidthings.updatemanagersample;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.things.update.StatusListener;
import com.google.android.things.update.UpdateManager;
import com.google.android.things.update.UpdateManagerStatus;
import com.google.android.things.update.UpdatePolicy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Activity demonstrating an app that will apply updates automatically but defers reboot until a
 * time when it is less disruptive. Restarts are scheduled for the next 2AM instance.
 * <p>
 * This includes UI affordances to check for updates and to restart the device immediately rather
 * than wait for the scheduled restart.
 */
public class UpdateActivity extends Activity implements StatusListener, OnClickListener {

    private static final String TAG = "UpdateActivity";

    private static final List<String> UPDATE_CHANNELS =
            Arrays.asList("stable-channel", "beta-channel", "dev-channel", "canary-channel");

    private static final int DATE_FORMAT_FLAGS =
            DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME;

    private TextView mStatus;
    private TextView mPendingVersion;
    private TextView mRebootMessage;
    private TextView mCheckButton;
    private Button mRestartButton;
    private Spinner mChannelSpinner;

    private Handler mHandler;
    private UpdateManager mUpdateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        mStatus = findViewById(R.id.status);
        mPendingVersion = findViewById(R.id.pending_version);
        mRebootMessage = findViewById(R.id.reboot_message);

        mCheckButton = findViewById(R.id.btn_check_now);
        mCheckButton.setOnClickListener(this);
        mRestartButton = findViewById(R.id.btn_restart_now);
        mRestartButton.setOnClickListener(this);

        mHandler = new Handler();
        mUpdateManager = UpdateManager.getInstance();
        // Set the policy to apply but not reboot. If for some reason we aren't getting rebooted
        // by the alarms we set, fall back to something sensible, like 5 days.
        UpdatePolicy policy = new UpdatePolicy.Builder()
                .setPolicy(UpdatePolicy.POLICY_APPLY_ONLY)
                .setApplyDeadline(5L, TimeUnit.DAYS)
                .build();
        mUpdateManager.setPolicy(policy);

        // Set up the channel selector
        mChannelSpinner = findViewById(R.id.channel_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, android.R.id.text1, UPDATE_CHANNELS);
        mChannelSpinner.setAdapter(adapter);
        // Show the current channel
        String channel = mUpdateManager.getChannel();
        mChannelSpinner.setSelection(UPDATE_CHANNELS.indexOf(channel));
        // Change the channel on selection
        mChannelSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> spinner, View view, int position, long id) {
                mUpdateManager.setChannel(UPDATE_CHANNELS.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> spinner) {}
        });

        // Start listening to UpdateManager.
        mUpdateManager.addStatusListener(this);
        // Adding a listener doesn't call back with the current status, so poll for it once.
        UpdateManagerStatus status = mUpdateManager.getStatus();
        // Display the current version.
        TextView currentVersion = findViewById(R.id.current_version);
        currentVersion.setText(getString(R.string.current_version_info,
                status.currentVersionInfo.androidThingsVersion,
                status.currentVersionInfo.buildId));
        // Configure remaining UI based on status.
        handleStatusUpdate(status);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUpdateManager.removeStatusListener(this);
    }

    @Override
    public void onStatusUpdate(final UpdateManagerStatus status) {
        if (mHandler.getLooper().isCurrentThread()) {
            // We're on the main thread.
            handleStatusUpdate(status);
        } else {
            // Bug: UpdateManager can deliver status on background threads. Since we need to update
            // views, send it over to the main thread.
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleStatusUpdate(status);
                }
            });
        }
    }

    private void handleStatusUpdate(UpdateManagerStatus status) {
        Log.d(TAG, "handleStatusUpdate: " + status);

        mChannelSpinner.setEnabled(false);
        mCheckButton.setEnabled(false);
        mRestartButton.setEnabled(false);
        mRebootMessage.setVisibility(View.GONE);

        boolean pendingVersionVisible = true;
        switch (status.currentState) {
            case UpdateManagerStatus.STATE_IDLE:
                mStatus.setText(R.string.status_idle);
                mCheckButton.setEnabled(true);
                mChannelSpinner.setEnabled(true);
                pendingVersionVisible = false;
                break;

            case UpdateManagerStatus.STATE_UPDATE_AVAILABLE:
                mStatus.setText(R.string.status_update_available);
                break;

            case UpdateManagerStatus.STATE_DOWNLOADING_UPDATE:
                int percent = (int) (status.pendingUpdateInfo.downloadProgress * 100);
                mStatus.setText(getString(R.string.status_downloading, percent));
                break;

            case UpdateManagerStatus.STATE_FINALIZING_UPDATE:
                mStatus.setText(R.string.status_finalizing);
                break;

            case UpdateManagerStatus.STATE_UPDATED_NEEDS_REBOOT:
                mStatus.setText(R.string.status_needs_reboot);
                mRestartButton.setEnabled(true);

                long time = getNext2amTime();
                String displayTime = DateUtils.formatDateTime(this, time, DATE_FORMAT_FLAGS);
                mRebootMessage.setText(getString(R.string.reboot_message, displayTime));
                mRebootMessage.setVisibility(View.VISIBLE);

                scheduleReboot(time);
                break;

            case UpdateManagerStatus.STATE_REPORTING_ERROR:
                mStatus.setText(R.string.status_error);
                break;
        }

        if (pendingVersionVisible && status.pendingUpdateInfo != null) {
            mPendingVersion.setVisibility(View.VISIBLE);
            mPendingVersion.setText(getString(R.string.pending_version_info,
                    status.pendingUpdateInfo.versionInfo.androidThingsVersion,
                    status.pendingUpdateInfo.versionInfo.buildId));
        } else {
            mPendingVersion.setVisibility(View.GONE);
        }
    }

    /**
     * Compute the next 2AM instance that is after the current time.
     *
     * @return the next 2AM instance, in milliseconds since epoch.
     */
    private long getNext2amTime() {
        Calendar nextUpdate = Calendar.getInstance();
        if (nextUpdate.get(Calendar.HOUR_OF_DAY) >= 2) {
            // We've passed 2AM today, so add 1 day before setting the remaining fields.
            nextUpdate.add(Calendar.DAY_OF_MONTH, 1);
        }
        nextUpdate.set(Calendar.HOUR_OF_DAY, 2);
        nextUpdate.set(Calendar.MINUTE, 0);
        return nextUpdate.getTimeInMillis();
    }

    /**
     * Schedule a device reboot at the specified time.
     *
     * @param time wallclock time in milliseconds since epoch.
     */
    private void scheduleReboot(long time) {
        Intent intent = DeviceRebootReceiver.getRestartIntent(this);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_check_now:
                mCheckButton.setEnabled(false); // avoid double taps

                // If this returns false, there's already an update check in progress and the UI
                // should update when we get the next status.
                mUpdateManager.performUpdateNow(UpdatePolicy.POLICY_APPLY_ONLY);
                break;

            case R.id.btn_restart_now:
                mRestartButton.setEnabled(false); // avoid double taps
                mRestartButton.setText(R.string.restarting);

                sendBroadcast(DeviceRebootReceiver.getRestartIntent(this));
                break;
        }
    }
}
