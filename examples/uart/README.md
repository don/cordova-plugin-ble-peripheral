# UART Example

This app implements the [Nordic UART BLE Service](https://learn.adafruit.com/introducing-the-adafruit-bluefruit-spi-breakout/uart-service) using Apache Cordova and cordova-plugin-ble-peripheral. UART isn't the best use of Bluetooth Low Energy services, but it makes a good example since it demonstrates read, write, and notify.

# iOS

    cordova platform add ios
    cordova run ios --device

# Android

    cordova platform add android
    cordova run android --device

You can use a second phone with the Adafruit Bluefruit LE Connect app to connect to this service.

 * [Adafruit Bluefruit LE Connect for iOS](https://itunes.apple.com/WebObjects/MZStore.woa/wa/viewSoftware?id=830125974&mt=8)
 * [Adafruit Bluefruit LE Connect for Android](https://play.google.com/store/apps/details?id=com.adafruit.bluefruit.le.connect&hl=en)
