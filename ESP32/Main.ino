//Authors: Sasha Kaplan Github: Sashakap, Alexander de Bont, 
//Project: Autism Treatment Center of Dallas Sensory Device
//Engineering Projects in Community Service
// Arduino library - Version: Latest

#include <Arduino.h>
#include <string.h>
#include <Wire.h>               // Wire Library - Version: Latest
#include <Adafruit_MPRLS.h>     // Adafruit MPRLS Library - Version: Latest
#include <heltec.h>             // Heltec ESP32 Dev-Boards - Version: Latest
#include <U8x8lib.h>            // UX8 Library for writing to display

//#include <string.h>
//using namespace std;


//Startup instructions
#define RESET_PIN  -1  // set to any GPIO pin # to hard-reset on begin()
#define EOC_PIN    -1  // set to any GPIO pin to read end-of-conversion by pin

Adafruit_MPRLS mpr = Adafruit_MPRLS(RESET_PIN, EOC_PIN);
U8X8_SSD1306_128X64_NONAME_SW_I2C u8x8(/* clock=*/ 15, /* data=*/ 4, /* reset=*/ 16);

int motor = 27; //Motor GPIO Address
int valve = 2;  //Valve GPIO Address
double pressureAmbiant;

//Main method
void setup() {
  pinMode(motor, OUTPUT); 
  pinMode(valve, OUTPUT);

  Serial.begin(90000);
  delay(1000);

  Serial.begin(9600);
  Serial.println();

  //Display that the program is starting
  u8x8.begin();
  u8x8.setFont(u8x8_font_torussansbold8_r);
  u8x8.drawString(6,1, "ATC");
  u8x8.drawString(2, 3, "Setting up");
  u8x8.clearDisplay();

  if (!mpr.begin()) {
    u8x8.drawString(1,2, "Failed to communicate with MPRLS sensor, check wiring?");
    while (!mpr.begin()) {
      delay(10);
    }
  }

  u8x8.drawString(2,3, "Found MPRLS sensor");
  delay(1000);
  u8x8.clearDisplay();
  delay(1000);
  
  pressureAmbiant = mpr.readPressure() / 68.94757293;


  /*TEST CASES*/
  changePressure(1.5);
  delay(3000);

  
  changePressure(0.7);
  delay(5000);
  
  
  changePressure(1.5);
  delay(5000);
}

 
//Unneeded
void loop() {}


//Fills up the bladder with 0 to 1.5 PSI of pressure
void changePressure(double pressureTarget){
  //Finds the pressure of the atmosphere in PSI
  //pressureAmbiant = mpr.readPressure() / 68.94757293; 
  
  //Finds the pressure of the bladder in PSI
  //At this point, read pressure should equal pressureAmbiant after conversion to PSI, so pressureBladder should be about 0
  double pressureBladder = (mpr.readPressure() / 68.94757293) - pressureAmbiant; 
  String pressureMessage = "";
  
  
  //Debugging code ----------------------------------
  pressureMessage = "Ambient";
  u8x8.drawString(2, 3, pressureMessage.c_str());
  delay(1500);
  
  pressureMessage = String(pressureAmbiant,4);
  u8x8.drawString(2, 3, pressureMessage.c_str());
  delay(1500);
  
  
  pressureMessage = "Bladder";
  u8x8.drawString(2, 3, pressureMessage.c_str());
  delay(1500);
  
  pressureMessage = String(pressureBladder,4);
  u8x8.drawString(2, 3, pressureMessage.c_str());
  delay(1500);
  //End of debugging code ---------------------------
  
  
  
  //If pressureTarget is out of range, exit the function
  if(pressureTarget < 0 || pressureTarget > 1.5){
    pressureMessage = "Invalid Pressure";
    u8x8.drawString(2, 3, pressureMessage.c_str());
    delay(3000);
      
    return;
  }

  if(pressureTarget < pressureBladder){
    pressureMessage = "Too high";
    u8x8.drawString(2, 3, pressureMessage.c_str());
    delay(1000);

    //Will continue a cycle of releasing air as long as the target pressure is lower than the pressure in the bladder
    while(pressureTarget < pressureBladder){
      //Activate the release valve for .3 seconds
      digitalWrite(valve, HIGH);    
      delay(300);
      digitalWrite(valve, LOW);
      
      //Update the read pressure of the bladder
      pressureBladder = (mpr.readPressure() / 68.94757293) - pressureAmbiant; 
      
      //Debug message
      pressureMessage = String(pressureBladder,4);
      u8x8.drawString(2, 3, pressureMessage.c_str());
      delay(1000);
    }
  }

  else if(pressureTarget > pressureBladder){
    pressureMessage = "Too low";
    u8x8.drawString(2, 3, pressureMessage.c_str());
    delay(1000);

    //Will continue a cycle of pumping air as long as the target pressure is higher than the pressure in the bladder
    while(pressureTarget > pressureBladder){
      //Activate the release motor for 2 seconds
      digitalWrite(motor, HIGH);    
      delay(2000);
      digitalWrite(motor, LOW);
      
      //Update the read pressure of the bladder
      pressureBladder = (mpr.readPressure() / 68.94757293) - pressureAmbiant;
      
      //Debug message
      pressureMessage = String(pressureBladder,4);
      u8x8.drawString(2, 3, pressureMessage.c_str());
      delay(1000);
    }
  }

  else{
    //Unlikely edge case that pressure is exactly where desired
    u8x8.drawString(2, 3, "No change necessary");
    u8x8.clearDisplay();
  }

  u8x8.clearDisplay();
}
