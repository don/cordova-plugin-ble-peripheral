# Bluetooth Low Energy (BLE) Peripheral Plugin for Apache Cordova

A Cordova plugin for implementing BLE (Bluetooth Low Energy) peripherals.

Need a BLE central module? See [cordova-plugin-ble-central](https://github.com/don/cordova-plugin-ble-central).

## Supported Platforms

* iOS
* Android

## Usage

### Callbacks

Register callbacks to receive notifications from the plugin

    blePeripheral.onWriteRequest(app.didReceiveWriteRequest);
    blePeripheral.onBluetoothStateChange(app.onBluetoothStateChange);

### Defining services with JSON

Define your Bluetooth Service using JSON

    var uartService = {
        uuid: SERVICE_UUID,
        characteristics: [
            {
                uuid: TX_UUID,
                properties: property.WRITE,
                permissions: permission.WRITEABLE,
                descriptors: [
                    {
                        uuid: '2901',
                        value: 'Transmit'
                    }
                ]
            },
            {
                uuid: RX_UUID,
                properties: property.READ | property.NOTIFY,
                permissions: permission.READABLE,
                descriptors: [
                    {
                        uuid: '2901',
                        value: 'Receive'
                    }
                ]
            }
        ]
    };

Create the service and start advertising

    Promise.all([
        blePeripheral.createServiceFromJSON(uartService),
        blePeripheral.startAdvertising(uartService.uuid, 'UART')
    ]).then(
        function() { console.log ('Created UART Service'); },
        app.onError
    );

### Defining services programatically

Instead of using JSON, you can create services programtically. Note that for 1.0 descriptors are only supported with the JSON format.

    Promise.all([
        blePeripheral.createService(SERVICE_UUID),
        blePeripheral.addCharacteristic(SERVICE_UUID, TX_UUID, property.WRITE, permission.WRITEABLE),
        blePeripheral.addCharacteristic(SERVICE_UUID, RX_UUID, property.READ | property.NOTIFY, permission.READABLE),
        blePeripheral.publishService(SERVICE_UUID),
        blePeripheral.startAdvertising(SERVICE_UUID, 'UART')
    ]).then(
        function() { console.log ('Created UART Service'); },
        app.onError
    );

### Examples

See the [examples](https://github.com/don/cordova-plugin-ble-peripheral/tree/master/examples) for more ideas on how this plugin can be used.

# Installing

### Cordova

    $ cordova plugin add cordova-plugin-ble-peripheral

### PhoneGap

    $ phonegap plugin add cordova-plugin-ble-peripheral

### PhoneGap Build

Edit config.xml to install the plugin for [PhoneGap Build](http://build.phonegap.com).

    <gap:plugin name="cordova-plugin-ble-peripheral" source="npm" />
    
# License

Apache 2.0

# Feedback

Try the code. If you find an problem or missing feature please create a github issue. When you're submitting an issue please include a sample project that recreates the problem.