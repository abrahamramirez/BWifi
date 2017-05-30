package novellius.com.boylerwifi.adapter;

/**
 * Created by abraham on 27/05/17.
 */

public class Network {

    private String ssid;
    private String password;
    private String security;

    public Network() {
    }

    public Network(String ssid, String security) {
        this.ssid = ssid;
        this.security = security;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSecurity() {
        return security;
    }

    public void setSecurity(String security) {
        this.security = security;
    }

    @Override
    public String toString() {
        return "Network{" +
                "ssid='" + ssid + '\'' +
                ", password='" + password + '\'' +
                ", security='" + security + '\'' +
                '}';
    }
}
