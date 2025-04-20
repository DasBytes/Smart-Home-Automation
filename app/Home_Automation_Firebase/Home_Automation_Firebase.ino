#include <ESP8266WiFi.h>
#include <FirebaseESP8266.h>

#define WIFI_SSID "wifi name"
#define WIFI_PASSWORD "wifi pass"

#define FIREBASE_HOST "home-automation-ca913-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "YHLmGAW1ITJfXcnz1YKDI2M7sK3dGYpYuDeBbqbO"

// Try different GPIOs if needed
#define LIGHT_PIN D1  // GPIO14
#define FAN_PIN   D2 // GPIO12

FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

void setup() {
  Serial.begin(115200);
  pinMode(LIGHT_PIN, OUTPUT);
  pinMode(FAN_PIN, OUTPUT);
  digitalWrite(LIGHT_PIN, HIGH);
  digitalWrite(FAN_PIN, HIGH);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");

  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  if (!Firebase.pathExist(firebaseData, "/devices/light"))
    Firebase.setBool(firebaseData, "/devices/light", false);

  if (!Firebase.pathExist(firebaseData, "/devices/fan"))
    Firebase.setBool(firebaseData, "/devices/fan", false);
}

void loop() {
  if (Firebase.getBool(firebaseData, "/devices/light")) {
    bool lightState = firebaseData.boolData();
    digitalWrite(LIGHT_PIN, lightState ? LOW : HIGH);
    Serial.print("Light: ");
    Serial.println(lightState ? "ON" : "OFF");
  }

  if (Firebase.getBool(firebaseData, "/devices/fan")) {
    bool fanState = firebaseData.boolData();
    digitalWrite(FAN_PIN, fanState ? LOW : HIGH);
    Serial.print("Fan: ");
    Serial.println(fanState ? "ON" : "OFF");
  }

  delay(1000);
}
