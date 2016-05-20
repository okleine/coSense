package de.uzl.itm.ncoap.android.server.resource;

/**
 * Created by olli on 18.05.15.
 */
public abstract class SensorValue <T>{

    private double latitude;
    private double longitude;
    private T value;
    private String xsdType = null;

    public SensorValue(double latitude, double longitude, T value){
        this.latitude = latitude;
        this.longitude = longitude;
        this.value = value;

        if(value instanceof Integer){
            xsdType = "xsd:int";
        }
        else if(value instanceof Double){
            xsdType = "xsd:double";
        }
        else if(value instanceof String){
            xsdType = "xsd:string";
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public T getValue() {
        return value;
    }

    public String getXsdType(){
        return this.xsdType;
    }

    @Override
    public boolean equals(Object object){
        if(!(object instanceof SensorValue))
            return false;

        SensorValue other = (SensorValue) object;

        if(other.getLongitude() != this.getLongitude())
            return false;

        if(other.getLatitude() != this.getLatitude())
            return false;

        return true;
    }
}
