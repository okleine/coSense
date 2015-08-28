package de.uzl.itm.ncoap.android.server.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import de.uzl.itm.ncoap.android.server.R;

/**
 * Created by olli on 26.08.15.
 */
public class SettingsDialog extends DialogFragment{

    private EditText txtProxy;

    private Set<Listener> listeners = new HashSet<>();


    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        if(activity instanceof Listener){
            listeners.add((Listener) activity);
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        super.onCreateDialog(savedInstanceState);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.ssp_config, null);
        this.txtProxy = (EditText) view.findViewById(R.id.ssp_host);

        builder.setView(view).setMessage("Configure Smart Service Proxy")

            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    for (Listener listener : listeners) {
                        listener.onProxyChanged(txtProxy.getText().toString());
                    }
                }
            })

            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Toast.makeText(getActivity(), "Canceled!", Toast.LENGTH_LONG).show();
                }
            });


        // Create the AlertDialog object and return it
        return builder.create();

    }


     public interface Listener {
        void onProxyChanged(String sspHost);
    }
}
