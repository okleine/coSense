package de.uzl.itm.ncoap.android.server.resource;

import android.util.Log;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Created by olli on 17.05.15.
 */
public class AmbientBrightnessSensorResource extends SensorResource<Double, AmbientBrightnessSensorValue> {

    private static String TAG = AmbientBrightnessSensorResource.class.getSimpleName();

    private AmbientBrightnessSensorValue tmpStatus;

    private ScheduledFuture statusUpdateFuture;

    public AmbientBrightnessSensorResource(String uriPath, AmbientBrightnessSensorValue initialStatus, ScheduledExecutorService executor) {
        super(uriPath, initialStatus, executor);

        this.tmpStatus = initialStatus;

        //To avoid permanent updates update the status only every 10 seconds
        this.statusUpdateFuture = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(!getResourceStatus().equals(tmpStatus)){
                    setResourceStatus(tmpStatus, 10);
                }
            }
        }, 1, 10, TimeUnit.SECONDS);
    }

    @Override
    public String getPlainObservedPropertyName() {
        return "Ambient Brightness";
    }

    @Override
    public String getRDFSensorType() {
        return "AmbientBrightnessSensor";
    }

    @Override
    public String getRDFObservedProperty() {
        return "ambientBrightness";
    }


    public void setLightValue(AmbientBrightnessSensorValue sensorValue){
        this.tmpStatus = sensorValue;
    }


    @Override
    public void updateEtag(SensorValue<Double> resourceStatus) {
        //Make the ETAG the first 4 bytes of the IEEE 754 representation
        byte[] etag = new byte[4];
        long tmp = Double.doubleToLongBits(getResourceStatus().getValue());
        for(int i = 0; i < 4; i++){
            etag[i] = (byte) (tmp >> (8 * (7-i)) & 0xFF);
        }
        setEtag(etag);
    }

    @Override
    public void shutdown(){
        super.shutdown();
        boolean canceled = this.statusUpdateFuture.cancel(true);
        Log.d(TAG, "Resource status updated canceled (" + canceled + ")");
    }
}
