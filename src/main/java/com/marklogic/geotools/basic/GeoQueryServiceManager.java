package com.marklogic.geotools.basic;

import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.util.RequestParameters;
import com.marklogic.client.extensions.ResourceServices;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.io.JacksonHandle;

import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

public class GeoQueryServiceManager extends ResourceManager {
    private static final Logger LOGGER = Logging.getLogger(GeoQueryServiceManager.class);

    static final public String NAME="geoQueryService";
    public GeoQueryServiceManager(DatabaseClient client) {
        super();

       // Initialize the Resource Manager via the Database Client
      client.init(NAME, this);
   }

   public List<Name> getLayerNames() throws Exception {
        return getLayerNames(null);
   }
    
   public List<Name> getLayerNames(String namespaceURI) throws Exception {
    ResourceServices services = getServices();
    RequestParameters params = new RequestParameters();
    params.add("service", NAME);

    String serviceParams = "{\"geoserver\": {\"method\":\"getLayerNames\"}}";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode json = objectMapper.readTree(serviceParams);
    
    JacksonHandle resultHandle = services.post(params,new JacksonHandle(json), new JacksonHandle());
    JsonNode result = resultHandle.get();
    JsonNode layerResult = result.get("layerNames");

    //handle old and new output formats from gds
    if (layerResult == null) {
        layerResult = result;
    }

    ArrayList<Name> layerNames = new ArrayList<Name>();
    
    Iterator<JsonNode> elements = layerResult.elements();
    while (elements.hasNext()) {
        JsonNode node = elements.next();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(node.toString());
        }
        Name n = new NameImpl(namespaceURI, node.asText());

        layerNames.add(n);
    }
    return layerNames;
   }
   
   public JsonNode getLayerSchema(String layerName) throws Exception {
	    ResourceServices services = getServices();
	    RequestParameters params = new RequestParameters();
	    params.add("service", NAME);

	    String serviceParams = "{\"geoserver\": {\"method\":\"getLayerSchema\", \"layerName\":\"" + layerName + "\"}}";
	    ObjectMapper objectMapper = new ObjectMapper();
	    JsonNode json = objectMapper.readTree(serviceParams);
	    
	    JacksonHandle resultHandle = services.post(params,new JacksonHandle(json), new JacksonHandle());
	    JsonNode result = resultHandle.get();
	    if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(result.toString());
        }
	    return result;
   }
   
   public int getLayerFeatureCount(String serviceName, int layerId) throws Exception {
	   	ResourceServices services = getServices();
	    RequestParameters params = new RequestParameters();
	    params.add("service", NAME);

	    String serviceParams = "{\"params\": {\"method\":\"query\", \"id\":\"" + serviceName + "\", \"layer\":" + layerId + "}, \"query\":{\"returnCountOnly\":\"true\"}}";
	    ObjectMapper objectMapper = new ObjectMapper();	    
	    JsonNode json = objectMapper.readTree(serviceParams);
	    
	    JacksonHandle resultHandle = services.post(params,new JacksonHandle(json), new JacksonHandle());
	    JsonNode result = resultHandle.get();
	    if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Count result:" + result.toString());
        }
	    return result.get("count").asInt();
   }
   
   public JsonNode getFeatures(JsonNode serviceParams) throws Exception {

	   ResourceServices services = getServices();
	   RequestParameters params = new RequestParameters();
	   params.add("service", NAME);

	   JacksonHandle resultHandle = services.post(params,new JacksonHandle(serviceParams), new JacksonHandle());
	   JsonNode result = resultHandle.get();

	   if (LOGGER.isLoggable(Level.FINE)) {
           LOGGER.fine("getFeatures result:");
           LOGGER.fine(result.toString());
       }
	   return result;	    
   }
}