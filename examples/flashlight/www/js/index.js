// Smartbotic Service
var SERVICE_UUID = 'FF10';
var SWITCH_UUID = 'FF11';
var DIMMER_UUID = 'FF12';

var app = {
    initialize: function() {
        this.bindEvents();
    },
    bindEvents: function() {
        document.addEventListener('deviceready', this.onDeviceReady, false);
    },
    onDeviceReady: function() {

        blePeripheral.onWriteRequest(app.didReceiveWriteRequest);
        blePeripheral.onBluetoothStateChange(app.onBluetoothStateChange);

       app.createServiceJSON();
   },
   createServiceJSON: function() {

       var property = blePeripheral.properties;
       var permission = blePeripheral.permissions;

       var flashlightService = {
           uuid: SERVICE_UUID,
           characteristics: [
               {
                   uuid: SWITCH_UUID,
                   properties: property.WRITE | property.READ,
                   permissions: permission.WRITEABLE | permission.READABLE,
                   descriptors: [
                       {
                           uuid: '2901',
                           value: 'Switch'
                       }
                   ]
               },
               {
                   uuid: DIMMER_UUID,
                   properties: property.WRITE | property.READ,
                   permissions: permission.WRITEABLE | permission.READABLE,
                   descriptors: [
                       {
                           uuid: '2901',
                           value: 'Dimmer'
                       }
                   ]
               }
           ]
       };

       Promise.all([
           blePeripheral.createServiceFromJSON(flashlightService),
           blePeripheral.startAdvertising(flashlightService.uuid, 'Flashlight')
       ]).then(
           function() { console.log ('Created Flashlight Service'); },
           app.onError
       );
   },
   didReceiveWriteRequest: function(request) {
       console.log(request);
       if (request.characteristic === SWITCH_UUID) {
           var data = new Uint8Array(request.value);
           if (data[0] === 0) {
               window.plugins.flashlight.switchOff();
           } else {
               window.plugins.flashlight.switchOn();
           }
       }
   },
   onBluetoothStateChange: function(state) {
       console.log('Bluetooth State is', state);
       outputDiv.innerHTML += 'Bluetooth  is ' +  state + '<br/>';
   }
};

app.initialize();
