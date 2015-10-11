package com.github.chemikadze.garminauthscanner;

import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;

import java.util.Collection;
import java.util.Map;

public interface AuthenticatorManager {

    public void sendAccountToDevices(AuthAccount account, Map<IQDevice, IQApp> devices);

    public static interface ScanProcessCallback {

        void onSdkInit();

        void onSdkReady();

        void onDeviceSearchStarted();

        void onDevicesFound(Collection<IQDevice> devices);

        void onApplicationResolveStarted();

        void onApplicationsResolved(Map<IQDevice, IQApp> apps);

        void onSendStarted();

        void onSendFinished();

        void onFailure(String reason);

    }

}
