package de.uzl.itm.ncoap.android.server;

/**
 * Created by olli on 18.05.15.
 */
public class NoiseSensorValue extends SensorValue<Integer>{

    public NoiseSensorValue(double latitude, double longitude, Integer value) {
        super(latitude, longitude, value);
    }

    @Override
    public boolean equals(Object object){
        if(!(object instanceof NoiseSensorValue)){
            return false;
        }

        NoiseSensorValue other = (NoiseSensorValue) object;

        if(other.getLongitude() != this.getLongitude()) {
            return false;
        }

        if(other.getLatitude() != this.getLatitude()) {
            return false;
        }

        return other.getValue().equals(this.getValue());

    }
}
