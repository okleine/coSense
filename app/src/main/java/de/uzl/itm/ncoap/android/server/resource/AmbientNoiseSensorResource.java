package de.uzl.itm.ncoap.android.server.resource;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Created by olli on 18.05.15.
 */
public class AmbientNoiseSensorResource extends SensorResource<Integer, AmbientNoiseSensorValue> {

    private static String TAG = AmbientNoiseSensorResource.class.getSimpleName();

    private static String PLAIN_OBSERVED_PROPERTY_NAME = "Ambient Noise";
    private static String RDF_OBSERVED_PROPERTY_NAME = "ambientNoise";


    private AmbientNoiseSensorValue tmpStatus;
    private ScheduledFuture statusUpdateFuture;


    public AmbientNoiseSensorResource(String uriPath, AmbientNoiseSensorValue initialStatus, ScheduledExecutorService executor) {
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


    public void setNoiseValue(AmbientNoiseSensorValue sensorValue){
        this.tmpStatus = sensorValue;
    }


    @Override
    public String getPlainObservedPropertyName() {
        return PLAIN_OBSERVED_PROPERTY_NAME;
    }

    @Override
    public String getRDFSensorType() {
        return "AmbientNoiseSensor";
    }

    @Override
    public String getRDFObservedProperty() {
        return RDF_OBSERVED_PROPERTY_NAME;
    }


    @Override
    public void updateEtag(SensorValue<Integer> resourceStatus) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(resourceStatus.getValue());
        setEtag(buffer.array());
    }


    @Override
    public void shutdown(){
        super.shutdown();
        boolean canceled = this.statusUpdateFuture.cancel(true);
        Log.d(TAG, "Resource status updated canceled (" + canceled + ")");
    }




}