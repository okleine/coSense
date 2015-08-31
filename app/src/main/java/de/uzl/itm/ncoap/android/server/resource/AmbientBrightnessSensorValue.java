package de.uzl.itm.ncoap.android.server.resource;

/**
 * Created by olli on 18.05.15.
 */
public class AmbientBrightnessSensorValue extends SensorValue<Double> {

    public AmbientBrightnessSensorValue(double latitude, double longitude, Double value) {
        super(latitude, longitude, value);
    }

    @Override
    public boolean equals(Object object){
        if(!(object instanceof AmbientBrightnessSensorValue)){
            return false;
        }

        AmbientBrightnessSensorValue other = (AmbientBrightnessSensorValue) object;

        if(other.getLongitude() != this.getLongitude()) {
            return false;
        }

        if(other.getLatitude() != this.getLatitude()) {
            return false;
        }

        return other.getValue().equals(this.getValue());

    }
}
