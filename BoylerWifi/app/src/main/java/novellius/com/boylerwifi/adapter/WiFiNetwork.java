package novellius.com.boylerwifi.adapter;

/**
 * Created by abraham on 27/05/17.
 */

public class WiFiNetwork {

    private String ssid;
    private float signalStrenght;

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public float getSignalStrenght() {
        return signalStrenght;
    }

    public void setSignalStrenght(float signalStrenght) {
        this.signalStrenght = signalStrenght;
    }

    @Override
    public String toString() {
        return "WiFiNetwork{" +
                "ssid='" + ssid + '\'' +
                ", signalStrenght=" + signalStrenght +
                '}';
    }
}
