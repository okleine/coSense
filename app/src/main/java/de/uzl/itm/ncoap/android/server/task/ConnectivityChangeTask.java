package de.uzl.itm.ncoap.android.server.task;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import de.uzl.itm.ncoap.android.server.MainActivity;

/**
 * Created by olli on 28.08.15.
 */
public class ConnectivityChangeTask extends AsyncTask<Void, Void, Void>{

    private MainActivity mainActivity;

    public ConnectivityChangeTask(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    protected Void doInBackground(Void... params) {
        ConnectivityManager cm = (ConnectivityManager) this.mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        String address = null;
        try{
            // connected to the internet
            if (activeNetwork != null) {
                // connected to wifi
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    address = getWifiData().getIpAddress();
                }
                // connected to the mobile internet
                else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                   address = getMobileConnectionData().getIpAddress();
                }
                //connected to something else (should never happen...)
                else{
                    address = getIPAddress();
                }
            }
        }
        catch(Exception ex){
            address = "ERROR";
        }

        this.mainActivity.setTxtIP(address);
        return null;
    }

    private ConnectionData getWifiData() throws Exception{
        String networkName;
        String clientIP;

        WifiManager wifiManager = (WifiManager) this.mainActivity.getSystemService(Context.WIFI_SERVICE);

        //SSID
        String wifiName = wifiManager.getConnectionInfo().getSSID();
        if(wifiName != null){
            networkName = wifiName;
        }
        else{
            networkName = "Unknown WiFi";
        }

        //IP
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        if(ip == 0){
            clientIP = "no";
        }
        else {
            clientIP = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
        }

        return new ConnectionData(networkName, clientIP);
    }

    private ConnectionData getMobileConnectionData() throws Exception{
        String networkName;

        TelephonyManager tm = (TelephonyManager) this.mainActivity.getSystemService(Context.TELEPHONY_SERVICE);
        String tmpName = tm.getNetworkOperatorName();
        if (tmpName != null){
            networkName = tmpName;
        }
        else {
            networkName = "Unknown Mobile";
        }

        return new ConnectionData(networkName, getIPAddress());
    }


    private class ConnectionData {

        private String networkName;
        private String ipAddress;

        private ConnectionData(String networkName, String ipAddress) {
            this.networkName = networkName;
            this.ipAddress = ipAddress;
        }

        public String getNetworkName() {
            return networkName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public boolean equals(Object object){
            if(!(object instanceof ConnectionData)){
                return false;
            }

            ConnectionData other = (ConnectionData) object;
            return (this.ipAddress.equals(other.ipAddress) && this.networkName.equals(other.networkName));
        }
    }


    private String getIPAddress() throws Exception {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        NetworkInterface networkInterface;

        while(networkInterfaces.hasMoreElements()){
            networkInterface = networkInterfaces.nextElement();

            for (Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();inetAddresses.hasMoreElements();){
                InetAddress inetAddress = inetAddresses.nextElement();

                if(!inetAddress.isLoopbackAddress()){
                    String address = inetAddress.getHostAddress();
                    //Very dirty for IPv6
                    if(!address.contains(":")) {
                        return (inetAddress.getHostAddress());
                    }
                }
            }
        }

        return "No IP Address";
    }
}
