# EPICS-Sensory-Devices
 Autism Treatment Center

Installation Instructions:- 
1. Flash the sketch and turn the ESP32 on.
2. Go to your phone and connect to 'esp32ap'
3. Click on the menu icon->Configure New AP
4. Select the Wi-Fi network that will be used to control the Cuff over
5. Enter in the password, Click connect. The webpage should just stop working(if successful) and then you can disconnect your phone from 'esp32ap'
6. If connection fails, the webpage should give options to reconnect.
7. Once the cuff is connected, install the mobile app on a Android/iOS device. Connect the Android/iOS device to the same Wi'Fi as the Cuff(ESP32)
8. Open the app, select a pressure level and click Turn On. Turn off will turn the motor off.
9. The app also features a database of pressure levels preffered by different people. To add to the pressure levels, click on the action button on the lower right corner of the app.
10. At this screen, click the action button on the lower right again and press "Add Profile"
11. Enter values and click Done.
12. To edit an existing profile, click on the same action button on the Home screen and now click on the profile that is to be edited. Repeat step 11 and the entry will be modified.
13. Thank you!

Project Link: https://epics.utdallas.edu/autism-center/projects/sensory-device/


To compile and develop project,
1. The Main.ino file runs in Arduino IDE. File uses Autoconnect, AsyncTCP, HTTPClient and ESPAsyncWebServer. All can be installed from the Arduino Manage Libraries.
2. For the react native mobile application, run "npm install" on the sensory-cuff folder. 
3. Then use "expo start" to run the app. App can be un using qr code on Android/iOS or on Android/iOS simulator.
4. https://expo.github.io/ for Documentation on Expo with React Native