package novellius.com.boylerwifi.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ListView;


import java.util.ArrayList;
import java.util.List;

import novellius.com.boylerwifi.R;

public class HomeConnectionActivity extends AppCompatActivity {

    private static final String TAG = "**** HomeConnectionAct ";
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 893;

    // Elementos de la UI
    private ImageButton imgRescanHome;
    private ListView lstSsidHome;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_connection);
//        new MakeRequest().execute("http://192.168.4.1/read?params=0");

        // Inicializar elementos de la UI
        imgRescanHome = (ImageButton) findViewById(R.id.imgRescanHome);
        lstSsidHome = (ListView) findViewById(R.id.lstSsidHome);

        // Inicializar otros elementos
        List<String> permissionsList = new ArrayList<String>();


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



    }



}
