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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.things.device.DeviceManager;


/**
 * BroadcastReceiver that reboots the device.
 */
public class DeviceRebootReceiver extends BroadcastReceiver {

    private static final String TAG = "DeviceRebootReceiver";

    private static final String ACTION_REBOOT = "updatemanagersample.action.REBOOT";

    /**
     * Construct an Intent that is configured to target this receiver and cause it to reboot the
     * device.
     *
     * @param context a Context
     * @return an Intent to be delivered to this receiver
     */
    public static Intent getRestartIntent(Context context) {
        Intent intent = new Intent(context, DeviceRebootReceiver.class);
        intent.setAction(ACTION_REBOOT);
        return intent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_REBOOT.equals(intent.getAction())) {
            Log.d(TAG, "Ignoring invalid broadcast");
            return;
        }

        // Anything your app needs to clean up, do it here. Note that if this could potentially take
        // a long time, you should use a Service instead of a BroadcastReceiver, and from there do
        // your work and reboot.

        DeviceManager deviceManager = DeviceManager.getInstance();
        // Add parameters if you want to reboot differently than normal.
        deviceManager.reboot();
    }
}
