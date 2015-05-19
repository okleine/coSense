package de.uzl.itm.ncoap.android.server;

import android.util.Log;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.uniluebeck.itm.ncoap.application.server.webservice.linkformat.LongLinkAttribute;
import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

/**
 * Created by olli on 18.05.15.
 */
public class PressureSensorService extends AbstractSensorService<PressureSensorValue> {

    private static String TAG = LightSensorService.class.getSimpleName();
    private static String SENSOR_NAME = "Pressure-Sensor";


    private static HashMap<Long, String> payloadTemplates = new HashMap<>();
    static{
        //Add template for plaintext UTF-8 payload
        payloadTemplates.put(
                ContentFormat.TEXT_PLAIN_UTF8,
                "Ambient pressure at latitude %.10f and longitude %.10f is %.10f hPa."
        );

        //Add template for XML payload
        payloadTemplates.put(
                ContentFormat.APP_N3,
                "@prefix exp: <http://example.org/itm/light#> .\n" +
                        "@prefix geo: <http://www.opengis.net/ont/geosparql#> .\n" +
                        "@prefix ssn: <http://purl.oclc.org/NET/ssnx/ssn#> .\n" +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
                        "@prefix sf: <http://www.opengis.net/ont/sf#> .\n" +
                        "@prefix vo: <http://www.auto-nomos.de/ontologies/vanet-ontology#> .\n\n" +

                        "exp:" + SENSOR_NAME + "\n" +
                        "\tvo:hasLocation exp:" + SENSOR_NAME + "-Position ;\n" +
                        "\texp:" + SENSOR_NAME + "-Position a sf:Point;\n" +
                        "\tgeo:asWKT \"<http://www.opengis.net/def/crs/OGC/1.3/CRS84>\n" +
                        "\tPOINT(%.10f %.10f)\"ˆˆgeo:wktLiteral .\n\n" +

                        "exp:" + SENSOR_NAME + "-Observation a ssn:Observation;\n" +
                        "\tssn:observedBy\n" +
                        "\t\texp:" + SENSOR_NAME + ";\n" +
                        "\tssn:observedResult\n" +
                        "\t\texp:" + SENSOR_NAME + "-SensorOutput .\n\n" +

                        "exp:" + SENSOR_NAME + "-SensorOutput a ssn:SensorOutput;\n" +
                        "\tssn:hasValue\n" +
                        "\t\t\"%.10f\"ˆˆxsd:double ."
        );
    }

    private PressureSensorValue tmpStatus;
    private byte[] etag = new byte[1];
    private ScheduledFuture statusUpdateFuture;

    protected PressureSensorService(String uriPath, PressureSensorValue initialStatus, ScheduledExecutorService executor) {
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

    protected void setPressureValue(PressureSensorValue sensorValue){
        this.tmpStatus = sensorValue;
    }

    @Override
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteEndpoint, Token token) {
        return false;
    }

    @Override
    public byte[] getEtag(long contentFormat) {
        return this.etag;
    }

    @Override
    public void updateEtag(PressureSensorValue resourceStatus) {
        //Make the ETAG the first 4 bytes of the IEEE 754 representation
        byte[] etag = new byte[4];
        long tmp = Double.doubleToLongBits(getStatus().getValue());
        for(int i = 0; i < 4; i++){
            etag[i] = (byte) (tmp >> (8 * (7-i)) & 0xFF);
        }
        this.etag = etag;
    }


    @Override
    public void shutdown(){
        super.shutdown();
        boolean canceled = this.statusUpdateFuture.cancel(true);
        Log.d(TAG, "Resource status updated canceled (" + canceled + ")");
    }

    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        String template = payloadTemplates.get(contentFormat);

        if(template != null){
            double lat = getStatus().getLatitude();
            double lon = getStatus().getLongitude();
            double value = getStatus().getValue();

            return String.format(Locale.ENGLISH, template, lat, lon, value).getBytes(CoapMessage.CHARSET);
        }
        else{
            return null;
        }
    }


}
