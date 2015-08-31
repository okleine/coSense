package de.uzl.itm.ncoap.android.server.resource;

import android.util.Log;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.uzl.itm.ncoap.application.server.webresource.linkformat.LongLinkAttribute;
import de.uzl.itm.ncoap.communication.dispatching.client.Token;
import de.uzl.itm.ncoap.message.options.ContentFormat;


/**
 * Created by olli on 17.05.15.
 */
public class AmbientBrightnessSensorResource extends SensorResource<Double, AmbientBrightnessSensorValue> {

    private static String TAG = AmbientBrightnessSensorResource.class.getSimpleName();

    private AmbientBrightnessSensorValue tmpStatus;

    private ScheduledFuture statusUpdateFuture;

    public AmbientBrightnessSensorResource(String uriPath, AmbientBrightnessSensorValue initialStatus, ScheduledExecutorService executor) {
        super(uriPath, initialStatus, executor);
        this.setLinkAttribute(new LongLinkAttribute(LongLinkAttribute.CONTENT_TYPE, ContentFormat.TEXT_PLAIN_UTF8));

        this.tmpStatus = initialStatus;

        //To avoid permanent updates update the status only every 10 seconds
        this.statusUpdateFuture = executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(!getStatus().equals(tmpStatus)){
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
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteEndpoint, Token token) {
        return false;
    }


    @Override
    public void updateEtag(SensorValue<Double> resourceStatus) {
        //Make the ETAG the first 4 bytes of the IEEE 754 representation
        byte[] etag = new byte[4];
        long tmp = Double.doubleToLongBits(getStatus().getValue());
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
