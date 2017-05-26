#include <ESP8266WiFi.h>
#include <aREST.h>
#include <EEPROM.h>

#define LISTEN_PORT  80           // The port to listen for incoming TCP connections
aREST rest = aREST();             // Create aREST instance


// EEPROM Map
//
// 0.   Modo: A=AP, W=WifiClient   
// 1.   Lenght: Tamaño del String que contiene SSID y password, por defecto 255
// 2-n  WifiData: String que contiene SSID y password.

const char* ssidAp = "BoylerWifi";          // SSID a mostrar en modo AP
const char* passwordAp = "pepsiman11";      // password para entrar a red en modo AP
const char* ssid = "";
const char* password = "";
const int OUT = 5;
String wifiData = "";
int len = 0;
int timeout = 0;
WiFiServer server(LISTEN_PORT);   // Create an instance of the server

// Variables to be exposed to the API
int temperature;
int humidity;

// Declare functions to be exposed to the API
int ledControl(String command);

void setup(void){
  Serial.begin(9600);
  EEPROM.begin(512);

  // REST config
  rest.variable("temperature",&temperature);
  rest.variable("humidity",&humidity);
  rest.function("read",readData);
  rest.function("save",saveData);
  rest.set_id("1");
  rest.set_name("esp8266");
  
  pinMode(OUT, OUTPUT);
  digitalWrite(OUT, 1);
  delay(2000);
  digitalWrite(OUT, 0);
 
  Serial.println("Read wifi data from EEPROM...");
  delay(1000);

  if(EEPROM.read(1) != 255){          // Comprobar si existen datos Wifi
    // Existen datos, intentar conectarse a la red 
    
    Serial.println("Wifi data found, try connection");   
    WiFi.begin(ssid, password);
    // Intentar conectar hasta agotar tiempo de espera
    while (WiFi.status() != WL_CONNECTED){
      delay(500);
      Serial.print("-");              // Caracter a imprimir mientras se realiza la conexión
      timeout++;
      if(timeout == 20){
        break;
      }
    }
    if(timeout == 150){
      Serial.println("ERROR"); 
      Serial.println("Wifi connection error, create AP...");
      delay(1000);
      initApMode(); 
    }
    else{                             // Error de conexión, entrar en modo AP
      String ip = WiFi.localIP().toString();
      Serial.print(ip);
      if(ip.equals("0.0.0.0")){
        Serial.println("Wifi connection error 2, create AP...");
        delay(1000);
        initApMode();
      }
      else{                           // Conexión wifi exitosa
        EEPROM.write(0, 'W');
        Serial.println("Connection successfull to wifi network");
      }
    }
  }
  else{           // No hay datos previos, entrar en modo AP
    Serial.println("No WifiData, create AP...");
    delay(1000);
    
    initApMode();
  }

  
  
  Serial.print("MODE: "); Serial.println(char(EEPROM.read(0)));
}

void loop() {
  if(char(EEPROM.read(0)) == 'A'){
    // Handle REST calls
    WiFiClient client = server.available();
    if (!client) {
      return;
    }
    while(!client.available()){
      delay(1);
    }
    rest.handle(client);
  }
  else if(char(EEPROM.read(0)) == 'W'){
    Serial.println("wifi mode");
  }
  
}

// ----------------------------------------
// ---------- Funciones REST --------------
//
int saveData(String command) {
  writeWifiData(command);
  Serial.println("New wifi data saved, restaring...");
  delay(1000);
  ESP.reset(); 
  return 1;
}

int readData(String command) {  
  splitWifiData(readWifiData());
  return 2;
}


/**
 * Obtiene y retorna desde EEPROM una cadena que contiene SSID,password
 * */
String readWifiData(){
  String data = "";
  int dataSize = (int)EEPROM.read(1);      // Leer tamaño del String que contiene SSID & pass
  if(dataSize != 255){
    Serial.println("Reading SSID and pass");
    Serial.print("Read size:");  Serial.println(dataSize);
    for (int i = 2; i < dataSize; i++){
      data += char(EEPROM.read(i));
    }
    Serial.print("Wifi Data: "); Serial.println(data);
  }
  return data;
}

/**
 * Escribe en la memoria EEPROM la cadena con formato: SSID,password
 * */
void writeWifiData(String ssidAndPass){
  int len = ssidAndPass.length();
  EEPROM.write(1, len);
  Serial.print("Saved size:");  Serial.println(len);
  
  for (int i = 0; i < len; ++i){
    EEPROM.write(i + 2, ssidAndPass[i]);    // Dirección 0 reservada para tamaño del String
    Serial.print("Wrote: ");
    Serial.println(ssidAndPass[i]);
  }
}


/**
 * Descompone la cadena con formato SSID,password colocando cada
 * elemento en su char* correspondiente.
 * */
void splitWifiData(String ssidAndPass){
  int index = ssidAndPass.indexOf(",");
  String sSsid = ssidAndPass.substring(0, index);
  String sPassword = ssidAndPass.substring(index + 1);

  ssid = sSsid.c_str();
  password = sPassword.c_str();
  
  Serial.print("ssid: "); Serial.println(ssid);
  Serial.print("password: "); Serial.println(password);
}


void initApMode(){
  EEPROM.write(0, 'A');
  delay(1000);
  
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssidAp, passwordAp);
  Serial.println("WiFi AP created");

  server.begin();
  Serial.println("Server started");

  IPAddress myIP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(myIP); 
}

