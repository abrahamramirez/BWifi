package novellius.com.boylerwifi.activity;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import novellius.com.boylerwifi.R;

public class WizardNewConnectionActivity extends AppCompatActivity {

    private static final String TAG = "****** DisplaySSIDAct ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_new_connection);

//        new MakeRequest().execute("http://192.168.4.1/read?params=0");
    }



}
