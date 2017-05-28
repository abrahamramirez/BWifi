package novellius.com.boylerwifi.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.List;

import novellius.com.boylerwifi.R;
import novellius.com.boylerwifi.adapter.WiFiNetwork;
import novellius.com.boylerwifi.adapter.WiFiNetworkAdapter;

public class DisplaySSIDActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener{


    private static final String TAG = "****** DisplaySSIDAct ";
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 777;

    private List<WiFiNetwork> networks = new ArrayList<WiFiNetwork>();
    private WifiManager wifiManager;
    private WiFiBroadcastReceiver wifiBroadcastReceiver;
    private LocationManager locationManager;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private GoogleApiClient googleApiClient;
    private WiFiNetworkAdapter wiFiNetworkAdapter;

    // Elementos de la UI
    private ListView lstSsid;
    private ImageButton imgRescan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_ssid);

        // Inicializar instancias
        List<String> permissionsList = new ArrayList<String>();


        // Inicializar elementos de la UI
        lstSsid = (ListView) findViewById(R.id.lstSsid);
        imgRescan = (ImageButton) findViewById(R.id.imgRescan);
        imgRescan.setOnClickListener(this);


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

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiBroadcastReceiver = new WiFiBroadcastReceiver();
        registerReceiver(wifiBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (wifiManager.isWifiEnabled() == false) {
            Toast.makeText(this, "Encendidiendo WiFi", Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.i(TAG, "GPS habilitado");
        }
        else {
            Log.i(TAG, "GPS deshabilitado");

            googleApiClient = new GoogleApiClient.Builder(DisplaySSIDActivity.this)
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
                            // All location settings are satisfied. The client can initialize location
                            // requests here.
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
                                        DisplaySSIDActivity.this, 1000);
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

        networks.clear();
        wifiManager.startScan();
        Log.i(TAG, "Iniciando búsqueda en onCreate");
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.imgRescan){
            Toast.makeText(this, getText(R.string.scanning), Toast.LENGTH_SHORT).show();
            networks.clear();
            wifiManager.startScan();
        }
    }

    class WiFiBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            Log.d(TAG, "Recibido...");
            List<ScanResult> wifiList = wifiManager.getScanResults();

            // Poblar el array de POJOS wifiNetwork a partir de resultados de búsqueda de WiFi
            for (ScanResult wifiListItem : wifiList){
                WiFiNetwork wiFiNetwork = new WiFiNetwork();
                wiFiNetwork.setSsid(wifiListItem.SSID);

                if(!networks.contains(wiFiNetwork)) {
                    networks.add(wiFiNetwork);
                    Log.i(TAG, wifiListItem.SSID);
                }

                // Instanciar adaptador
                wiFiNetworkAdapter = new WiFiNetworkAdapter(DisplaySSIDActivity.this,
                        R.layout.wifi_network_layout,
                        networks.toArray(new WiFiNetwork[networks.size()]));
//        Log.d(TAG,"test: " + String.valueOf(adapterFoundDevices.areAllItemsEnabled()));
                // Asignar adaptador
                Log.d(TAG, networks.toString());
                lstSsid.setAdapter(wiFiNetworkAdapter);
            }

        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Revisar constantemente que GSP esté encendido??
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





}
