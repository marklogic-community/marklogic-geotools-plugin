package com.marklogic.geotools.basic;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentState;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.geotools.geometry.jts.WKTReader2;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class MarkLogicFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	  private static final Logger LOGGER = Logging.getLogger(MarkLogicFeatureReader.class);
	/** State used when reading file */
    protected ContentState state;
    
    protected GeoQueryServiceManager geoQueryServices;
    
    private String serviceName;
    private int layerId;
    /** The next feature */
    private SimpleFeature next;
    
    private String sqlQuery;
    private long index = 0;
    private long pageLength = 200;
    private int maxFeatures;
    private int featuresRead = 0;
    private Query query;
    private ObjectNode readFeatureRequestParams;
    private JsonNode currentResponse;
    private FeatureCollection<?, ?> currentFeatureCollection;
    private FeatureIterator<?> currentPage = null;
    private SimpleFeatureBuilder featureBuilder;
    private String idField;
    private String geometryColumn;
    private WKTReader2 wktReader = new WKTReader2();

	JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    
    
    MarkLogicFeatureReader(ContentState contentState, Query query, String serviceName, int layerId, String idField, String geometryColumn) throws IOException {
        this.state = contentState;
			if (query != null) {
			  LOGGER.log(Level.INFO, () -> "FeatureReader Query:\n" + query.toString());
			  if (query.getSortBy() != null && query.getSortBy().length > 0) {
			  	LOGGER.log(Level.INFO, () -> "FeatureReader Query:\n" + query.getSortBy()[0].toString());
			  }
			}
        
	    this.serviceName = serviceName;
	    this.layerId = layerId;
	    this.sqlQuery = "1=1"; //translate the Query.getFilter() above into sql
	    this.query = query;
	    this.idField = idField;
	    this.geometryColumn = geometryColumn;
	    
	    if (query.isMaxFeaturesUnlimited()) {
	    	this.maxFeatures = Integer.MAX_VALUE;
	    }
	    else {
	    	this.maxFeatures = query.getMaxFeatures();
	    }
	    if (query.getStartIndex() != null) {
	    	this.index = query.getStartIndex();
	    }
	    generateReadFeatureRequestParams();

	    // Need Support for checking authentication mechanism here
		// If using "custom authentication" need to in some manner pick up the class they specify
		// and create the authentication using whatever the current Spring Security Context says
		// The below line will go get the current Principal String from Spring Security
		//
		//String user = getLogin();

		geoQueryServices = getGeoQueryServices(contentState);
		featureBuilder = new SimpleFeatureBuilder(state.getFeatureType());
    }
    
    private GeoQueryServiceManager getGeoQueryServices(ContentState cs) {
    	MarkLogicDataStore store = (MarkLogicDataStore) cs.getEntry().getDataStore();

    	MarkLogicDataStore.UserConnectionDetails details = store.getUserConnectionDetails();
    	// using a switch for future separation of authType (Basic, Digest, etc)
    	switch(details.authType != null ? details.authType : "Unknown") {
			case "PreAuthenticatedHeader":
				// Pre Authenticated Headers for GEOAxIS end up Base64 encoded, and are Basic HTTP Spec formatted.
				// This decodes, and returns just the userid, which is all the ML Rewriter needs for pre-authed users.
				try {
					String user = (new String(Base64.getDecoder().decode(getGeoServerLogin().substring(6)))).split(":")[0];
					DatabaseClientFactory.BasicAuthContext sContext = new DatabaseClientFactory.BasicAuthContext(user, "");
					DatabaseClient userClient = DatabaseClientFactory.newClient(details.host, details.port, sContext);
					return new GeoQueryServiceManager(userClient);
				} catch (NullPointerException npe) {
					throw new IllegalStateException("Unable to create database client connection", npe);
				}
			default:
				return store.getGeoQueryServiceManager();
		}
	}

	/**
	 * The getLogin method will retrieve the Spring Security Context and get the principal from it...
	 * if it is undefined, it will return null.  Potentially should throw a security exception instead.
	 */
    private String getGeoServerLogin() {
		Optional<SecurityContext> opt = Optional.ofNullable(SecurityContextHolder.getContext());
		if (opt.isPresent()) {
			Optional<Authentication> optAuth = Optional.ofNullable(opt.get().getAuthentication());
			if (optAuth.isPresent()) {
				return optAuth.get().getPrincipal().toString();
			}
		}
		return null;

	}

    private void generateFilterRequestParams(ObjectNode queryProperty) {
    	
    	LOGGER.log(Level.INFO, () -> "in generateFilterRequestParams(); Query:\n" + query.toString());
    	LOGGER.log(Level.INFO, () -> "in generateFilterRequestParams(); Filter:\n" + query.getFilter().toString());
    	StringWriter writer = new StringWriter();
    	
    	FilterToMarkLogic f2m = new FilterToMarkLogic(writer, queryProperty);
    	Filter f = query.getFilter();
        f.accept(f2m, null);
        String sql = writer.toString();
    	
    	queryProperty.set("where", nodeFactory.textNode(sql));
    }
    
    private void generateReadFeatureRequestParams() {
    	readFeatureRequestParams = nodeFactory.objectNode();
    	
    	ObjectNode paramsProperty = nodeFactory.objectNode();
    	paramsProperty.set("method", nodeFactory.textNode("query"));
    	paramsProperty.set("id", nodeFactory.textNode(serviceName));
    	paramsProperty.set("layer", nodeFactory.numberNode(layerId));
    	readFeatureRequestParams.set("params", paramsProperty);
    	
    	ObjectNode queryProperty = nodeFactory.objectNode();
    	queryProperty.set("resultOffset", nodeFactory.numberNode(index));
    	queryProperty.set("resultRecordCount", nodeFactory.numberNode(pageLength));
    	queryProperty.set("returnGeometry", nodeFactory.booleanNode(true));
    	
    	//do "where" clause TODO:  base on the query.getFilter()!
	    generateFilterRequestParams(queryProperty);
    	
    	//property names
    	if (query.retrieveAllProperties()) {
    		queryProperty.set("outFields", nodeFactory.textNode("*"));
    	}
    	else {
    		ArrayList<String> temp = new ArrayList<String>();
    		for (String prop : query.getPropertyNames()) {
    			if (!prop.equals(geometryColumn)) temp.add(prop);
			}
			if (temp.size() > 0) {
				String[] propertyNames = temp.toArray(new String[temp.size()]);
				
				String outFields = "";
				for (int i = 0; i < propertyNames.length; i++) {
					if (i > 0) outFields += ", ";
					String propName = propertyNames[i];
					outFields += propName;
				}
				queryProperty.set("outFields", nodeFactory.textNode(outFields));
			}
			else {
				queryProperty.set("outFields", nodeFactory.textNode(""));
			}
    	}
    	
    	//sort
    	SortBy[] sortSpecs = query.getSortBy();
    	if (sortSpecs != null && sortSpecs.length > 0 ) {
    		if (sortSpecs.length == 1 && sortSpecs[0].getPropertyName() == null) {
    			//do nothing
    		}
    		else {
    			String sortByString = "";
    			for (int i = 0; i < sortSpecs.length; i++) {
    				SortBy sortSpec = sortSpecs[i];
    				if (i > 0) sortByString += ", ";
    				PropertyName propertyName = sortSpec.getPropertyName();
    				String name = propertyName.getPropertyName();
    				sortByString += name + " " + (sortSpec.getSortOrder() == SortOrder.ASCENDING ? "ASC" : "DESC");
    			}
    			queryProperty.set("orderByFields", nodeFactory.textNode(sortByString));
    		}
    	}
    	readFeatureRequestParams.set("query", queryProperty);
    }
    
	@Override
	public SimpleFeatureType getFeatureType() {
		return state.getFeatureType();
	}

	private void logFeature(SimpleFeature feature) {
		LOGGER.log(Level.INFO, () -> "FEATURE LOG:");
		LOGGER.log(Level.INFO, () -> "Feature ID: " + feature.getID());
		SimpleFeatureType featureType = feature.getType();
		GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
		
		Collection<PropertyDescriptor> propertyDescriptors = featureType.getDescriptors();
		for (PropertyDescriptor propDesc : propertyDescriptors) {
			Name attrName = propDesc.getName();
			Object attr = feature.getAttribute(attrName);
			
			if (attr != null)
				LOGGER.log(Level.INFO, () -> attrName.toString() + ": " + attr.toString());
			else
				LOGGER.log(Level.INFO, () -> "Could not find property for property descriptor: " + propDesc.toString());
		}
		
	}
	
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		SimpleFeature feature = readFeature();
		LOGGER.log(Level.INFO, () -> "next(): successfully read feature, about to return it");
		logFeature(feature);
		return feature;
	}

	@Override
	public boolean hasNext() throws IOException {
		LOGGER.log(Level.INFO, () -> "hasNext(): entered; currentPage: " + currentPage + "; featuresRead: " + featuresRead + "; maxFeatures: " + maxFeatures );
		if (currentPage == null) {
			readNextPage();
			return currentPage.hasNext();
		}
		else if (featuresRead <= maxFeatures) {
			if (!currentPage.hasNext()) {
				readNextPage();
			}
			return currentPage.hasNext();
		} else
			return false;
		/*
		LOGGER.log(Level.INFO, () -> "hasNext(): entered");
		if (next != null && featuresRead <= maxFeatures) {
			LOGGER.log(Level.INFO, () -> "hasNext(): next != null, featuresRead = " + featuresRead);
			  return true;
    } else {
    	LOGGER.log(Level.INFO, () -> "hasNext(): next: " + next + "; featuresRead: " + featuresRead);
        next = readFeature(); // read next feature so we can check
			  LOGGER.log(Level.INFO, () -> "hasNext(): set next to readFeature(), returning");
        return next != null;
    }
    */
	}

	
	private SimpleFeature parseJsonFeature(JsonNode node) throws Exception {
		LOGGER.log(Level.INFO, () -> "parsing feature...");

		SimpleFeatureType featureType = getFeatureType();
		

		JsonNode properties = node.get("properties");
		
		//get the geometry and assign it to the internal geometry column name.
		//We don't use this as a request back to the server because the
		//backing services understand whether to get the geometry or not based
		//on the "returnGeometry" service parameter
		JsonNode geometryNode = node.get("geometry");
		if (geometryNode != null && !(geometryNode instanceof NullNode)) {
			GeometryJSON gj = new GeometryJSON();
			Geometry readGeo = gj.read(new StringReader(geometryNode.toString()));
		
			featureBuilder.set(geometryColumn, readGeo);
		
			LOGGER.log(Level.FINE, "readGeo:");
			LOGGER.log(Level.FINE, readGeo.toString());
		}
		
		Iterator<String> fieldNames = properties.fieldNames();
		String id = null;
		
		id = node.get("id").asText();
		//featureBuilder.set(idField, id);
		
		while(fieldNames.hasNext()) {
			try {
				String fieldName = fieldNames.next();
				AttributeType attrType = featureType.getType(fieldName);
				if (attrType instanceof GeometryType) {
					Geometry geo = wktReader.read(properties.get(fieldName).asText());
					Object value = Converters.convert(geo, attrType.getBinding());
					featureBuilder.set(fieldName, value);
				}
				else if (fieldName.equals(idField)) {
					JsonNode data = properties.get(fieldName);
					id = data.asText();
					featureBuilder.set(fieldName, id);
				} else {
					JsonNode data = properties.get(fieldName);
					if (data instanceof NullNode) {
						featureBuilder.set(fieldName, null);
						}
					else if (data instanceof BooleanNode) {
						String text = data.asText();
						if (text.equalsIgnoreCase("true")) {
							featureBuilder.set(fieldName, 1);
						}
						else
							featureBuilder.set(fieldName, 0);
						//featureBuilder.set(fieldName, new Boolean(data.asText()));
					}
					else
						featureBuilder.set(fieldName, data.asText());
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
				throw ex;
			}
		}
		return featureBuilder.buildFeature(id);
	}
	private FeatureCollection parseCurrentResponse() {
		DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
		
		ArrayNode features = (ArrayNode)currentResponse.get("features");
		Iterator<JsonNode> iter = features.elements();
		while (iter.hasNext()) {
			try {
				featureCollection.add(parseJsonFeature(iter.next()));
			}
			catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		return featureCollection;
	}
	
	private void readNextPage() throws IOException {
		LOGGER.log(Level.INFO, () -> "readNextPage(): serviceName: " + serviceName + "\n"
				+ "layerId: " + layerId + "\n"
				+ "sqlQuery: " + sqlQuery + "\n"
				+ "index: " + index + "\n"
				+ "pageLength: " + pageLength
				);
		
		try {
			JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
			ObjectNode queryNode = (ObjectNode)readFeatureRequestParams.get("query");
			queryNode.set("resultOffset", nodeFactory.numberNode(index));
			
			currentResponse = geoQueryServices.getFeatures(readFeatureRequestParams);
			
			currentFeatureCollection = parseCurrentResponse();
			/*
			StringReader reader = new StringReader(currentResponse.toString());
			FeatureJSON fj = new FeatureJSON();
			fj.setFeatureType(getFeatureType());
			currentFeatureCollection = fj.readFeatureCollection(reader);
			*/
			currentPage = currentFeatureCollection.features();
			index += pageLength;
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw new IOException(ex);
		}
	}
	
	private SimpleFeature readNextFeature() {
		featuresRead++;
		LOGGER.log(Level.INFO, () -> "readNextFeature(): featuresRead: " + featuresRead);
		return (SimpleFeature)currentPage.next();
	}

	/**
	 * Read a GeoJSON doc from MarkLogic and parse into a SimpleFeature
	 * @return simple feature
	 * @throws IOException when things go sideways
	 */
  SimpleFeature readFeature() throws IOException {
  	if (currentPage == null) { //first time we've accessed the backend store
  		readNextPage();
    }
    	
    if (currentPage.hasNext()) {
    	return readNextFeature();
    }
    else if (currentResponse.get("metadata").get("limitExceeded").asBoolean()) {
    	readNextPage();
    	return readNextFeature();
    }
    else {
    	currentPage.close();
    	return null;
    }
  }
    	
	@Override
	public void close() throws IOException {
  		if (currentPage != null) {
			currentPage.close();
		}
	}

}
