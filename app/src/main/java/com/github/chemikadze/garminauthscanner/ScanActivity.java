package com.github.chemikadze.garminauthscanner;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.commons.codec.binary.Base32;

import java.net.URL;
import java.util.Collection;
import java.util.Map;


public class ScanActivity extends ActionBarActivity implements AuthenticatorManager.ScanProcessCallback {

    private static String STATE_CODE_SCAN_TRIGGERED = "SCAN_TRIGGERED";

    private ConnectIQ sdk;
    private AuthenticatorManagerImpl manager;
    private Collection<IQDevice> devices;
    private Map<IQDevice, IQApp> apps;

    private AuthAccount account;
    private boolean scanTriggered;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        if (savedInstanceState != null) {
            scanTriggered = savedInstanceState.getBoolean(STATE_CODE_SCAN_TRIGGERED, false);
        } else {
            scanTriggered = false;
        }
        if (!scanTriggered) {
            scanCode();
        }
        scanTriggered = !scanTriggered;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (account == null) {
            outState.putSerializable(STATE_CODE_SCAN_TRIGGERED, scanTriggered);
        } else {
            outState.putSerializable(STATE_CODE_SCAN_TRIGGERED, scanTriggered);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (sdk != null) {
                sdk.shutdown(getApplicationContext());
            }
        } catch (InvalidStateException e) {
            // ignore
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_open_iq_store) {
            openAppInBrowser();
            return true;
        } else if (id == R.id.action_scan_code) {
            scanCode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openAppInBrowser() {
        final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getApplicationContext().getString(R.string.app_url)));
        startActivity(intent);
    }

    private void scanCode() {
        scanTriggered = true;
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result == null || resultCode != RESULT_OK || result.getContents().length() == 0) {
                progress(70, getString(R.string.status_cancelled));
            } else {
                Uri uri = Uri.parse(result.getContents());
                if (!uri.getScheme().equals("otpauth")) {
                    onFailure(String.format("Expected otpauth://, got %s://", uri.getScheme()));
                } else {
                    account = accountFromUrl(uri);
                    initiateSendProcess();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private AuthAccount accountFromUrl(Uri uri) {
        String[] nameCandidates = uri.getPath().split(":", 2);
        String name;
        if (nameCandidates.length == 1) {
            name = uri.getPath();
        } else {
            if (nameCandidates[0].equals("Google")) {
                name = nameCandidates[1];
            } else {
                name = nameCandidates[0];
            }
        }
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
//        String b64code = uri.getQueryParameter("secret");
//        String b32code = new Base32().encodeAsString(Base64.decode(b64code, Base64.DEFAULT));
        String b32code = uri.getQueryParameter("secret");
        return new AuthAccount(name, b32code);
    }

    private void initiateSendProcess() {
        sdk = ConnectIQ.getInstance(getApplicationContext(), ConnectIQ.IQConnectType.WIRELESS);
        manager = new AuthenticatorManagerImpl(getApplicationContext(), sdk, this);
        sdk.initialize(getApplicationContext(), true, manager);
    }

    // ScanProcessCallback


    @Override
    public void onSdkInit() {
        progress(0, getString(R.string.status_initializing));
    }

    @Override
    public void onSdkReady() {
        progress(30, getString(R.string.status_initialized));
    }

    @Override
    public void onDeviceSearchStarted() {
        progress(35, getString(R.string.status_searching));
    }

    @Override
    public void onDevicesFound(Collection<IQDevice> devices) {
        progress(40, getString(R.string.status_devices_found));
    }

    @Override
    public void onApplicationResolveStarted() {
        progress(50, getString(R.string.status_resolve_started));
    }

    @Override
    public void onApplicationsResolved(Map<IQDevice, IQApp> apps) {
        progress(55, getString(R.string.status_resolved));
        manager.sendAccountToDevices(account, apps);
    }

    @Override
    public void onSendStarted() {
        progress(60, getString(R.string.status_sending));
    }

    @Override
    public void onSendFinished() {
        progress(70, getString(R.string.status_completed));
    }

    @Override
    public void onFailure(String reason) {
        ProgressBar bar = (ProgressBar)findViewById(R.id.progress_bar);
        TextView label = (TextView)findViewById(R.id.progress_comment);
        label.setText(reason);
        bar.setVisibility(View.INVISIBLE);
    }

    private void progress(int percent, String status) {
        ProgressBar bar = (ProgressBar)findViewById(R.id.progress_bar);
        TextView label = (TextView)findViewById(R.id.progress_comment);
        bar.setProgress(percent);
        label.setText(status);
    }
}
