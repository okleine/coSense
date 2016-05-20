package de.uzl.itm.ncoap.android.server.resource;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;

import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.options.ContentFormat;


/**
 * Created by olli on 17.05.15.
 */
public class LocationResource extends SensorResource<Void, LocationValue> {

    private static String TAG = LocationResource.class.getSimpleName();

    private static String PLAIN_TEXT_TEMPLATE =
            "Current position is at latitude \"%.10f\" and longitude \"%.10f\".";

    private static String RDF_SENSOR_TYPE = "PositionSensor";

    private static String TURTLE_STRING_WITHOUT_OBSERVATION =
            "@prefix ssn: <http://purl.oclc.org/NET/ssnx/ssn#> .\n" +
            "@prefix sf: <http://www.opengis.net/ont/sf#> .\n" +
            "@prefix " + SENSORS_ONTOLOGY_ABBREVIATION + ": <" + SENSORS_ONTOLOGY_NAMESPACE + "> .\n" +
            "@prefix dul: <http://www.loa-cnr.it/ontologies/DUL.owl#> .\n\n" +

            "_:sensor a " + SENSORS_ONTOLOGY_ABBREVIATION + ":" + RDF_SENSOR_TYPE + ";\n\t" +
                "ssn:onPlatform  _:phone .\n\n" +

            "_:phone dul:hasLocation _:location ;\n\t" +
                    " a " + SENSORS_ONTOLOGY_ABBREVIATION + ":Smartphone .\n\n" +

            "_:location a sf:Point .";


    private static String TURTLE_TEMPLATE_WITH_OBSERVATION =
            "@prefix geo: <http://www.opengis.net/ont/geosparql#> .\n" +
            "@prefix ssn: <http://purl.oclc.org/NET/ssnx/ssn#> .\n" +
            "@prefix sf: <http://www.opengis.net/ont/sf#> .\n" +
            "@prefix " + SENSORS_ONTOLOGY_ABBREVIATION + ": <" + SENSORS_ONTOLOGY_NAMESPACE + "> .\n" +
            "@prefix dul: <http://www.loa-cnr.it/ontologies/DUL.owl#> .\n\n" +

            "_:sensor a " + SENSORS_ONTOLOGY_ABBREVIATION + ":" + RDF_SENSOR_TYPE + ";\n\t" +
                "ssn:onPlatform  _:phone ;\n\t" +
                "ssn:madeObservation _:observation .\n\n" +

            "_:observation  a ssn:Observation ;\n\t" +
                "ssn:featureOfInterest  _:phone ;\n\t" +
                "ssn:observedProperty  dul:hasLocation ;\n\t" +
                "ssn:observationResult  _:result .\n\n" +

            "_:result a ssn:SensorOutput ;\n\t" +
                "ssn:hasValue _:location .\n\n" +

            "_:phone dul:hasLocation _:location ;\n\t" +
                "a " + SENSORS_ONTOLOGY_ABBREVIATION + ":Smartphone .\n\n" +

            "_:location a sf:Point ;\n\t" +
                "geo:asWKT \"<http://www.opengis.net/def/crs/OGC/1.3/CRS84>POINT(%.10f %.10f)\"^^geo:wktLiteral .";



    public LocationResource(String uriPath, LocationValue initialStatus, ScheduledExecutorService executor) {
        super(uriPath, initialStatus, executor);
    }

    @Override
    public String getPlainObservedPropertyName() {
        return null;
    }

    @Override
    public String getRDFSensorType() {
        return "PositionSensor";
    }

    @Override
    public String getRDFObservedProperty() {
        return null;
    }

    @Override
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteEndpoint) {
        return true;
    }

    @Override
    public byte[] getEtag(long contentFormat) {
        return new byte[1];
    }

    @Override
    public void updateEtag(SensorValue<Void> resourceStatus) {

    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {

        double lat = this.getResourceStatus().getLatitude();
        double lon = this.getResourceStatus().getLongitude();

        if(contentFormat == ContentFormat.APP_TURTLE || contentFormat == ContentFormat.APP_N3){

            if(lat == Double.POSITIVE_INFINITY){
               return TURTLE_STRING_WITHOUT_OBSERVATION.getBytes(CoapMessage.CHARSET);
            }
            else{
                return String.format(Locale.ENGLISH, TURTLE_TEMPLATE_WITH_OBSERVATION,
                        lat, lon).getBytes(CoapMessage.CHARSET);
            }
        }

        return String.format(Locale.ENGLISH, PLAIN_TEXT_TEMPLATE, lat, lon).getBytes(CoapMessage.CHARSET);

    }
}
