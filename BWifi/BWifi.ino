#include <ESP8266WiFi.h>
#include <aREST.h>
#include <EEPROM.h>

// **************************************
// EEPROM Map
//
// 0.   Modo: A=AP, W=WifiClient   
// 1.   Lenght: Tamaño del String que contiene SSID y password, por defecto 255
// 2-n  WifiData: String que contiene SSID y password.


#define LISTEN_PORT  80                     // The port to listen for incoming TCP connections
aREST rest = aREST();                       // Create aREST instance

const char* ssidAp = "BoylerWifi";          // SSID a mostrar en modo AP
const char* passwordAp = "pepsiman11";      // password para entrar a red en modo AP
String sSsid = "";
String sPassword = "";
const int OUT = 5;
String wifiData = "";
int len = 0;
int delim = 0;
int timeout = 0;
WiFiServer server(LISTEN_PORT);             // Create an instance of the server

// Variables to be exposed to the API
int temperature;
int humidity;



void setup(void){
  Serial.begin(9600);
  EEPROM.begin(512);
  
//  EEPROM.write(1, 255);
//  EEPROM.commit();
  
  pinMode(OUT, OUTPUT);

  // REST config
  rest.variable("temperature",&temperature);
  rest.variable("humidity",&humidity);
  rest.function("read",readData);
  rest.function("save",saveData);
  rest.function("reset", resetMcu);
  rest.set_id("1");
  rest.set_name("esp8266");
  
  Serial.println("Read WiFi data from EEPROM...");
  delay(1000);

  // Comprobar si existen datos WiFi en EEPROM
  if((int)EEPROM.read(1) != 255){          
    Serial.print("WiFi data found, try connection. ");    
    WiFi.softAPdisconnect();
    WiFi.disconnect();

    // Obtener SSID y password de EEPROM
    wifiData = "";
    len = (int)EEPROM.read(1);              // Leer tamaño del String que contiene SSID & password  
    if(len != 255){
      Serial.println("Reading SSID and pass"); Serial.print("Read size:");  Serial.println(len);
      for (int i = 2; i < len + 2; i++){
        wifiData += char(EEPROM.read(i));
      }
      Serial.print("WifiData: "); Serial.println(wifiData);
    }
    // Descomponer SSID y password respectivamente
    delim = wifiData.indexOf(",");
    sSsid = wifiData.substring(0, delim);
    sPassword = wifiData.substring(delim + 1);

    // Intentar conectar a red WiFi hasta agotar tiempo de espera
    WiFi.mode(WIFI_STA);
    WiFi.begin(sSsid.c_str(), sPassword.c_str());
    timeout = 0;
    while (WiFi.status() != WL_CONNECTED){
      delay(500);
      Serial.print(".");              // Caracter a imprimir mientras se realiza la conexión
      timeout++;
      if(timeout == 50){
        break;
      }
    }
    if(timeout == 150){
      Serial.println("WiFi connection error, create AP...");
      delay(1000);
      initApMode(); 
    }
    else{                             // Error de conexión, entrar en modo AP
      String ip = WiFi.localIP().toString();
      Serial.print(ip);
      if(ip.equals("0.0.0.0")){       // Esto se lanza cuando los datos de acceso WiFi son incorrectos
        Serial.println("Wifi connection error 2, create AP...");
        delay(1000);
        initApMode();
      }
      else{                           // Conexión wifi exitosa
        EEPROM.write(0, 'W');
        EEPROM.commit();
        server.begin();
        Serial.println("Server started ");
        Serial.println("Connection successfull to wifi network :)");
      }
    }
  }
  else{                               // No hay datos previos, entrar en modo AP
    Serial.println("No WifiData, create AP...");
    delay(1000);
    initApMode();
  }
  Serial.print("MODE: "); Serial.println(char(EEPROM.read(0)));
}


void loop() {
  if(char(EEPROM.read(0)) == 'A'){
    // Handle REST calls
    
  }
  else if(char(EEPROM.read(0)) == 'W'){
    
  }

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
  int len = command.length();
  EEPROM.write(1, len);
  EEPROM.commit();
  Serial.print("Saved size: ");  Serial.println(len);
  
  for (int i = 0; i < len; ++i){
    EEPROM.write(i + 2, command[i]);    
    EEPROM.commit();
    Serial.print("Wrote: ");
    Serial.println(command[i]);
  }
  Serial.print("EEPROM 1: "); Serial.println((int)EEPROM.read(1));
  Serial.println("New wifi data saved");
  return 1;
}

int readData(String command) {  
  Serial.println("REST read called...");
  return 2;
}

int resetMcu(String command) {  
  setup();
  return 2;
}


void initApMode(){
  EEPROM.write(0, 'A');
  EEPROM.commit();
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


void testBlink(){
  digitalWrite(OUT, 1);
  delay(2000);
  digitalWrite(OUT, 0);
}

