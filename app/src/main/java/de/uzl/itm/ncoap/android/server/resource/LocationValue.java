package de.uzl.itm.ncoap.android.server.resource;

/**
 * Created by olli on 18.05.15.
 */
public class LocationValue extends SensorValue<Void> {

    public LocationValue(double latitude, double longitude, Void value) {
        super(latitude, longitude, value);
    }
}
