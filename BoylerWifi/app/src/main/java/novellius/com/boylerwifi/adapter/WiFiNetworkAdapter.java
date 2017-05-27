package novellius.com.boylerwifi.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import novellius.com.boylerwifi.R;

/**
 * Created by abraham on 27/05/17.
 */

public class WiFiNetworkAdapter extends ArrayAdapter{

    private Context context;
    private int resourceId;
    private WiFiNetwork[] networks;


    public WiFiNetworkAdapter(@NonNull Context context,
                              @LayoutRes int resourceId,
                              @NonNull WiFiNetwork[] networks) {

        super(context, resourceId, networks);
        this.context = context;
        this.resourceId = resourceId;
        this.networks = networks;
    }


    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity)context).getLayoutInflater();
        View view = layoutInflater.inflate(resourceId, parent, false);

        // Inicializar UI elements
        TextView lblWiFiSsid = (TextView) view.findViewById(R.id.lblWiFiSSID);
        ImageView imgWiFiIcon = (ImageView) view.findViewById(R.id.imgWiFiIcon);

        lblWiFiSsid.setText(networks[position].getSsid());

        return view;
    }
}
