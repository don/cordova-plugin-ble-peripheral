//
//  BLEPeripheralPlugin.h
//  BLE Peripheral Cordova Plugin
//
//  (c) 2106 Don Coleman
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

#ifndef BLEPeripheralPlugin_h
#define BLEPeripheralPlugin_h

#import <Cordova/CDV.h>
#import <CoreBluetooth/CoreBluetooth.h>

@interface BLEPeripheralPlugin : CDVPlugin <CBPeripheralManagerDelegate> {
    NSString* bluetoothStateChangedCallback;
    NSString* characteristicValueChangedCallback;
    NSString* descriptorValueChangedCallback;
    NSMutableDictionary* services;
    NSString* publishServiceCallbackId; // TODO need to handle multiple
    NSString* createServiceFromJSONCallbackId;
    NSString* startAdvertisingCallbackId;
}

@property (strong, nonatomic) CBPeripheralManager *manager;

- (void)createService:(CDVInvokedUrlCommand *)command;
- (void)createServiceFromJSON:(CDVInvokedUrlCommand *)command;
- (void)addCharacteristic:(CDVInvokedUrlCommand *)command;
- (void)publishService:(CDVInvokedUrlCommand *)command;
- (void)setCharacteristicValue:(CDVInvokedUrlCommand *)command;

- (void)startAdvertising:(CDVInvokedUrlCommand *)command;

- (void)setCharacteristicValueChangedListener:(CDVInvokedUrlCommand *)command;
- (void)setDescriptorValueChangedListener:(CDVInvokedUrlCommand *)command;
- (void)setBluetoothStateChangedListener:(CDVInvokedUrlCommand *)command;

@end

#endif
