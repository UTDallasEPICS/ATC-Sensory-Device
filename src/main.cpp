/* Authors:
            Sasha Kaplan        Github: Sashakap, Alexander de Bont,
            Harold F
            Varsha Thomas       Github: VT_c0des
            Rohan Thomas        Github: tr0han
 * Project: Autism Treatment Center of Dallas Sensory Device
 * UTD Engineering Projects in Community Service (EPICS)
 * Semester/Year Updated: Fall 2023
 */

#include <constants_includes.h> // includes CONST, defines, headers

TwoWire I2CMPR = TwoWire(1);
Adafruit_MPRLS mpr;

// Function Declarations
//  BLE
void startBLESetup();

// OLED Display
void setupOLED();
void readyMessage();
void drawMonitor(float, float);
void drawString(String);

// Operation Modes
void motorON(int);
void motorOFF(int);
void valveON();
void valveOFF();
void standby();
void freeRun();
void cycleRun();
bool inflateBladder();
bool deflateBladder();
void emergencySwitchTriggered();
float readPressureSensor();

// Auxilliary Methods
float byteArrayToFloat(byte, byte, byte, byte);
void printStructContents();

// BLE Fields
BLEServer *pServer = NULL;
BLECharacteristic *pTxCharacteristic;

// Reading Messages from Android App
typedef unsigned char uChar;
float pressureTarget = -1;

// data for message from iOS App
struct DataReceived
{
  uint8_t mobileIdentifier;
  bool freeRun;
  bool inflate;
  bool deflate;
  bool cycleRun;
  bool start;
  bool stop;
  float targetPressure;
  float holdTime;

  // initialize constructor with defaults upon startup
  DataReceived()
  {
    mobileIdentifier = NO_IDENTIFIER;
    freeRun = false;
    inflate = false;
    deflate = false;
    cycleRun = false;
    start = false;
    stop = false;
    targetPressure = PRESSURE_MIN;
    holdTime = 0.0;
  } // end of default constructor

  // initialize struct with data recieved from ble transmission
  DataReceived(int mI, bool fR, bool inf,
               bool def, bool cR, bool stt,
               bool stp, float pVal, float hT)
  {
    mobileIdentifier = mI;
    freeRun = fR;
    inflate = inf;
    deflate = def;
    cycleRun = cR;
    start = stt;
    stop = stp;
    targetPressure = pVal;
    holdTime = (hT * 1000); // convert to milliseconds
  }                         // end of constructor
};

// Pressure Regulation Data
struct DataReceived data;
unsigned long startTime = 0.0;
float currentBladderPressure = 0.0;
bool eStopEnabled = false;
bool inflateMotorIsOn = false;
bool deflateMotorIsOn = false;
bool valveIsOn = false;
bool deviceConnected = false;
bool currentDeviceConnected = false;
bool inflateOpComplete = false;
bool deflateOpComplete = false;
bool cycleStartCondition = true;
bool runStartCondition = true;
char operationMode = STANDBY;
String pressureMessage = "";

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
  // handle a case where device is neither ios or android. throw an error?
  void onWrite(BLECharacteristic *pCharacteristic)
  {
    if (data.mobileIdentifier == NO_IDENTIFIER)
    {
      Serial.println("DANGER: UNAUTHORIZED DEVICE IS ATTEMPTING TO SEND DATA");
    }
    else
    {
      Serial.println("Recieving from Device");
      memcpy(&data, pCharacteristic, sizeof(pCharacteristic));
      printStructContents();
    }

    // // recieve data from android device
    // std::string rxValue = pCharacteristic->getValue();
    // Serial.print(F("rxValue -> "));
    // Serial.println(rxValue.c_str());

    // // Create Byte Array from String
    // // A float is a 32-bit datatype (4 bytes)
    // uChar rxBytes[rxValue.length()];
    // memcpy(rxBytes, rxValue.data(), rxValue.length());

    // Serial.print("rxLength: " + String(rxValue.length())); //
    // Serial.println();                                      //
    // int length = sizeof(rxBytes) / sizeof(rxBytes[0]);     //  Debugging
    // for (int i = 0; i < length; i++)
    // { //
    //   Serial.print(rxBytes[i], HEX);
    //   Serial.print("  ");
    // } //

    // // Convert byte array to floating point number
    // // This copies the values of the bytes of rxBytes directly to the memory location pointed to by pressureTarget
    // // memcpy(&pressureTarget, &rxBytes, sizeof(pressureTarget));
    // pressureTarget = byteArrayToFloat(rxBytes[0], rxBytes[1], rxBytes[2], rxBytes[3]);
    // Serial.println("\n*********");
    // Serial.print("Received Value: ");
    // Serial.print(String(pressureTarget));
    // Serial.println("\n*********");

    // UPDATE OPERATION MODE
    if (data.freeRun)
    {
      operationMode = FREERUN;
    }
    else if (data.cycleRun)
    {
      operationMode = CYCLERUN;
    }
    else
    {
      operationMode = STANDBY;
    }
  }
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
  pinMode(eStopSwitch, INPUT);
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

  // create interrupt
  attachInterrupt(eStopSwitch, emergencySwitchTriggered, FALLING);
}

void loop()
{
  if (deviceConnected)
  {
    currentDeviceConnected = true;

    //-------------------------------------------------------------

    // get pressure in PSI
    currentBladderPressure = readPressureSensor();

    // OLED Display
    drawMonitor(currentBladderPressure, pressureTarget);

    // Transmit pressure data to mobile device
    pTxCharacteristic->setValue(currentBladderPressure);
    pTxCharacteristic->notify();
    delay(10); // bluetooth stack will go into congestion, if too many packets are sent

    if (currentBladderPressure > PRESSURE_MAX)
    {
      Heltec.display->clear();
      Heltec.display->drawXbm(0, 0, smile_width, smile_height, smile_bits);
      Heltec.display->display();
    }

    //-------------------------------------------------------------

    // check mode of operation and call the appropriate function
    if (eStopEnabled)
    {
      operationMode = STANDBY;
    }
    else
    {
      eStopEnabled = false;
      switch (operationMode)
      {
      case FREERUN:
        freeRun();
        break;
      case CYCLERUN:
        cycleRun();
        break;
      case STANDBY:
        standby();
        break;
      default:
        standby();
        break;
      } // end of switch-case
    }
  }
  delay(DELAY_TIME);

  // disconnecting
  if (!deviceConnected && currentDeviceConnected)
  {
    Serial.println("Device Disconnected");
    delay(500);                  // give the bluetooth stack the chance to get things ready
    pServer->startAdvertising(); // restart advertising
    Serial.println("Advertising Restarted");
    deviceConnected = false;
    currentDeviceConnected = false;
    readyMessage();
  }
} // end of loop

//===================Operation Mode Methods===============
void motorON(int motorPin)
{
  digitalWrite(motorPin, HIGH);
  // update both motor booleans.
  inflateMotorIsOn = (motorPin == inflateMotor) ? true : false;
  deflateMotorIsOn = (motorPin == deflateMotor) ? true : false;

  // switch valve if deflate motor is activated, else keep it in the (default) inflate position
  (motorPin == deflateMotor) ? valveON() : valveOFF();
}

void motorOFF(int motorPin)
{
  digitalWrite(motorPin, LOW);
  // update motor booleans
  inflateMotorIsOn = (motorPin == inflateMotor) ? false : true;
  deflateMotorIsOn = (motorPin == deflateMotor) ? false : true;
}

// default valve position
void valveOFF()
{
  digitalWrite(valve, LOW);
  valveIsOn = false;
}

void valveON()
{
  digitalWrite(valve, HIGH);
  valveIsOn = true;
}

// turns off both motors, sets valve to default position
void standby()
{
  motorOFF(inflateMotor);
  motorOFF(deflateMotor);
  valveOFF();
}

bool inflateBladder()
{
  pressureMessage = "PSI: " + String(currentBladderPressure);
  Serial.print("Current Bladder Pressure: ");
  Serial.println(pressureMessage);
  // inflate bladder until pressure hits target pressure
  if (currentBladderPressure <= data.targetPressure)
  {
    valveOFF();
    motorON(inflateMotor);
  }
  // stop inflating and hold at target pressure
  else if (currentBladderPressure > data.targetPressure)
  {
    if (startTime == 0)
    {
      startTime = millis();
    }
    motorOFF(inflateMotor);
  }
  // check if holdTime has elapsed
  if ((millis() - startTime) >= data.holdTime)
  {
    startTime = 0.0;
    return true;
  }

  return false;
}

bool deflateBladder()
{
  pressureMessage = "PSI: " + String(currentBladderPressure);
  Serial.print("Current Bladder Pressure: ");
  Serial.println(pressureMessage);

  // deflate bladder until minimum pressure
  if (currentBladderPressure >= PRESSURE_MIN)
  {
    valveON();
    motorON(deflateMotor);
  }
  // turn off deflate motor
  else if (currentBladderPressure < PRESSURE_MIN)
  {
    valveOFF();
    motorOFF(deflateMotor);
    return true;
  }
  return false;
}

void freeRun()
{
  // deflate bladder upon startup
  if (runStartCondition)
  {
    deflateOpComplete = deflateBladder();
    if (deflateOpComplete)
    {
      runStartCondition = false;
    }
  }
  else
  {
    if (data.inflate)
    {
      inflateOpComplete = inflateBladder();
      if (inflateOpComplete)
      {
        // trigger deflate
        data.deflate = true;
        data.inflate = false;
      }
    }
    else if (data.deflate)
    {
      // deflate bladder then go to standby
      deflateOpComplete = deflateBladder();
      if (deflateOpComplete)
      {
        data.deflate = false;
        runStartCondition = true;
        operationMode = STANDBY;
      }
    }
    else
    {
      operationMode = STANDBY;
    }
  }
}

void cycleRun()
{
  if (data.start)
  {
    // deflate bladder upon startup.
    if (cycleStartCondition)
    {
      deflateOpComplete = deflateBladder();
      if (deflateOpComplete)
      {
        cycleStartCondition = false;
      }
    }
    else
    {
      // inflate if deflate completed
      if (deflateOpComplete)
      {
        inflateOpComplete = inflateBladder();
      }
      // deflate if inflate completed
      else if (inflateOpComplete)
      {
        deflateOpComplete = deflateBladder();
      }
    }
  }
  else if (data.stop)
  {
    cycleStartCondition = true;
    // deflate device then go to standby
    deflateOpComplete = deflateBladder();
    if (deflateOpComplete)
    {
      operationMode = STANDBY;
    }
  }
  else
  {
    operationMode = STANDBY;
  }
}

void emergencySwitchTriggered()
{
  // deflate motor and enter standby
  do
  {
    deflateOpComplete = deflateBladder();
  } while (!deflateOpComplete);
  eStopEnabled = true;
}

float readPressureSensor()
{
  return (mpr.readPressure() / 68.947572932);
}

//===================Auxilliary Methods===================
float byteArrayToFloat(uChar b0, uChar b1, uChar b2, uChar b3)
{
  // Intepret the 4 bytes as floating point in Big Endian
  float result;
  uChar byte_array[] = {b3, b2, b1, b0};
  std::copy(reinterpret_cast<const char *>(&byte_array[0]), reinterpret_cast<const char *>(&byte_array[3]), reinterpret_cast<char *>(&result));

  return result;
}

void printStructContents()
{
  // print each value in the struct
  Serial.print("Values from the struct:\n");
  Serial.print("mobile identifier: ");
  Serial.println(data.mobileIdentifier);
  Serial.print("free run flag: ");
  Serial.println(data.freeRun);
  Serial.print("inflate flag: ");
  Serial.println(data.inflate);
  Serial.print("deflate flag: ");
  Serial.println(data.deflate);
  Serial.print("cycle run flag: ");
  Serial.println(data.cycleRun);
  Serial.print("start flag: ");
  Serial.println(data.start);
  Serial.print("stop flag: ");
  Serial.println(data.stop);
  Serial.print("target pressure (psi): ");
  Serial.println(data.targetPressure);
  Serial.print("inflate hold time (ms): ");
  Serial.println(data.holdTime / 1000);
}

//===================BLE Methods===================
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
