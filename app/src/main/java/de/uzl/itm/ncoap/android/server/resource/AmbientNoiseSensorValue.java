package de.uzl.itm.ncoap.android.server.resource;

/**
 * Created by olli on 18.05.15.
 */
public class AmbientNoiseSensorValue extends SensorValue<Integer>{

    public AmbientNoiseSensorValue(double latitude, double longitude, Integer value) {
        super(latitude, longitude, value);
    }

    @Override
    public boolean equals(Object object){
        if(!(object instanceof AmbientNoiseSensorValue)){
            return false;
        }

        AmbientNoiseSensorValue other = (AmbientNoiseSensorValue) object;

        if(other.getLongitude() != this.getLongitude()) {
            return false;
        }

        if(other.getLatitude() != this.getLatitude()) {
            return false;
        }

        return other.getValue().equals(this.getValue());

    }
}
