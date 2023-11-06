/* Authors:
            Sasha Kaplan        Github: Sashakap, Alexander de Bont,
            Harold F
            Varsha Thomas       Github: VT_c0des
            Rohan Thomas        Github: tr0han
 * Project: Autism Treatment Center of Dallas Sensory Device
 * UTD Engineering Projects in Community Service (EPICS)
 * Semester/Year Updated: Fall 2023
 */

#include <constants.h>

TwoWire I2CMPR = TwoWire(1);
Adafruit_MPRLS mpr;

// BLE Fields
BLEServer *pServer = NULL;
BLECharacteristic *pTxCharacteristic;

// Function Prototypes
float getPressure();
void standby();
float byteArrayToFloat(byte, byte, byte, byte);
void startBLESetup();
void setupOLED();
void readyMessage();
void drawMonitor(float, float);
void drawString(String);

// Reading Messages from Mobile App
typedef unsigned char uChar;
float pressureTarget = -1;

// Pressure Regulation Data
double pressureAmbiant;
bool inflateMotorAlreadyOn = false;
//***other bool for d motor
bool valveAlreadyOn = false;
bool deviceConnected = false;
bool oldDeviceConnected = false;

// Class that checks for server callbacks
class MyServerCallbacks : public BLEServerCallbacks
{
  void onConnect(BLEServer *pServer)
  {
    Serial.println("Device Connected");
    deviceConnected = true;
  }

  void onDisconnect(BLEServer *pServer)
  {
    Serial.println("Device Disconnected");
    deviceConnected = false;
    standby();
  }
};

// After a notification is received that the characterstic value changed (i.e., data was sent from the phone)
class MyCallbacks : public BLECharacteristicCallbacks
{
  // recieve data from Android Device
  void onWrite(BLECharacteristic *pCharacteristic)
  {
    Serial.println("Recieving from Device");

    std::string rxValue = pCharacteristic->getValue();
    Serial.print(F("rxValue -> "));
    Serial.println(rxValue.c_str());

    // Create Byte Array from String
    // A float is a 32-bit datatype (4 bytes)
    uChar rxBytes[rxValue.length()];
    memcpy(rxBytes, rxValue.data(), rxValue.length());

    Serial.print("rxLength: " + String(rxValue.length())); //
    Serial.println();                                      //
    int length = sizeof(rxBytes) / sizeof(rxBytes[0]);     //  Debugging
    for (int i = 0; i < length; i++)
    { //
      Serial.print(rxBytes[i], HEX);
      Serial.print("  ");
    } //

    // Convert byte array to floating point number
    // This copies the values of the bytes of rxBytes directly to the memory location pointed to by pressureTarget
    // memcpy(&pressureTarget, &rxBytes, sizeof(pressureTarget));
    pressureTarget = byteArrayToFloat(rxBytes[0], rxBytes[1], rxBytes[2], rxBytes[3]);
    Serial.println("\n*********");
    Serial.print("Received Value: ");
    Serial.print(String(pressureTarget));
    Serial.println("\n*********");

    if (pressureTarget < 0)
    {
      standby();
    }
  }

  // recieve data from iOS device
};

// Main method
void setup()
{
  // Serial Setup & OLED Setup
  Serial.begin(115200);
  setupOLED();

  // Custom I2C Pins & MPRLS Setup
  I2CMPR.begin(I2C_SDA, I2C_SCL, 400000);
  bool status = mpr.begin(0x18, &I2CMPR);

  // GPIO Setup
  pinMode(inflateMotor, OUTPUT);
  pinMode(deflateMotor, OUTPUT);
  pinMode(valve, OUTPUT);

  // Check That MPRLS Sensor is Connected
  if (!status)
  {
    drawString("MPRLS Undetected, Cannot Proceed");
    while (!status)
    {
      delay(10);
    }
  }
  else
  {
    drawString("MPRLS Detected!");
    delay(1000);
  }

  // Begin BluetoothLE GATT Server
  startBLESetup();
}

void loop()
{
  // Idle
  if (deviceConnected)
  {
    oldDeviceConnected = true;

    // Send Pressure Data to Phone
    float pressureBladder = getPressure(); // Pressure in PSI
    pTxCharacteristic->setValue(pressureBladder);
    pTxCharacteristic->notify();
    delay(10); // bluetooth stack will go into congestion, if too many packets are sent

    // OLED Display
    drawMonitor(pressureBladder, pressureTarget);

    //-------------------------------------------------------------

    if (pressureTarget >= 0)
    {
      String pressureMessage = "";

      // Over Target
      if ((pressureBladder > pressureTarget) && !inflateMotorAlreadyOn)
      {
        inflateMotorAlreadyOn = true;
        valveAlreadyOn = false;
        pressureMessage = "PSI: " + String(pressureBladder);
        // Serial.println(pressureMessage + "Over Target");
        // Will continue a cycle of releasing air as long as the target pressure is lower than the pressure in the bladder
        // Activate the release valve
        digitalWrite(valve, HIGH);
        digitalWrite(inflateMotor, LOW);
      }
      // Under Target
      else if ((pressureBladder < pressureTarget) && !valveAlreadyOn)
      {
        valveAlreadyOn = true;
        inflateMotorAlreadyOn = false;
        pressureMessage = "PSI: " + String(pressureBladder);
        // Serial.println(pressureMessage + "Under Target");
        // Will continue a cycle of pumping air as long as the target pressure is higher than the pressure in the bladder
        digitalWrite(inflateMotor, HIGH);
        digitalWrite(valve, LOW);
      }
      // On Target
      else
      {
        // Unlikely edge case that pressure is exactly where desired
        // Serial.println("Target Aquired");
      }

      if (pressureBladder > 15.6)
      {
        Heltec.display->clear();
        Heltec.display->drawXbm(0, 0, smile_width, smile_height, smile_bits);
        Heltec.display->display();
      }
    }
    delay(DELAY_TIME);
    //-------------------------------------------------------------
  }

  // disconnecting
  if (!deviceConnected && oldDeviceConnected)
  {
    Serial.println("Device Disconnected");
    delay(500);                  // give the bluetooth stack the chance to get things ready
    pServer->startAdvertising(); // restart advertising
    Serial.println("Advertising Restarted");
    deviceConnected = false;
    oldDeviceConnected = false;
    readyMessage();
  }
}

//===================Auxilliary Methods===================
// Fills up the bladder with 0 to PSI_LIMIT
float getPressure()
{
  return mpr.readPressure() / 68.947572932;
}

void standby()
{
  digitalWrite(inflateMotor, LOW);
  //***digitalWrite(delfateMotor, LOW);
  digitalWrite(valve, LOW);
  inflateMotorAlreadyOn = false;
  //***other bool
  valveAlreadyOn = false;
}

float byteArrayToFloat(uChar b0, uChar b1, uChar b2, uChar b3)
{
  // Intepret the 4 bytes as floating point in Big Endian
  float result;
  uChar byte_array[] = {b3, b2, b1, b0};
  std::copy(reinterpret_cast<const char *>(&byte_array[0]), reinterpret_cast<const char *>(&byte_array[3]), reinterpret_cast<char *>(&result));

  return result;
}

//===================Setup Methods===================
void startBLESetup()
{
  // Create the BLE Device
  BLEDevice::init(DEVICE_NAME);

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristics

  // TX Characteristic (Send Data to Mobile Device)
  pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
  pTxCharacteristic->addDescriptor(new BLE2902());

  // RX Characteristic (Receive Data from Mobile Device)
  BLECharacteristic *pRxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE);
  pRxCharacteristic->setCallbacks(new MyCallbacks());

  // Start the service
  pService->start();

  // Start advertising
  pServer->getAdvertising()->addServiceUUID(pService->getUUID());
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");
  readyMessage();
}

//=================================OLED Drawing======================================

void setupOLED()
{
  // OLED Splash Screen
  Heltec.begin(true /*DisplayEnable Enable*/, false /*LoRa Disable*/, true /*Serial Enable*/);
  Heltec.display->setFont(ArialMT_Plain_10);
  Heltec.display->setTextAlignment(TEXT_ALIGN_CENTER);
  Heltec.display->clear();
  Heltec.display->drawXbm(0, 0, EPICS_Logo_width, EPICS_Logo_height, EPICS_Logo_bits);
  Heltec.display->display();
  delay(3000);
  Heltec.display->clear();
  Heltec.display->drawXbm(0, 11, ATC_Logo_width, ATC_Logo_height, ATC_Logo_bits);
  Heltec.display->display();
  delay(3000);
}

void readyMessage()
{
  Heltec.display->clear();
  Heltec.display->setTextAlignment(TEXT_ALIGN_CENTER);
  String readyMessage = "GATT Server Started as '" + String(DEVICE_NAME) + "'";
  Heltec.display->drawStringMaxWidth(64, 10, 128, readyMessage);
  Heltec.display->display();
}

void drawString(String msg)
{
  Heltec.display->clear();
  Heltec.display->drawStringMaxWidth(64, 10, 128, msg);
  Heltec.display->display();
}

void drawMonitor(float pBladder, float pTarget)
{
  Heltec.display->clear();
  Heltec.display->setTextAlignment(TEXT_ALIGN_LEFT);
  Heltec.display->drawXbm(0, 0, target_icon_width, target_icon_height, target_icon_bits);
  Heltec.display->drawString(12, 0, String(pTarget));
  Heltec.display->drawXbm(0, 15, scope_icon_width, scope_icon_height, scope_icon_bits);
  Heltec.display->drawString(12, 15, String(String(pBladder, 1)));
  Heltec.display->display();
}
