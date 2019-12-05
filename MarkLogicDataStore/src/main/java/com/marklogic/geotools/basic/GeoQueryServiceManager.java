package com.marklogic.geotools.basic;

import com.marklogic.client.extensions.ResourceManager;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.util.RequestParameters;
import com.marklogic.client.extensions.ResourceServices;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.client.io.JacksonHandle;

import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

public class GeoQueryServiceManager extends ResourceManager {

    static final public String NAME="geoQueryService";
    public GeoQueryServiceManager(DatabaseClient client) {
        super();

       // Initialize the Resource Manager via the Database Client
      client.init(NAME, this);
   }

   public List<Name> getLayerNames() throws Exception {
    ResourceServices services = getServices();
    RequestParameters params = new RequestParameters();
    params.add("service", NAME);

    String serviceParams = "{\"geoserver\": {\"method\":\"getLayerNames\"}}";
    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode json = objectMapper.readTree(serviceParams);
    JacksonHandle resultHandle = services.post(params,new JacksonHandle(json), new JacksonHandle());
    JsonNode result = resultHandle.get();
    ArrayList<Name> layerNames = new ArrayList<Name>();
    
    Iterator<JsonNode> elements = result.elements();
    while (elements.hasNext()) {
        JsonNode node = elements.next();
        System.out.println(node.toString());
        Name n = new NameImpl(null, node.asText());

        layerNames.add(n);
    }
    return layerNames;
   }
   
   public JsonNode getLayerSchema() throws Exception {
	    ResourceServices services = getServices();
	    RequestParameters params = new RequestParameters();
	    params.add("service", NAME);

	    String serviceParams = "{\"geoserver\": {\"method\":\"getLayerSchema\"}}";
	    ObjectMapper objectMapper = new ObjectMapper();
	    
	    JsonNode json = objectMapper.readTree(serviceParams);
	    JacksonHandle resultHandle = services.post(params,new JacksonHandle(json), new JacksonHandle());
	    JsonNode result = resultHandle.get();
	    return result;
   }
}