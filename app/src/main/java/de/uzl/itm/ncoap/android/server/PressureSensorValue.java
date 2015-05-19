package de.uzl.itm.ncoap.android.server;

/**
 * Created by olli on 18.05.15.
 */
public class PressureSensorValue extends SensorValue<Double> {

    public PressureSensorValue(double latitude, double longitude, Double value) {
        super(latitude, longitude, value);
    }

    @Override
    public boolean equals(Object object){
        if(!(object instanceof PressureSensorValue)){
            return false;
        }

        PressureSensorValue other = (PressureSensorValue) object;

        if(other.getLongitude() != this.getLongitude()) {
            return false;
        }

        if(other.getLatitude() != this.getLatitude()) {
            return false;
        }

        return other.getValue().equals(this.getValue());

    }
}
