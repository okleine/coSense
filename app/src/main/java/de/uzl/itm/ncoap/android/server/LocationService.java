package de.uzl.itm.ncoap.android.server;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;

import de.uniluebeck.itm.ncoap.communication.dispatching.client.Token;
import de.uniluebeck.itm.ncoap.message.CoapMessage;
import de.uniluebeck.itm.ncoap.message.options.ContentFormat;

/**
 * Created by olli on 17.05.15.
 */
public class LocationService extends AbstractSensorService<LocationValue> {

    private static String TAG = LocationService.class.getSimpleName();
    private static String SENSOR_NAME = "Location-Sensor";

    private static HashMap<Long, String> payloadTemplates = new HashMap<>();
    static{
        //Add template for plaintext UTF-8 payload
        payloadTemplates.put(
                ContentFormat.TEXT_PLAIN_UTF8,
                "Current position is %.10f (LAT) and %.10f (LON)."
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
                        "\tPOINT(%.10f %.10f)\"ˆˆgeo:wktLiteral .\n\n"
        );
    }

    protected LocationService(String uriPath, LocationValue initialStatus, ScheduledExecutorService executor) {
        super(uriPath, initialStatus, executor);
    }

    @Override
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteEndpoint, Token token) {
        return false;
    }

    @Override
    public byte[] getEtag(long contentFormat) {
        return new byte[1];
    }

    @Override
    public void updateEtag(LocationValue locationValue) {

    }

    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        String template = payloadTemplates.get(contentFormat);

        if(template != null){
            return String.format(Locale.ENGLISH, template, getStatus().getLatitude(), getStatus().getLongitude()).getBytes(CoapMessage.CHARSET);
        }
        else{
            return null;
        }
    }
}
