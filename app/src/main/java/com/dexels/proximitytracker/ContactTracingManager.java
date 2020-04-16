package com.dexels.proximitytracker;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;

import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import androidx.annotation.Nullable;

public class ContactTracingManager implements ContactTracingService {

    private static final ParcelUuid CONTACT_DETECTION_SERVICE_UUID = new ParcelUuid(UUID.fromString("0000FD6F-0000-1000-8000-00805F9B34FB")); // see https://en.wikipedia.org/wiki/Bluetooth_Low_Energy#Software_model

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private boolean scanning = false;

    private static byte[] proximityIdentifier = {0,0,0,0,0,0,0,0,0,0,0,0,1,2,3,4};

    private static AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder() //
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY) //
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) //
            .setConnectable(false) //
            .build();
    private static AdvertiseData advertiseData = new AdvertiseData.Builder() //
            .addServiceUuid(CONTACT_DETECTION_SERVICE_UUID) //
            .setIncludeTxPowerLevel(true)
            .addServiceData(CONTACT_DETECTION_SERVICE_UUID, proximityIdentifier) //
            .build();

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            System.out.println("start success: " + settingsInEffect);
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            System.out.println("start failure: " + errorCode);
            super.onStartFailure(errorCode);
        }
    };

    private static ScanSettings scanSettings = new ScanSettings.Builder() //
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // TODO this is useful for testing, might be changed for release
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY) // TODO this seems to be a good setting for close devices that stay around for a while
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // we want all results, not only the first or errors
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .build();
    private static ScanFilter scanFilter = new ScanFilter.Builder() //
            .setServiceUuid(CONTACT_DETECTION_SERVICE_UUID) //
            .build();
    private static List<ScanFilter> scanFilters = new ArrayList<>();

    private ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            System.out.println("batch results");
            for (ScanResult scanResult : results) {
                System.out.println("Found: " + scanResult);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            System.out.println("scan failed: " + errorCode);
            super.onScanFailed(errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            System.out.println("scan result: " + result);
            super.onScanResult(callbackType, result);
        }
    };

    static {
        scanFilters.add(scanFilter);
    }

    public ContactTracingManager(Context context) {
        this.context = context;
    }

    private abstract class BLETask extends Task<Integer> {

        private List<OnSuccessListener<? super Integer>> successListeners = new ArrayList<>();
        private List<OnCompleteListener<? super Integer>> completeListeners = new ArrayList<>();
        private List<OnFailureListener> failureListeners = new ArrayList<>();

        @Status
        private Integer status;

        @Nullable
        private Exception exception = null;

        private BLETask() {
            try {
                bluetoothAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
                // Ensures Bluetooth is available on the device and it is enabled. If not,
                // displays a dialog requesting user permission to enable Bluetooth.
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    status = Status.FAILED_SERVICE_DISABLED;
                } else {
                    status = execute(bluetoothAdapter);
                }
            } catch (Exception e) {
                exception = e;
            }
        }

        @Override
        public Integer getResult() {
            return status;
        }

        @Override
        public <X extends Throwable> Integer getResult(Class<X> exceptionType) throws X {
            if (exception != null) {
                throw (X) exception;
            }
            return status;
        }

        @Override
        public Exception getException() {
            return exception;
        }

        @Override
        public boolean isComplete() {
            return status != null;
        }

        @Override
        public boolean isSuccessful() {
            return exception != null;
        }

        @Override
        public Task<Integer> addOnSuccessListener(OnSuccessListener<? super Integer> listener) {
            successListeners.add(listener);
            return this;
        }

        @Override
        public Task<Integer> addOnSuccessListener(Executor executor, OnSuccessListener<? super Integer> listener) {
            return null;
        }

        @Override
        public Task<Integer> addOnFailureListener(OnFailureListener listener) {
            failureListeners.add(listener);
            return this;
        }

        @Override
        public Task<Integer> addOnFailureListener(Executor executor, OnFailureListener listener) {
            return null;
        }

        @Override
        public Task<Integer> addOnCompleteListener(OnCompleteListener<Integer> listener) {
            completeListeners.add(listener);
            return this;
        }

        @Override
        public Task<Integer> addOnCompleteListener(Executor executor, OnCompleteListener<Integer> listener) {
            return null;
        }

        abstract int execute(BluetoothAdapter bluetoothAdapter);


    }

    private class StartContactTracingTask extends BLETask {

        @Override
        int execute(final BluetoothAdapter bluetoothAdapter) {
            if (scanning) {
                return Status.SUCCESS;
            }
            scanning = true;
            System.out.println(advertiseData);
            bluetoothAdapter.getBluetoothLeAdvertiser().startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
            bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, scanSettings, scanCallback);
            System.out.println("started advertising/scanning");
            return Status.SUCCESS;
        }


    }

    private class StopContactTracingTask extends BLETask {

        @Override
        int execute(final BluetoothAdapter bluetoothAdapter) {
            if (!scanning) {
                return Status.SUCCESS;
            }
            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            bluetoothAdapter.getBluetoothLeAdvertiser().stopAdvertising(advertiseCallback);
            scanning = false;
            return Status.SUCCESS;
        }


    }

    @Override
    public Task<Integer> startContactTracing(PendingIntent contactTracingCallback) {
        return new StartContactTracingTask();
    }

    @Override
    public void handleIntent(Intent intentCallback, ContactTracingCallback callback) {

    }

    @Override
    public Task<Integer> stopContactTracing() {
        return new StopContactTracingTask();
    }

    @Override
    public Task<Integer> isContactTracingEnabled() {
        return null;
    }

    @Override
    public Task<Integer> startSharingDailyTracingKeys() {
        return null;
    }

    @Override
    public Task<Integer> provideDiagnosisKeys(List<DailyTracingKey> keys) {
        return null;
    }

    @Override
    public int getMaxDiagnosisKeys() {
        return 0;
    }

    @Override
    public Task<Boolean> hasContact() {
        return null;
    }

    @Override
    public Task<List<ContactInfo>> getContactInformation() {
        return null;
    }
}
