# Android Things Device Updates Sample

This application demonstrates how to interact with the device update service on
Android Things through the `UpdateManager` API. By default, the system checks for
new OTA updates every few hours and applies them automatically. This example shows
you how to configure the update policy to allow an app to take more control over
the update process and monitor progress.

## Pre-requisites

- Android Things compatible board
- Android Studio 2.2+

## Getting Started

Import the project using Android Studio and deploy it to your device:

1.  Stage a new OTA update for your device using the
    [Android Things Console](https://developer.android.com/things/console/index.html).
1.  Press the **Check Now** button on the device to force a manual update check.
1.  The update status is displayed in the UI as the device downloads and verifies
    the new update.
1.  Press the **Reboot Now** button to reboot the device and complete the update
    process.

## Enable auto-launch behavior

This sample app is currently configured to launch only when deployed from your
development machine. To enable the main activity to launch automatically on boot,
add the following `intent-filter` to the app's manifest file:

```xml
<activity ...>

    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.HOME"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>

</activity>
```

## License

Copyright 2017 The Android Open Source Project, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
