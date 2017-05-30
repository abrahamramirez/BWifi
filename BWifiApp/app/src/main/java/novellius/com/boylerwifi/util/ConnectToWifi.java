package novellius.com.boylerwifi.util;

import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import novellius.com.boylerwifi.adapter.Network;


public class ConnectToWifi extends AsyncTask<Network, Void, Boolean> {

    private static final String TAG = "****** ConnectToWifi ";

    private String ssid = null;
    private String password = null;
    private String security = null;
    private WifiConfiguration wifiConfiguration;
    private WifiManager wifiManager;


    public ConnectToWifi(WifiConfiguration wifiConfiguration, WifiManager wifiManager) {
        this.wifiConfiguration = wifiConfiguration;
        this.wifiManager = wifiManager;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
    }

    @Override
    protected Boolean doInBackground(Network... network) {
        Log.d(TAG, "" + network[0]);
        ssid = network[0].getSsid();
        password = network[0].getPassword();
        security = network[0].getSecurity();

        if(security.contains("WPA")) {
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        }
        if(security.contains("WEP")){
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        }
        wifiConfiguration.SSID = "\"" + ssid + "\"";
        wifiConfiguration.preSharedKey = "\"" + password + "\"";

        Log.d(TAG, "Connecting...");

        wifiManager.addNetwork(wifiConfiguration);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }
        }
        try {
            Thread.sleep(5000);     // Esperear a realizar conexión exitosa
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            String connectedSsid = wifiInfo.getSSID();
            Log.d(TAG, "SSID connected: " + connectedSsid);
            Log.d(TAG, "SSID to connect: " + ssid);
            if(connectedSsid.equals("\"" + ssid + "\"")){
                Log.d(TAG, "Connection OK!");
                return true;
            }
            else{
                Log.d(TAG, "Connection Error!");
                return false;
            }
        }
        else{
            Log.d(TAG, "Connection Error 2!");
            return false;
        }
    }
}
