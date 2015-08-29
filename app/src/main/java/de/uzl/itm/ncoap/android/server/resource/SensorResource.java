package de.uzl.itm.ncoap.android.server.resource;

import com.google.common.util.concurrent.SettableFuture;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import de.uzl.itm.ncoap.application.server.webresource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.webresource.WrappedResourceStatus;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.options.ContentFormat;

/**
 * Created by olli on 18.05.15.
 */
public abstract class SensorResource<V, T extends SensorValue<V>> extends ObservableWebresource<SensorValue<V>> {

    public static long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

    private static HashMap<Long, String> CONTENT_TEMPLATES = new HashMap<>();

    private static String PLAIN_TEXT_TEMPLATE =
            "Value of \"%s\" at latitude %.10f and longitude %.10f is %d .";

    private static String TURTLE_TEMPLATE =
            "@prefix geo: <http://www.opengis.net/ont/geosparql#> .\n" +
            "@prefix ssn: <http://purl.oclc.org/NET/ssnx/ssn#> .\n" +
            "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
            "@prefix sf: <http://www.opengis.net/ont/sf#> .\n" +

            "_:sensor a ssn:Sensor ;\n\t" +
                "dul:hasLocation _:location ;\n\t" +
                "ssn:madeObservation _:observation .\n\n" +

            "_:location a sf:Point ;\n\t" +
                "geo:asWKT \"<http://www.opengis.net/def/crs/OGC/1.3/CRS84>POINT(%.10f %.10f)\"ˆˆgeo:wktLiteral .\n\n" +

            "_:observation  a ssn:Observation ;\n\t" +
                "ssn:featureOfInterest  _:location ;\n\t" +
                "ssn:observedProperty  %s ;\n\t" +
                "ssn:observationResult  _:result .\n\n" +

            "_:result a ssn:SensorOutput ;\n\t" +
                "ssn:hasValue %s .\n\n" +

            "_:location %s %s .";


    static{
        CONTENT_TEMPLATES.put(ContentFormat.TEXT_PLAIN_UTF8, PLAIN_TEXT_TEMPLATE);
        CONTENT_TEMPLATES.put(ContentFormat.APP_TURTLE, TURTLE_TEMPLATE);
        CONTENT_TEMPLATES.put(ContentFormat.APP_N3, TURTLE_TEMPLATE);
    }

    private byte[] etag;

    protected SensorResource(String uriPath, T initialStatus, ScheduledExecutorService executor) {
        super(uriPath, initialStatus, executor);
    }

    /**
     * The plain text name of the property observed by this sensor, e.g. "Ambient Noise"
     * @return the plain text name of the property observed by this sensor
     */
    public abstract String getPlainObservedPropertyName();

//    /**
//     * The RDF name of the sensor resource (either a URI or a blank node, e.g. "_:mysensor")
//     * @return the RDF name of the sensor resource (either a URI or a blank node, e.g. "_:mysensor")
//     */
//    public abstract String getRDFSensorName();

    /**
     * The URI of the observed property
     * @return The URI of the observed property
     */
    public abstract String getRDFObservedProperty();


    @Override
    public byte[] getEtag(long contentFormat){
        return this.etag;
    }

    protected void setEtag(byte[] etag){
        this.etag = etag;
    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        String template = CONTENT_TEMPLATES.get(contentFormat);
        if(template == null){
            return null;
        }

        double lat = getStatus().getLatitude();
        double lon = getStatus().getLongitude();
        V value = getStatus().getValue();

        if(contentFormat == ContentFormat.APP_TURTLE || contentFormat == ContentFormat.APP_N3){
            return String.format(Locale.ENGLISH, template, lat, lon, getRDFObservedProperty(),
                    value, getRDFObservedProperty(), value).getBytes(CoapMessage.CHARSET);
        }

        return String.format(Locale.ENGLISH, template, getPlainObservedPropertyName(), lat, lon,
                value).getBytes(CoapMessage.CHARSET);

    }


    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest, InetSocketAddress remoteEndpoint) throws Exception {
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
            coapResponse = new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.CONTENT_205);
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
            coapResponse = new CoapResponse(coapRequest.getMessageTypeName(), MessageCode.Name.NOT_ACCEPTABLE_406);

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
