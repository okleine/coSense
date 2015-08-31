package de.uzl.itm.ncoap.android.server.resource;

import android.util.Log;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.uzl.itm.ncoap.application.server.webresource.linkformat.LongLinkAttribute;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.message.options.ContentFormat;


/**
 * Created by olli on 18.05.15.
 */
public class NoiseSensorResource extends SensorResource<Integer, NoiseSensorValue> {

    private static String TAG = NoiseSensorResource.class.getSimpleName();

    private static String PLAIN_OBSERVED_PROPERTY_NAME = "Ambient Noise";
    private static String RDF_OBSERVED_PROPERTY_NAME = "ambientNoise";


    private NoiseSensorValue tmpStatus;
    private ScheduledFuture statusUpdateFuture;


    public NoiseSensorResource(String uriPath, NoiseSensorValue initialStatus, ScheduledExecutorService executor) {
        super(uriPath, initialStatus, executor);
        this.setLinkAttribute(new LongLinkAttribute(LongLinkAttribute.CONTENT_TYPE, ContentFormat.TEXT_PLAIN_UTF8));

        this.tmpStatus = initialStatus;

        //To avoid permanent updates update the status only every 5 seconds
        this.statusUpdateFuture = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(!getStatus().equals(tmpStatus)){
                    setResourceStatus(tmpStatus, 5);
                }
            }
        }, 1, 5, TimeUnit.SECONDS);
    }


    public void setNoiseValue(NoiseSensorValue sensorValue){
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
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteEndpoint, Token token) {
        return false;
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