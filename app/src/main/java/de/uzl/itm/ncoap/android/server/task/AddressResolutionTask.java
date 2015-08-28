package de.uzl.itm.ncoap.android.server.task;

import android.os.AsyncTask;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

import de.uzl.itm.ncoap.android.server.MainActivity;

/**
 * Created by olli on 27.08.15.
 */
public class AddressResolutionTask extends AsyncTask<String, Void, Void> {

    private MainActivity activity;

    public AddressResolutionTask(MainActivity activity){
        this.activity = activity;
    }

    @Override
    public Void doInBackground(String... params) {
        try {
            final InetAddress sspAddress = InetAddress.getByName(params[0]);
            this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.setTxtProxy(sspAddress.getHostAddress());
                }
            });

        }
        catch(final UnknownHostException ex){
            this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        }

        return null;
    }
}
