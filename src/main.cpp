/* Authors:
            Sasha Kaplan        Github: Sashakap, Alexander de Bont,
            Harold F
            Varsha Thomas       Github: VT_c0des
 * Project: Autism Treatment Center of Dallas Sensory Device
 * UTD Engineering Projects in Community Service (EPICS)
 * Semester/Year Updated: Fall 2023
 */

#include <constants_headers.h> // include statements, consts, defines

TwoWire I2CMPR = TwoWire(1);
Adafruit_MPRLS mpr;

// Function Declarations
//  BLE
void startBLESetup();

// OLED Display
void setupOLED();
void readyMessage();
void drawMonitor(float, float, float);
void drawString(String);

// Operation Modes
void motorON(int);
void motorOFF(int);
void valveON();
void valveOFF();
void standby();
void freeRun();
void cycleRun();
void eStopEnabledHandler();
void eStopDisabledHandler();
bool inflateBladder();
bool deflateBladder();
float readPressureSensor();

// Auxilliary Methods
void printStructContents();

// BLE Fields
BLEServer *pServer = NULL;
BLECharacteristic *pTxCharacteristic;

struct DataReceived
{
  uint8_t mobileIdentifier = NO_IDENTIFIER;
  bool freeRun = false;
  bool inflate = false;
  bool deflate = false;
  bool cycleRun = false;
  bool start = false;
  bool stop = false;
  float inflateTargetPressure = 0.0;
  float holdTime = 0.0;
  float deflateTargetPressure = 0.0;
};

// Pressure Regulation Data
struct DataReceived data;
char operationMode = STANDBY;
unsigned long holdStartTime = 0;
float currentBladderPressure = 0.0;

volatile bool eStopSwitchStatus = false;
volatile bool eStopInterruptEnabled = false;
bool eStopDeflateComplete = false;
bool inflateMotorIsOn = false;
bool deflateMotorIsOn = false;
bool valveIsOn = false;
bool deviceConnected = false;
bool currentDeviceConnected = false;
bool startHold = true;
bool inflateOpComplete = false;
bool deflateOpComplete = false;
bool cycleStartCondition = true;
bool freerunStartCondition = true;

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
  void onWrite(BLECharacteristic *pCharacteristic)
  {
    Serial.println("Recieving from Device...");
    std::string rxValue = pCharacteristic->getValue();
    memcpy(&data, rxValue.data(), rxValue.length());
    data.holdTime = data.holdTime * 1000; // convert hold time to ms
    data.inflateTargetPressure += INF_MARGIN;
    data.deflateTargetPressure = pressure_min - DEF_MARGIN;
    printStructContents();

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
  // GPIO Setup
  pinMode(inflateMotor, OUTPUT);
  pinMode(deflateMotor, OUTPUT);
  pinMode(valve, OUTPUT);
  pinMode(eStopSwitch, INPUT);

  digitalWrite(inflateMotor, LOW);
  digitalWrite(deflateMotor, LOW);
  digitalWrite(valve, LOW);

  // Serial Setup & OLED Setup
  Serial.begin(115200);
  setupOLED();

  // Custom I2C Pins & MPRLS Setup
  I2CMPR.begin(I2C_SDA, I2C_SCL, 400000);
  bool status = mpr.begin(0x18, &I2CMPR);

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

  pressure_min = readPressureSensor(); // determine ATM pressure
  delay(1000);

  // Begin BluetoothLE GATT Server
  startBLESetup();
}

void loop()
{
  // Device Connected Actions
  if (deviceConnected)
  {
    currentDeviceConnected = true;

    currentBladderPressure = readPressureSensor();

    //=========BLE/OLED COMMUNICATION==================================================

    // display easter egg
    if (currentBladderPressure > PRESSURE_MAX)
    {
      Heltec.display->clear();
      Heltec.display->drawXbm(0, 0, smile_width, smile_height, smile_bits);
      Heltec.display->display();
    }

    // update OLED display
    drawMonitor(currentBladderPressure, data.inflateTargetPressure, pressure_min);

    // transmit data to mobile device
    pTxCharacteristic->setValue(currentBladderPressure);
    pTxCharacteristic->notify();
    delay(10); // bluetooth stack will go into congestion if too many packets are sent

    eStopSwitchStatus = digitalRead(eStopSwitch);

    if ((eStopSwitchStatus == 0) && (eStopInterruptEnabled == 0))
    {
      eStopEnabledHandler();
    }
    else if ((eStopSwitchStatus == 0) && (eStopInterruptEnabled > 0))
    {
      eStopEnabledHandler();
    }
    else if ((eStopSwitchStatus > 0) && (eStopInterruptEnabled > 0))
    {
      eStopDisabledHandler();
    }
    else
    {
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
      }
    }
    delay(SAMPLING_PERIOD); // sample pressure sensor if device is connected
  }

  // Device Disconnected Actions
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
}

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
  // inflate bladder until pressure hits target pressure
  if (currentBladderPressure <= data.inflateTargetPressure)
  {
    // Serial.println("Inflate\nTarget: " + String(data.inflateTargetPressure) + " PSI\tCurrent: " + String(currentBladderPressure) + " PSI");
    valveOFF();
    motorON(inflateMotor);
  }
  // stop inflating and hold at target pressure
  else
  {
    valveOFF();
    motorOFF(inflateMotor);
    if (startHold)
    {
      Serial.println("Begin Hold");
      holdStartTime = millis();
      startHold = false;
      Serial.println("Start time(ms): " + String(holdStartTime) + "ms");
    }
    else
    {
      // check if holdTime has elapsed
      if ((millis() - holdStartTime) >= data.holdTime)
      {
        Serial.println("Finish time(ms): " + String(millis() - holdStartTime));
        holdStartTime = 0.0;
        startHold = true;
        Serial.println("Inflate Op Complete. Hold Time(s): " + String(holdStartTime));
        return true;
      }
      else
      {
        startHold = false;
      }
    }
  }
  return false;
}

bool deflateBladder()
{
  // deflate bladder until minimum pressure
  if (currentBladderPressure >= data.deflateTargetPressure)
  {
    Serial.println("Deflate\nTarget: " + String(data.deflateTargetPressure) + " PSI\tCurrent: " + String(currentBladderPressure) + " PSI");
    motorOFF(inflateMotor);
    valveON();
    motorON(deflateMotor);
  }
  // turn off deflate motor
  else
  {
    Serial.println("Deflate Pressure Threshold Exceeded");
    valveOFF();
    motorOFF(deflateMotor);
    return true;
  }
  return false;
}

void freeRun()
{
  // deflate bladder upon startup
  if (freerunStartCondition)
  {
    // Serial.println("Deflate Start Condition");
    deflateOpComplete = deflateBladder();
    if (deflateOpComplete)
    {
      freerunStartCondition = false;
      deflateOpComplete = false;
    }
  }
  else
  {
    if (data.inflate)
    {
      // Serial.println("Mode: FR, Inf");
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
      // Serial.println("Mode: FR, Def");
      deflateOpComplete = deflateBladder();
      if (deflateOpComplete)
      {
        data.deflate = false;
        inflateOpComplete = false;
        deflateOpComplete = false;
        freerunStartCondition = true;
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
      // Serial.println("Deflate Start Condition");
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
        // Serial.println("Mode: CR, Inf");
        inflateOpComplete = inflateBladder();
        deflateOpComplete = (inflateOpComplete) ? false : true;
      }
      // deflate if inflate completed
      if (inflateOpComplete)
      {
        // Serial.println("Mode: CR, Def");
        deflateOpComplete = deflateBladder();
        inflateOpComplete = (deflateOpComplete) ? false : true;
      }
    }
  }
  else if (data.stop)
  {
    // deflate device then go to standby
    deflateOpComplete = deflateBladder();
    if (deflateOpComplete)
    {
      data.start = false;
      data.stop = false;
      deflateOpComplete = false;
      inflateOpComplete = false;
      cycleStartCondition = true;
      operationMode = STANDBY;
    }
  }
  else
  {
    operationMode = STANDBY;
  }
}

void eStopEnabledHandler()
{
  Serial.println("Power Disconnected OR EStop Enabled");
  eStopInterruptEnabled = true;
  // deflate bladder
  if (!eStopDeflateComplete)
  {
    Serial.println("EStop - Deflating");
    eStopDeflateComplete = deflateBladder();
  }
  // remain in standby once emergency deflate is complete
  else
  {
    standby();
  }
}

void eStopDisabledHandler()
{
  eStopInterruptEnabled = false;
  eStopDeflateComplete = false;
  operationMode = STANDBY;
}

float readPressureSensor()
{
  return (mpr.readPressure() / 68.947572932);
}

//===================Auxilliary Methods===================
void printStructContents()
{
  // print each value in the struct
  Serial.println("Values from the struct:");
  Serial.println("mobile identifier: " + String(data.mobileIdentifier));
  Serial.println("free run: " + String(data.freeRun));
  Serial.println("inflate: " + String(data.inflate));
  Serial.println("deflate: " + String(data.deflate));
  Serial.println("cycle run: " + String(data.cycleRun));
  Serial.println("start: " + String(data.start));
  Serial.println("stop: " + String(data.stop));
  Serial.println("inflate target pressure (psi): " + String(data.inflateTargetPressure));
  Serial.println("deflate target pressure (psi): " + String(data.deflateTargetPressure));
  Serial.println("hold time(s): " + String(data.holdTime));
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

void drawMonitor(float pBladder, float pTarget, float pATM)
{
  Heltec.display->clear();
  Heltec.display->setTextAlignment(TEXT_ALIGN_LEFT);

  Heltec.display->drawXbm(0, 0, target_icon_width, target_icon_height, target_icon_bits);
  Heltec.display->drawString(12, 0, String(pTarget));

  Heltec.display->drawXbm(0, 15, scope_icon_width, scope_icon_height, scope_icon_bits);
  Heltec.display->drawString(12, 15, String(String(pBladder, 1)));

  Heltec.display->drawXbm(0, 30, scope_icon_width, scope_icon_height, scope_icon_bits);
  Heltec.display->drawString(12, 30, "ATM (psi) " + String(pATM));
  Heltec.display->display();
}
