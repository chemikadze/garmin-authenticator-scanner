package com.github.chemikadze.garminauthscanner;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AuthenticatorManagerImpl implements
        AuthenticatorManager,
        ConnectIQ.ConnectIQListener,
        ConnectIQ.IQApplicationEventListener,
        ConnectIQ.IQDeviceEventListener {

    private String LOG_TAG = getClass().getSimpleName();
    private Map<IQDevice, IQApp> registeredApps = new HashMap<IQDevice, IQApp>();

    private ConnectIQ sdk;
    private Context context;
    private AuthenticatorManager.ScanProcessCallback scanProcessCallback;

    AuthenticatorManagerImpl(Context context, ConnectIQ sdk, AuthenticatorManager.ScanProcessCallback scanProcessCallback) {
        this.context = context;
        this.sdk = sdk;
        this.scanProcessCallback = scanProcessCallback;
    }

    @Override
    public void onSdkReady() {
        scanProcessCallback.onSdkReady();
        try {
            scanProcessCallback.onDeviceSearchStarted();
            List<IQDevice> paired = sdk.getKnownDevices();
            if (paired != null && paired.size() > 0) {
                for (IQDevice aDevice : paired) {
                    sdk.registerForDeviceEvents(aDevice, this);
                }
            }
        } catch (InvalidStateException e) {
            Log.e(LOG_TAG, "Failed to get device", e);
            scanProcessCallback.onFailure(context.getString(R.string.sdk_get_devices_failure));
        } catch (ServiceUnavailableException e) {
            Log.e(LOG_TAG, "Failed to get device", e);
            scanProcessCallback.onFailure(context.getString(R.string.sdk_get_devices_failure));
        }
    }

    @Override
    public void onInitializeError(ConnectIQ.IQSdkErrorStatus iqSdkErrorStatus) {
        Toast.makeText(context, R.string.sdk_init_failure, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSdkShutDown() {
        // TODO
    }

    // IQDeviceListener

    @Override
    public void onDeviceStatusChanged(IQDevice iqDevice, IQDevice.IQDeviceStatus iqDeviceStatus) {
        if (iqDeviceStatus == IQDevice.IQDeviceStatus.CONNECTED) {
            try {
                scanProcessCallback.onDevicesFound(Collections.singleton(iqDevice));
                scanProcessCallback.onApplicationResolveStarted();
                sdk.getApplicationInfo(context.getString(R.string.app_id), iqDevice, new AppInfoListener(this, iqDevice));
            } catch (InvalidStateException e) {
                Log.e(LOG_TAG, "Failed to get app info", e);
                scanProcessCallback.onFailure(context.getString(R.string.sdk_app_info_failure));
            } catch (ServiceUnavailableException e) {
                Log.e(LOG_TAG, "Failed to get app info", e);
                scanProcessCallback.onFailure(context.getString(R.string.sdk_app_info_failure));
            }
        } else {
            IQApp app = registeredApps.get(iqDevice);
            if (app != null) {
                try {
                    sdk.unregisterForApplicationEvents(iqDevice, app);
                } catch (InvalidStateException e) {
                    Log.e(LOG_TAG, "Failed to unregister for events", e);
                    scanProcessCallback.onFailure(context.getString(R.string.sdk_device_failure));
                }
            }
        }
    }

    // IQAppInfoListener

    class AppInfoListener implements ConnectIQ.IQApplicationInfoListener {

        private AuthenticatorManagerImpl listener;
        private IQDevice device;

        AppInfoListener(AuthenticatorManagerImpl listener, IQDevice device) {
            this.listener = listener;
            this.device = device;
        }

        @Override
        public void onApplicationNotInstalled(String s) {
        }

        @Override
        public void onApplicationInfoReceived(IQApp iqApp) {
            try {
                registeredApps.put(device, iqApp);
                scanProcessCallback.onApplicationsResolved(Collections.unmodifiableMap(registeredApps));
                sdk.registerForAppEvents(device, iqApp, listener);
            } catch (InvalidStateException e) {
                Log.e(LOG_TAG, "Failed to register for app events", e);
                scanProcessCallback.onFailure(context.getString(R.string.sdk_device_failure));
            }
        }
    }

    @Override
    public void sendAccountToDevices(final AuthAccount account, Map<IQDevice, IQApp> receivers) {
        for (final Map.Entry<IQDevice, IQApp> entry : receivers.entrySet()) {
            Map<String, String> data = new HashMap<String, String>();
            final String name = account.getName();
            final String code = account.getCode();
            data.put("name", name);
            data.put("code", code);
            try {
                sdk.sendMessage(entry.getKey(), entry.getValue(), data, new ConnectIQ.IQSendMessageListener() {
                    @Override
                    public void onMessageStatus(IQDevice iqDevice, IQApp iqApp, ConnectIQ.IQMessageStatus iqMessageStatus) {
                        if (iqMessageStatus == ConnectIQ.IQMessageStatus.SUCCESS) {
                            scanProcessCallback.onSendFinished(iqDevice, account);
                        } else {
                            String msg = context.getString(R.string.msg_send_account_failure, name, entry.getKey().getFriendlyName());
                            scanProcessCallback.onFailure(msg);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to send account", e);
                String msg = context.getString(R.string.msg_send_account_failure, name, entry.getKey().getFriendlyName());
                scanProcessCallback.onFailure(msg);
            }
        }
    }

    // IQAppEventsListener

    @Override
    public void onMessageReceived(IQDevice iqDevice, IQApp iqApp, List<Object> objects, ConnectIQ.IQMessageStatus iqMessageStatus) {

    }

}
