#include <ESP8266WiFi.h>
#include <aREST.h>
#include <EEPROM.h>

#define LISTEN_PORT  80           // The port to listen for incoming TCP connections
aREST rest = aREST();             // Create aREST instance

// EEPROM Map
//  0   1   2...
// 


// WiFi parameters
const char* ssidAp = "TEST3333";          // SSID a mostrar en modo AP
const char* passwordAp = "pepsiman11";    // password para entrar a red en modo AP
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
//  EEPROM.write(0,0);
  
  pinMode(OUT, OUTPUT);
  digitalWrite(OUT, 1);
  delay(2000);
  digitalWrite(OUT, 0);
  
  // Init variables and expose them to REST API
  temperature = 24;
  humidity = 40;
  rest.variable("temperature",&temperature);
  rest.variable("humidity",&humidity);

  // Function to be exposed
  rest.function("save", saveData);
  rest.function("read", readData);  

  // Give name & ID to the device (ID should be 6 characters long)
  rest.set_id("1");
  rest.set_name("esp8266");

  Serial.println("Read wifi data from EEPROM...");
  delay(1000);

  if(readWifiData().length() > 0){    // Existen datos, intentar conectarse a la red
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
    }
    else{
      String ip = WiFi.localIP().toString();
      Serial.print(ip);
      if(ip.equals("0.0.0.0")){
//        asm volatile ("  jmp 0"); 
        Serial.println("Wifi connection error, create AP...");
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
    }
  }
  else{           // No hay datos previos, entrar en modo AP
    Serial.println("No WifiData, create AP...");
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

  
  
 
}

void loop() {
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

// ----------------------------------------
// ---------- Funciones REST --------------
//
int saveData(String command) {
  writeWifiData(command);
  return 1;
  delay(3000);
  asm volatile ("  jmp 0"); 
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
  int dataSize = (int)EEPROM.read(0);      // Leer tamaño del String que contiene SSID & pass
  if(dataSize > 0){
    Serial.println("Reading SSID and pass");
    Serial.print("Read size:");  Serial.println(dataSize);
    for (int i = 0; i < dataSize; i++){
      data += char(EEPROM.read(i));
    }
    Serial.print("Data: "); Serial.println(data);
  }
  return data;
}

/**
 * Escribe en la memoria EEPROM la cadena con formato: SSID,password
 * */
void writeWifiData(String ssidAndPass){
  int len = ssidAndPass.length();
  EEPROM.write(0, len);
  Serial.print("Saved size:");  Serial.println(len);
  
  for (int i = 0; i < len; ++i){
    EEPROM.write(i + 1, ssidAndPass[i]);    // Dirección 0 reservada para tamaño del String
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




