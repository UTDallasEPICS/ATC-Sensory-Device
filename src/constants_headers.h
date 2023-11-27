#include "headerFiles.h"

#define DEVICE_NAME "ATC_3"

// Custom I2C Pins
#define I2C_SCL 42
#define I2C_SDA 41

// MPRLS Setup
#define RESET_PIN -1 // set to any GPIO pin # to hard-reset on begin()
#define EOC_PIN -1   // set to any GPIO pin to read end-of-conversion by pin

// GPIO Pins
const int inflateMotor = 19; // inflate Motor GPIO Pin
const int deflateMotor = 20; // deflate Motor GPIO Pin
const int valve = 34;        // Valve GPIO Pin
const int eStopSwitch = 1;   // E-Stop GPIO Pin

// Define BLE Service
#define SERVICE_UUID "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"           // UART service UUID
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E" // For Receiving Values from Phone
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" // For Transmitting Values to Phone

// BLE Communication Mobile Identifiers
const byte NO_IDENTIFIER = 0;
const byte IOS = 1;
const byte ANDROID = 2;

// Pressure Targets
const int SAMPLING_PERIOD = 250; // for pressure sensor pressure
const float PRESSURE_MAX = 15.7;
float pressure_min = 14.4; // update value upon device startup or reset

// Operation Modes
const char STANDBY = 's';
const char FREERUN = 'f';
const char CYCLERUN = 'c';
