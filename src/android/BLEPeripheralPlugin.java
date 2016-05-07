// (c) 2014-2016 Don Coleman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.megster.cordova.ble.peripheral;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.IntentFilter;

import android.os.ParcelUuid;
import android.provider.Settings;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.*;

public class BLEPeripheralPlugin extends CordovaPlugin {

    // actions
//    private static final String CREATE_SERVICE = "createService";
    private static final String CREATE_SERVICE_FROM_JSON = "createServiceFromJSON";
//    private static final String ADD_CHARACTERISTIC = "addCharacteristic";
//    private static final String PUBLISH_SERVICE = "publishService";
    private static final String SET_CHARACTERISTIC_VALUE = "setCharacteristicValue";
    private static final String START_ADVERTISING = "startAdvertising";
    private static final String SET_CHARACTERISTIC_VALUE_CHANGED_LISTENER = "setCharacteristicValueChangedListener";
//    private static final String SET_DESCRIPTOR_VALUE_CHANGED_LISTENER = "setDescriptorValueChangedListener";
    private static final String SET_BLUETOOTH_STATE_CHANGED_LISTENER = "setBluetoothStateChangedListener";

    private static final String SETTINGS = "settings";
    private static final String ENABLE = "enable";

    // callbacks
    private CallbackContext writeRequestCallback;
    private CallbackContext createServiceFromJSONCallback;
    private CallbackContext enableBluetoothCallback;

    private static final String TAG = "BLEPlugin";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothGattServer gattServer;

    // Android 23 requires new permissions for BluetoothLeScanner.startScan()
    private static final String ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 2;
    private static final int PERMISSION_DENIED_ERROR = 20;
    private CallbackContext permissionCallback;
    private UUID[] serviceUUIDs;

    // Bluetooth state notification
    CallbackContext stateCallback;
    BroadcastReceiver stateReceiver;
    Map<Integer, String> bluetoothStates = new Hashtable<Integer, String>() {{
        put(BluetoothAdapter.STATE_OFF, "off");
        put(BluetoothAdapter.STATE_TURNING_OFF, "turningOff");
        put(BluetoothAdapter.STATE_ON, "on");
        put(BluetoothAdapter.STATE_TURNING_ON, "turningOn");
    }};

    public void onDestroy() {
        removeStateListener();
    }

    public void onReset() {
        removeStateListener();
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        LOG.d(TAG, "action = " + action);

        if (bluetoothAdapter == null) {
            Activity activity = cordova.getActivity();
            bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            // TODO better error
            callbackContext.error("This device does can not create BLE peripherals");
        }

        boolean validAction = true;

        if (action.equals(CREATE_SERVICE_FROM_JSON)) {

            JSONObject serviceDescription = args.getJSONObject(0);
            createServiceFromJSON(callbackContext, serviceDescription);
            createServiceFromJSONCallback = callbackContext;

        } else if (action.equals(START_ADVERTISING)) {

            //UUID[] serviceUUIDs = parseServiceUUIDList(args.getJSONArray(0));
            UUID serviceUUID = uuidFromString(args.getString(0));
            String localName = args.getString(1);
            advertiseService(callbackContext, serviceUUID, localName);

        } else if (action.equals(SET_CHARACTERISTIC_VALUE_CHANGED_LISTENER)) {

            writeRequestCallback = callbackContext;

        } else if (action.equals(SET_CHARACTERISTIC_VALUE)) {

            UUID serviceUUID = uuidFromString(args.getString(0));
            UUID characteristicUUID = uuidFromString(args.getString(1));
            byte[] value = args.getArrayBuffer(2);

            BluetoothGattService service = gattServer.getService(serviceUUID);
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
            boolean success = characteristic.setValue(value);

            if (success) {
                callbackContext.success();

                // TODO
                // if notify && value has changed
                // [manager updateValue:data forCharacteristic:characteristic onSubscribedCentrals:nil];

            } else {
                callbackContext.error("Failed to set value for characteristic " + characteristicUUID + " on service " + serviceUUID);
            }

        } else if (action.equals(SETTINGS)) {

            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            cordova.getActivity().startActivity(intent);
            callbackContext.success();

        } else if (action.equals(ENABLE)) {

            enableBluetoothCallback = callbackContext;
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            cordova.startActivityForResult(this, intent, REQUEST_ENABLE_BLUETOOTH);

        } else if (action.equals(SET_BLUETOOTH_STATE_CHANGED_LISTENER)) {

            if (this.stateCallback != null) {
                callbackContext.error("State callback already registered.");
            } else {
                this.stateCallback = callbackContext;
                addStateListener();
                sendBluetoothStateChange(bluetoothAdapter.getState());
            }

        } else {

            validAction = false;

        }

        return validAction;
    }

    private UUID[] parseServiceUUIDList(JSONArray jsonArray) throws JSONException {
        List<UUID> serviceUUIDs = new ArrayList<UUID>();

        for(int i = 0; i < jsonArray.length(); i++){
            String uuidString = jsonArray.getString(i);
            serviceUUIDs.add(uuidFromString(uuidString));
        }

        return serviceUUIDs.toArray(new UUID[jsonArray.length()]);
    }

    private void onBluetoothStateChange(Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            sendBluetoothStateChange(state);
        }
    }

    private void sendBluetoothStateChange(int state) {
        if (this.stateCallback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, this.bluetoothStates.get(state));
            result.setKeepCallback(true);
            this.stateCallback.sendPluginResult(result);
        }
    }

    private void addStateListener() {
        if (this.stateReceiver == null) {
            this.stateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    onBluetoothStateChange(intent);
                }
            };
        }

        try {
            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            webView.getContext().registerReceiver(this.stateReceiver, intentFilter);
        } catch (Exception e) {
            LOG.e(TAG, "Error registering state receiver: " + e.getMessage(), e);
        }
    }

    private void removeStateListener() {
        if (this.stateReceiver != null) {
            try {
                webView.getContext().unregisterReceiver(this.stateReceiver);
            } catch (Exception e) {
                LOG.e(TAG, "Error unregistering state receiver: " + e.getMessage(), e);
            }
        }
        this.stateCallback = null;
        this.stateReceiver = null;
    }

    private void createServiceFromJSON(CallbackContext callbackContext, JSONObject serviceDescription) throws JSONException {

        UUID serviceUUID = uuidFromString(serviceDescription.getString("uuid"));

        BluetoothGattService service = new BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        JSONArray characteristicsArray = serviceDescription.getJSONArray("characteristics");
        for (int i = 0; i < characteristicsArray.length(); i++) {
            JSONObject json = characteristicsArray.getJSONObject(i);

            BluetoothGattCharacteristic characteristic = characteristicFromJSON(json);
            service.addCharacteristic(characteristic);
        }

        gattServer = bluetoothManager.openGattServer(cordova.getActivity(), bluetoothGattServerCallback);
        gattServer.addService(service);

    }

    private BluetoothGattCharacteristic characteristicFromJSON(JSONObject json) throws JSONException {

        UUID uuid = uuidFromString(json.getString("uuid"));
        int properties = json.getInt("properties");
        int permissions = json.getInt("permissions");
        JSONArray descriptorArray = json.getJSONArray("descriptors");

        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid, properties, permissions);

        if (descriptorArray.length() > 0) {
            for (int j = 0; j < descriptorArray.length(); j++) {
                BluetoothGattDescriptor descriptor = descriptorFromJSON(descriptorArray.getJSONObject(j));
                characteristic.addDescriptor(descriptor);
            }
        }

        return characteristic;
    }

    private BluetoothGattDescriptor descriptorFromJSON(JSONObject json) throws JSONException {
        UUID uuid = uuidFromString(json.getString("uuid"));
        String value = json.getString("value"); // TODO getBytes()
        int permissions = json.getInt("permissions");
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(uuid, permissions);
        descriptor.setValue(value.getBytes());
        return descriptor;
    }

    private AdvertiseSettings getAdvertiseSettings() {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        return builder.build();
    }

    private AdvertiseData getAdvertisementData(UUID serviceUUID) {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.setIncludeDeviceName(true);
        builder.addServiceUuid(new ParcelUuid(serviceUUID));

        return builder.build();
    }

    private void advertiseService(final CallbackContext callbackContext, UUID serviceUUID, String localName) {

        BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseData advertisementData = getAdvertisementData(serviceUUID);
        AdvertiseSettings advertiseSettings = getAdvertiseSettings();

        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                callbackContext.success();
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                callbackContext.error(errorCode);
            }
        };

        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertisementData, advertiseCallback);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

            if (resultCode == Activity.RESULT_OK) {
                LOG.d(TAG, "User enabled Bluetooth");
                if (enableBluetoothCallback != null) {
                    enableBluetoothCallback.success();
                }
            } else {
                LOG.d(TAG, "User did *NOT* enable Bluetooth");
                if (enableBluetoothCallback != null) {
                    enableBluetoothCallback.error("User did not enable Bluetooth");
                }
            }

            enableBluetoothCallback = null;
        }
    }

    /* @Override */
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) /* throws JSONException */ {
        for(int result:grantResults) {
            if(result == PackageManager.PERMISSION_DENIED)
            {
                LOG.d(TAG, "User *rejected* Coarse Location Access");
                this.permissionCallback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
                return;
            }
        }

        switch(requestCode) {
            case REQUEST_ACCESS_COARSE_LOCATION:
                LOG.d(TAG, "User granted Coarse Location Access");
//                findLowEnergyDevices(permissionCallback, serviceUUIDs, scanSeconds);
//                this.permissionCallback = null;
//                this.serviceUUIDs = null;
//                this.scanSeconds = -1;
//                break;
        }
    }

    private UUID uuidFromString(String uuid) {
        return UUIDHelper.uuidFromString(uuid);
    }

    BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (createServiceFromJSONCallback != null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    createServiceFromJSONCallback.success();
                } else {
                    createServiceFromJSONCallback.error(status);
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

            // TODO this should Just Work by default
//            gattServer.sendResponse(device,
//                    requestId,
//                    BluetoothGatt.GATT_SUCCESS,
//                    0,
//                    characteristic.getValue());

        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

            // TODO does this work, or do I need to look it up from gatt server and set?
            characteristic.setValue(value);

            if (responseNeeded) {
                gattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        value);
            }

            try {
                JSONObject json = new JSONObject();
                json.put("service", characteristic.getService().getUuid().toString());
                json.put("characteristic", characteristic.getUuid().toString());
                json.put("value", value);

                PluginResult result = new PluginResult(PluginResult.Status.OK, json);
                result.setKeepCallback(true);
                writeRequestCallback.sendPluginResult(result);
            } catch (JSONException e) {  // shouldn't happen
                e.printStackTrace();
                writeRequestCallback.error("Error building JSON for onCharacteristicWriteRequest");
            }

        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
        }
    };

}
