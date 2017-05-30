package novellius.com.boylerwifi.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.List;

import novellius.com.boylerwifi.R;
import novellius.com.boylerwifi.adapter.Network;
import novellius.com.boylerwifi.adapter.WiFiNetworkAdapter;
import novellius.com.boylerwifi.util.ConnectToWifi;

public class APConnectionActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        AdapterView.OnItemClickListener, OnConnectionTried{


    private static final String TAG = "****** DisplaySSIDAct ";
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 777;
    private static final int WEP = 0;
    private static final int WPA = 1;
    private static final int OPEN = 2;

    private List<Network> networks = new ArrayList<Network>();
    List<String> permissionsList = new ArrayList<String>();
    private WifiManager wifiManager;
    private WifiConfiguration wifiConfiguration;
    private WiFiBroadcastReceiver wifiBroadcastReceiver;
    private LocationManager locationManager;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private GoogleApiClient googleApiClient;
    private WiFiNetworkAdapter wiFiNetworkAdapter;
    private PopupWindow popupWindow;
    private View popupView;
    private LayoutInflater layoutInflater;
    private ViewGroup root;
    private Network network;

    // Elementos de la UI
    private TextView lblStatus;
    private ListView lstSsid;
    private ImageButton imgRescan;

    // Elementos de la ventana PopUp
    private TextView lblSsidToConnect;
    private TextView lblErrorCredentials;
    private EditText txtPassword;
    private CheckBox chkShowPassword;
    private Button btnConnect;
    private Button btnCancel;
    private ProgressBar pbConnecting;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ap_connection);

        // Inicializar instancias
        permissionsList = new ArrayList<String>();
        root = (ViewGroup) getWindow().getDecorView().getRootView();
        wifiConfiguration = new WifiConfiguration();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Inicializar elementos de la UI
        pbConnecting = (ProgressBar) findViewById(R.id.pbConnecting);
        lblStatus = (TextView) findViewById(R.id.lblStatus);
        lstSsid = (ListView) findViewById(R.id.lstSsid);
        lstSsid.setOnItemClickListener(this);
        imgRescan = (ImageButton) findViewById(R.id.imgRescan);
        imgRescan.setOnClickListener(this);

        checkPermissions();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiBroadcastReceiver = new WiFiBroadcastReceiver();
        registerReceiver(wifiBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (wifiManager.isWifiEnabled() == false) {
            Toast.makeText(this, "Encendidiendo WiFi", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

        wifiManager.startScan();
        Log.i(TAG, "Iniciando búsqueda en onCreate");

        List<WifiConfiguration> wifiConfigurations = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfiguration : wifiConfigurations) {
            Log.d(TAG, "preconfigured: " + wifiConfiguration.SSID);
        }
    }

    @Override
    public void setConnectionResult(boolean result) {
        Log.d(TAG, "setConnectionResult: " + result);
        if(result){
            Toast.makeText(this, getString(R.string.connection_successful), Toast.LENGTH_SHORT).show();
            // Iniciar nueva activity
        }
        else{
            pbConnecting.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getString(R.string.error_credentials), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.imgRescan){
            Toast.makeText(this, getText(R.string.scanning), Toast.LENGTH_SHORT).show();
            checkPermissions();
            wifiManager.startScan();
        }
        if(id == R.id.btnCancel){
            popupWindow.dismiss();
        }
        if(id == R.id.btnConnect){
//            new MakeRequest().execute("http://192.168.4.1/read?params=0");
            network.setPassword(txtPassword.getText().toString());
            pbConnecting.setVisibility(View.VISIBLE);
            Toast.makeText(this, getString(R.string.connecting), Toast.LENGTH_SHORT).show();
            new ConnectToWifi(wifiConfiguration, wifiManager, this).execute(network);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String ssid = null;
        if(adapterView.getAdapter().equals(wiFiNetworkAdapter)){        // Verificar que elemento fue seleccionado
            network = (Network) wiFiNetworkAdapter.getItem(i);
//            ssid = ssidList.get(i);

            // Obtener dimensiones del dipositivo
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int height = size.y;                            // Obtener altura del dispositivo
            int width = size.x;                             // Obtener largo del dispositivo

            // Inflar ventana popup
            layoutInflater = (LayoutInflater)getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            popupView = layoutInflater.inflate(R.layout.input_password_layout, null, false);

            // Inicializar PopUp Window
            popupWindow = new PopupWindow(popupView, (int)(width * 0.90), (int)(height * 0.45));
//            popupWindow = new PopupWindow(popupView);

            // Configuraciones adicionales de la ventana popup
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            popupWindow.setFocusable(true);
            popupWindow.setOutsideTouchable(false);
            popupWindow.setElevation(30);
            popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

            // Inicializar elementos de la ventana PopUp
            pbConnecting = (ProgressBar) popupView.findViewById(R.id.pbConnecting);
            lblSsidToConnect = (TextView) popupView.findViewById(R.id.lblSsidToConnect);
            txtPassword = (EditText) popupView.findViewById(R.id.txtPassword);
            chkShowPassword = (CheckBox) popupView.findViewById(R.id.chkShowPassword);
            btnConnect = (Button) popupView.findViewById(R.id.btnConnect);
            btnCancel = (Button) popupView.findViewById(R.id.btnCancel);

            btnConnect.setOnClickListener(this);
            btnCancel.setOnClickListener(this);

            chkShowPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b){
                        txtPassword.setTransformationMethod(null);
                        txtPassword.setSelection(txtPassword.getText().length());
                    }
                    else{
                        txtPassword.setTransformationMethod(new PasswordTransformationMethod());
                        txtPassword.setSelection(txtPassword.getText().length());
                    }
                }
            });
            lblSsidToConnect.setText(network.getSsid());

            // Mostrar ventana popup
            popupWindow.showAtLocation(popupView, Gravity.CENTER, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//            applyDim(root, 0.5f);
        }
    }




    private class WiFiBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            Log.d(TAG, "Recibido...");
            List<ScanResult> scanResults = wifiManager.getScanResults();
            networks.clear();
            // Poblar el array de POJOS network a partir de resultados de búsqueda de WiFi
            for (ScanResult scanResult : scanResults){

                Network network = new Network(scanResult.SSID, scanResult.capabilities);
                if(!networks.contains(network)){
                    networks.add(network);
                }

                wiFiNetworkAdapter = new WiFiNetworkAdapter(APConnectionActivity.this,
                        R.layout.wifi_network_layout,
                        networks.toArray(new Network[networks.size()]));
                lstSsid.setAdapter(wiFiNetworkAdapter);
            }
        }
    }




    @Override
    protected void onResume() {
        super.onResume();
        // Revisar constantemente que GSP esté encendido??
        checkPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    Log.d(TAG, "Permisos concedidos");
                    wifiManager.startScan();
                    Log.i(TAG, "Iniciando búsqueda en onRequestPermissionsResult");
                }
                else {
                    Log.d(TAG, "Permisos NO concedidos");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public void checkPermissions(){
        permissionsList.clear();
        Log.d(TAG, "Checking permissions...");
        // Revisar que los permisos adecuados estén concedidos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "ACCESS_FINE_LOCATION");
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "ACCESS_COARSE_LOCATION");
            permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        Log.i(TAG, "size: " + permissionsList.size());
        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }
        else {
            Log.i(TAG, "Permisos concedidos, escanear");
        }

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.i(TAG, "GPS habilitado");
        }
        else {
            Log.i(TAG, "GPS deshabilitado");

            googleApiClient = new GoogleApiClient.Builder(APConnectionActivity.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            builder.setAlwaysShow(true); //this is the key ingredient

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());

            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    final LocationSettingsStates state = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied.
                            Log.i(TAG, "All location settings are satisfied");
                            networks.clear();
                            wifiManager.startScan();
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the user
                            // a dialog.
                            Log.i(TAG, "Location settings are not satisfied");
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(
                                        APConnectionActivity.this, 1000);
                            }
                            catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            Log.i(TAG, "Location settings are not satisfied 2");
                            break;
                    }
                }
            });
        }
    }


//    private class ConnectToWifiTask extends AsyncTask<Network, Void, Boolean>{
//        OnConnectionTried onConnectionTried;
//        private String ssid;
//        private String password;
//        private String security;
//        private boolean result;
//
//
//        public ConnectToWifiTask(Activity activity){
//            onConnectionTried =(OnConnectionTried) activity;
//        }
//
//        @Override
//        protected void onPostExecute(Boolean aBoolean) {
//            super.onPostExecute(aBoolean);
//            onConnectionTried.setConnectionResult(aBoolean);
//        }
//
//        @Override
//        protected Boolean doInBackground(Network... network) {
//            Log.d(TAG, "" + network[0]);
//            ssid = network[0].getSsid();
//            password = network[0].getPassword();
//            security = network[0].getSecurity();
//
//            if(security.contains("WPA")) {
//                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
//                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
//                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
//                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
//            }
//            if(security.contains("WEP")){
//                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
//                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
//                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
//                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
//                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
//            }
//            wifiConfiguration.SSID = "\"" + ssid + "\"";
//            wifiConfiguration.preSharedKey = "\"" + password + "\"";
//
//            Log.d(TAG, "Connecting...");
//
//            wifiManager.addNetwork(wifiConfiguration);
//
//            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
//            for( WifiConfiguration i : list ) {
//                if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
//                    wifiManager.disconnect();
//                    wifiManager.enableNetwork(i.networkId, true);
//                    wifiManager.reconnect();
//
//                    break;
//                }
//            }
//            try {
//                Thread.currentThread();
//                Thread.sleep(7000);     // Esperear a realizar conexión exitosa
//            }
//            catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//            if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
//                String connectedSsid = wifiInfo.getSSID();
//                Log.d(TAG, "SSID connected: " + connectedSsid);
//                Log.d(TAG, "SSID to connect: " + ssid);
//                if(connectedSsid.equals("\"" + ssid + "\"")){
//                    Log.d(TAG, "Connection OK!");
//                    result = true;
//                }
//                else{
//                    Log.d(TAG, "Connection Error!");
//                    result = false;
//                }
//            }
//            else{
//                Log.d(TAG, "Connection Error 2!");
//                result = false;
//            }
//            Log.d(TAG, "AT Result: " + result);
//            return result;
//        }
//    }
//


    public static void applyDim(@NonNull ViewGroup parent, float dimAmount){
        Drawable dim = new ColorDrawable(Color.BLACK);
        dim.setBounds(0, 0, parent.getWidth(), parent.getHeight());
        dim.setAlpha((int) (255 * dimAmount));

        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.add(dim);
    }

    public static void clearDim(@NonNull ViewGroup parent) {
        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.clear();
    }


}
