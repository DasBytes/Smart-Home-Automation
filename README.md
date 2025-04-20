# IoT Home Automation App

An Android-based smart home automation system that allows real-time control of a **dim light (5V DC)** and **fan** using **Firebase Realtime Database** and **NodeMCU ESP8266**. The app also features **WiFi status monitoring**, **location detection**, and **temperature display** via OpenWeatherMap API.

## 🔧 Features

- ✅ Real-time control of light and fan
- 📶 WiFi connection status monitoring
- 📍 Location detection using GPS
- 🌡️ Current temperature display from OpenWeatherMap
- 🔄 Firebase Realtime Database synchronization
- 🛠️ ESP8266-based microcontroller system

## 📱 Technologies Used

- **Android (Java)**
- **Firebase Realtime Database**
- **ESP8266 NodeMCU**
- **OpenWeatherMap API**
- **Google Play Location Services**

## ⚡ Hardware Components

| Component             | Reason for Use                          |
|-----------------------|------------------------------------------|
| NodeMCU ESP8266       | WiFi-enabled microcontroller              |
| 2-Channel Relay Module| Switches light and fan safely             |
| Dim Light (5V DC)     | Energy-saving and safer to control       |
| 5V DC Fan             | Safe and efficient cooling control       |
| Jumper Wires          | Easy connection between components       |
| Breadboard            | Temporary circuit setup                  |
| USB Power Adapter     | Powers the ESP8266 module                |


## 🧠 How It Works

- The Android app sends light/fan status to Firebase.
- ESP8266 reads from Firebase and toggles relays accordingly.
- App also fetches user location and weather data.



## 📦 How to Run

1. Upload the Arduino code to ESP8266.
2. Setup Firebase with correct database structure.
3. Replace API keys and Firebase credentials in the app.
4. Install and run the app on your Android phone.

---

**Made with by Pranta Das**
