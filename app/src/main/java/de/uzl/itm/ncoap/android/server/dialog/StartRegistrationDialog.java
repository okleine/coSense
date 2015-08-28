package de.uzl.itm.ncoap.android.server.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by olli on 26.08.15.
 */
public class StartRegistrationDialog extends DialogFragment {

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

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Perform Proxy Registration?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        for (Listener listener : listeners) {
                            listener.registerAtProxy();
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Toast.makeText(getActivity(), "Canceled!", Toast.LENGTH_LONG).show();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }


    public interface Listener {
        void registerAtProxy();
    }
}
