package de.uzl.itm.ncoap.android.server.resource;

import com.google.common.util.concurrent.SettableFuture;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import de.uzl.itm.ncoap.application.linkformat.LinkParam;
import de.uzl.itm.ncoap.application.server.resource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.options.ContentFormat;

import static de.uzl.itm.ncoap.message.options.ContentFormat.APP_RDF_XML;
import static de.uzl.itm.ncoap.message.options.ContentFormat.APP_TURTLE;
import static de.uzl.itm.ncoap.message.options.ContentFormat.TEXT_PLAIN_UTF8;

/**
 * Created by olli on 18.05.15.
 */
public abstract class SensorResource<V, T extends SensorValue<V>> extends ObservableWebresource<SensorValue<V>> {

    public static final long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

    public static final String SENSORS_ONTOLOGY_NAMESPACE =
            "http://example.org/";

    public static final String SENSORS_ONTOLOGY_ABBREVIATION =
            "exp";


    private static String PLAIN_TEXT_TEMPLATE =
            "Value of \"%s\" at latitude %.10f and longitude %.10f is %d .";


    private static String TURTLE_XSD_PREFIX =
            "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";

    private static String TURTLE_TEMPLATE =
            "@prefix geo: <http://www.opengis.net/ont/geosparql#> .\n" +
            "@prefix ssn: <http://purl.oclc.org/NET/ssnx/ssn#> .\n" +
            "@prefix sf: <http://www.opengis.net/ont/sf#> .\n" +
            "@prefix " + SENSORS_ONTOLOGY_ABBREVIATION + ": <" + SENSORS_ONTOLOGY_NAMESPACE + "> .\n" +
            "@prefix dul: <http://www.loa-cnr.it/ontologies/DUL.owl#> .\n\n" +

            "_:sensor a " + SENSORS_ONTOLOGY_ABBREVIATION + ":%s ;\n\t" +
                "dul:hasLocation _:location ;\n\t" +
                "ssn:madeObservation _:observation .\n\n" +

            "_:observation  a ssn:Observation ;\n\t" +
                "ssn:featureOfInterest  _:location ;\n\t" +
                "ssn:observedProperty  " + SENSORS_ONTOLOGY_ABBREVIATION + ":%s ;\n\t" +
                "ssn:observationResult  _:result .\n\n" +

            "_:result a ssn:SensorOutput ;\n\t" +
                "ssn:hasValue %s .";

    private static String TURTLE_UNKNOWN_LOCATION_TEMPLATE =
            "_:location a sf:Point ;\n\t" +
                SENSORS_ONTOLOGY_ABBREVIATION + ":%s %s .";

    private static String TURTLE_LOCATION_TEMPLATE_WITH_COORDS =
            "_:location a sf:Point ;\n\t" +
                SENSORS_ONTOLOGY_ABBREVIATION + ":%s %s ;\n\t" +
                "geo:asWKT \"<http://www.opengis.net/def/crs/OGC/1.3/CRS84>POINT(%.10f %.10f)\"^^geo:wktLiteral .";


    private byte[] etag;

    protected SensorResource(String uriPath, T initialStatus, ScheduledExecutorService executor) {
        super(uriPath, initialStatus, executor);

        String contentTypes = "\"" + TEXT_PLAIN_UTF8 + " " + APP_RDF_XML + " " + APP_TURTLE + "\"";
        this.setLinkParam(LinkParam.createLinkParam(LinkParam.Key.CT,  contentTypes));
    }

    /**
     * The plain text name of the property observed by this sensor, e.g. "Ambient Noise"
     * @return the plain text name of the property observed by this sensor
     */
    public abstract String getPlainObservedPropertyName();

    public abstract String getRDFSensorType();


    /**
     * The URI of the observed property
     * @return The URI of the observed property
     */
    public abstract String getRDFObservedProperty();

    @Override
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteEndpoint) {
        return true;
    }

    @Override
    public byte[] getEtag(long contentFormat){
        return this.etag;
    }

    protected void setEtag(byte[] etag){
        this.etag = etag;
    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {

        SensorValue sensorValue = this.getResourceStatus();
        double lat = sensorValue.getLatitude();
        double lon = sensorValue.getLongitude();
        V plainValue = (V) sensorValue.getValue();

        if (contentFormat == ContentFormat.APP_TURTLE || contentFormat == ContentFormat.APP_N3){
            String xsdType = sensorValue.getXsdType() == null ? "" : sensorValue.getXsdType();
            String typedValue = "\"" + plainValue + "\"^^" + xsdType;
            String observedProperty = getRDFObservedProperty();
            String sensorType = getRDFSensorType();

            String turtle = sensorValue.getXsdType() == null ? "" : TURTLE_XSD_PREFIX;
            turtle += String.format(Locale.ENGLISH, TURTLE_TEMPLATE, sensorType, observedProperty,
                    typedValue) + "\n\n";

            if(lat == Double.POSITIVE_INFINITY){
                turtle += String.format(Locale.ENGLISH, TURTLE_UNKNOWN_LOCATION_TEMPLATE,
                        observedProperty, typedValue);
            }
            else{
                turtle += String.format(Locale.ENGLISH, TURTLE_LOCATION_TEMPLATE_WITH_COORDS,
                        observedProperty, typedValue, lat, lon);
            }

            return turtle.getBytes(CoapMessage.CHARSET);
        }

        return String.format(Locale.ENGLISH, PLAIN_TEXT_TEMPLATE, getPlainObservedPropertyName(),
                lat, lon, plainValue).getBytes(CoapMessage.CHARSET);

    }


    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
           InetSocketAddress remoteEndpoint) throws Exception {

        Set<Long> acceptedFormats = coapRequest.getAcceptedContentFormats();

        //If accept option is not set in the request, use the default (TEXT_PLAIN_UTF8)
        if(acceptedFormats.isEmpty())
            acceptedFormats.add(DEFAULT_CONTENT_FORMAT);

        //Generate the payload of the response (depends on the accepted content formats, resp. the default
        WrappedResourceStatus resourceStatus = null;
        Iterator<Long> iterator = acceptedFormats.iterator();

        long contentFormat = DEFAULT_CONTENT_FORMAT;

        while(resourceStatus == null && iterator.hasNext()){
            contentFormat = iterator.next();
            resourceStatus = getWrappedResourceStatus(contentFormat);
        }

        //generate the CoAP response
        CoapResponse coapResponse;

        //if the payload could be generated, i.e. at least one of the accepted content formats (according to the
        //requests accept option(s)) is offered by the Webservice then set payload and content format option
        //accordingly
        if(resourceStatus != null){
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTENT_205);
            coapResponse.setContent(resourceStatus.getContent(), contentFormat);

            coapResponse.setEtag(resourceStatus.getEtag());
            coapResponse.setMaxAge(resourceStatus.getMaxAge());

            if(coapRequest.getObserve() == 0)
                coapResponse.setObserve();
        }

        //if no payload could be generated, i.e. none of the accepted content formats (according to the
        //requests accept option(s)) is offered by the Webservice then set the code of the response to
        //400 BAD REQUEST and set a payload with a proper explanation
        else{
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.NOT_ACCEPTABLE_406);

            StringBuilder payload = new StringBuilder();
            payload.append("Requested content format(s) (from requests ACCEPT option) not available: ");
            for(long acceptedContentFormat : coapRequest.getAcceptedContentFormats())
                payload.append("[").append(acceptedContentFormat).append("]");

            coapResponse.setContent(payload.toString()
                    .getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        }

        //Set the response future with the previously generated CoAP response
        responseFuture.set(coapResponse);
    }
}
