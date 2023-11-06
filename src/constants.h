#include "headerIncludes.h"

// Custom I2C Pins
#define I2C_SCL 42
#define I2C_SDA 41

// MPRLS Setup
#define RESET_PIN -1 // set to any GPIO pin # to hard-reset on begin()
#define EOC_PIN -1   // set to any GPIO pin to read end-of-conversion by pin

// GPIO Pins
#define inflateMotor 19 // inflate Motor GPIO Pin
#define deflateMotor 20 // deflate Motor GPIO Pin
#define valve 34        // Valve GPIO Pin

// Define BLE Service
#define SERVICE_UUID "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"           // UART service UUID
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E" // For Receiving Values from Phone
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" // For Transmitting Values to Phone

#define DEVICE_NAME "ATC_3"

const int DELAY_TIME = 250; // Periodically check pressure
const int PRESSURE_MAX = 15.7;
const int PRESSURE_MIN = 14.7; // atmospheric pressure is 14.7
