// (c) 2016 Don Coleman
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

/* global cordova, module, console, document, Uint8Array, atob, Promise, ArrayBuffer*/
"use strict";

// Util functions for translating nested array buffers going across the Cordova bridge

var stringToArrayBuffer = function(str) {
    var ret = new Uint8Array(str.length);
    for (var i = 0; i < str.length; i++) {
        ret[i] = str.charCodeAt(i);
    }
    // TODO would it be better to return Uint8Array?
    return ret.buffer;
};

var base64ToArrayBuffer = function(b64) {
    return stringToArrayBuffer(atob(b64));
};

function massageMessageNativeToJs(message) {
    if (message.CDVType == 'ArrayBuffer') {
        message = base64ToArrayBuffer(message.data);
    }
    return message;
}

// Cordova 3.6 doesn't unwrap ArrayBuffers in nested data structures
// https://github.com/apache/cordova-js/blob/94291706945c42fd47fa632ed30f5eb811080e95/src/ios/exec.js#L107-L122
function convertToNativeJS(object) {
    Object.keys(object).forEach(function (key) {
        var value = object[key];
        object[key] = massageMessageNativeToJs(value);
        if (typeof(value) === 'object') {
            convertToNativeJS(value);
        }
    });
}

// end Util functions

var onWriteRequestCallback;
var onBluetoothStateChangeCallback;

function registerWriteRequestCallback() {

    var didReceiveWriteRequest = function(json) {
      console.log('didReceiveWriteRequest');
      console.log(json);
      convertToNativeJS(json);

      if (onWriteRequestCallback && typeof onWriteRequestCallback === 'function') {
        onWriteRequestCallback(json);
      }
    };

    var failure = function() {
        // this should never happen
        console.log("Failed to add setCharacteristicValueChangedListener");
    };

    cordova.exec(didReceiveWriteRequest, failure, 'BLEPeripheral', 'setCharacteristicValueChangedListener', []);
}
registerWriteRequestCallback();

function registerBluetoothStateChangeCallback() {

    var bluetoothStateChanged = function(state) {
      console.log('bluetoothStateChanged', state);

      if (onBluetoothStateChangeCallback && typeof onBluetoothStateChangeCallback === 'function') {
        onBluetoothStateChangeCallback(state);
      }
    };

    var failure = function() {
        // this should never happen
        console.log("Failed to add bluetoothStateChangedListener");
    };

    cordova.exec(bluetoothStateChanged, failure, 'BLEPeripheral', 'setBluetoothStateChangedListener', []);
}
registerBluetoothStateChangeCallback();

/* 
Characteristic premissions are not consistent across platforms. This will need to be reconciled.
Maybe permissions should be optional and default to read/write based on the properties.

// iOS permissions CBCharacteristic.h
    CBAttributePermissionsReadable					= 0x01,
    CBAttributePermissionsWriteable					= 0x02,
    CBAttributePermissionsReadEncryptionRequired	= 0x04,
    CBAttributePermissionsWriteEncryptionRequired	= 0x08

// Android permissions BluetoothGattCharacteristic.java

    public static final int PERMISSION_READ = 0x01;
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 0x04;
    public static final int PERMISSION_WRITE = 0x10;
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 0x40;
    public static final int PERMISSION_WRITE_SIGNED = 0x80;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 0x100;
*/

module.exports = {

    properties : {
        READ: 0x02,
        WRITE: 0x08,
        WRITE_NO_RESPONSE: 0x04,
        NOTIFY: 0x10,
        INDICATE: 0x20
    },

    permissions: {
        READABLE: 0x01,
        WRITEABLE: cordova.platformId === 'ios' ? 0x02 : 0x10,
        READ_ENCRYPTION_REQUIRED: cordova.platformId === 'ios' ? 0x04 : 0x02,
        WRITE_ENCRYPTION_REQUIRED: cordova.platformId === 'ios' ? 0x08: 0x20
    },

    createService: function(uuid) {

        return new Promise(function(resolve, reject) {
             cordova.exec(resolve, reject, 'BLEPeripheral', 'createService', [uuid]);
        });
    },

    createServiceFromJSON: function(json) {

        return new Promise(function(resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'createServiceFromJSON', [json]);
        });

    },

    addCharacteristic: function(service, characteristic, properties, permissions) {

        return new Promise(function(resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'addCharacteristic', [service, characteristic, properties, permissions]);
        });

    },

    addService: function(service) {

        return new Promise(function(resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'addService', [service]);
        });

    },

    publishService: function(uuid) {

        return new Promise(function(resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'publishService', [uuid]);
        });

    },

    // Future versions should should allow one or multiple services, name should be optional
    startAdvertising: function(service, localName) {

        return new Promise(function(resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'startAdvertising', [service, localName]);
        });

    },

    // setting the value automatically notifies subscribers
    setCharacteristicValue: function(service, characteristic, value) {

        return new Promise(function(resolve, reject) {
            if (value.constructor !== ArrayBuffer) {
                // TODO try calling value.buffer before rejecting
                reject('value must be an ArrayBuffer');
            }
            cordova.exec(resolve, reject, 'BLEPeripheral', 'setCharacteristicValue', [service, characteristic, value]);
        });

    },

    // setDescriptorValue: function(service, characteristic, descriptor, value) {
    // 
    //     return new Promise(function(resolve, reject) {
    //         if (value.constructor !== ArrayBuffer) {
    //             reject('value must be an ArrayBuffer');
    //         }
    //         cordova.exec(resolve, reject, 'BLEPeripheral', 'setDescriptorValue', [service, characteristic, descriptor, value]);
    //     });
    // 
    // },

    // setDescriptorValueChangedListener: function(success) {
    //     var failure = function() {
    //         // this should never happen
    //         console.log("Failed to add setDescriptorValueChangedListener");
    //     };
    //
    //     cordova.exec(success, failure, 'BLEPeripheral', 'setDescriptorValueChangedListener', []);
    // },

    // callback gets called with JSON object whenever a central
    // updates a characteristic value. For version 1.0 this just
    // informs the app what happened, it can't be rejected
    // {
    //   service: '1234',
    //   characteristic: '5678',
    //   value: someArrayBuffer
    // }
    onWriteRequest: function(callback) {
        onWriteRequestCallback = callback;
    },

    onBluetoothStateChange: function(callback) {
        onBluetoothStateChangeCallback = callback;
    }

};
