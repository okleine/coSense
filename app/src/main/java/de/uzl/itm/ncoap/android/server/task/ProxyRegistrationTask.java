package de.uzl.itm.ncoap.android.server.task;

import android.os.AsyncTask;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.net.URI;

import de.uzl.itm.ncoap.android.server.MainActivity;
import de.uzl.itm.ncoap.application.client.ClientCallback;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;

/**
 * Created by olli on 28.08.15.
 */
public class ProxyRegistrationTask extends AsyncTask<String, Void, Void>{

    private MainActivity mainActivity;

    public ProxyRegistrationTask(MainActivity mainActivity){

        this.mainActivity = mainActivity;
    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            URI registrationUri = new URI("coap", null, params[0], 5683, "/registry", null, null);
            CoapRequest request = new CoapRequest(MessageType.CON, MessageCode.POST, registrationUri);
            mainActivity.getCoapEndpoint().sendCoapRequest(request, new ClientCallback() {

                @Override
                public void processCoapResponse(CoapResponse coapResponse) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mainActivity, "Registered...", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }, new InetSocketAddress(params[0], 5683));


        } catch (final Exception ex) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mainActivity, "ERROR: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        return null;
    }
}
