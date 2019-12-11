package com.marklogic.geotools.basic;

import java.io.IOException;
import java.io.StringReader;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentState;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.geotools.geojson.feature.FeatureJSON;

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
    private long index = 1;
    private long pageLength = 200;
    private int maxFeatures;
    private int featuresRead = 0;
    private Query query;
    private ObjectNode readFeatureRequestParams;
    private JsonNode currentResponse;
    private FeatureCollection<?, ?> currentFeatureCollection;
    private FeatureIterator<?> currentPage = null;

	JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    
    
    MarkLogicFeatureReader(ContentState contentState, Query query, String serviceName, int layerId) throws IOException {
        this.state = contentState;
			  LOGGER.log(Level.INFO, () -> "FeatureReader Query:\n" + query.toString());
			  LOGGER.log(Level.INFO, () -> "FeatureReader Query:\n" + query.getSortBy()[0].toString());
        
	    this.serviceName = serviceName;
	    this.layerId = layerId;
	    this.sqlQuery = "1=1"; //translate the Query.getFilter() above into sql
	    this.query = query;
	    
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
	    
        MarkLogicDataStore ml = (MarkLogicDataStore) contentState.getEntry().getDataStore();
        geoQueryServices = ml.getGeoQueryServiceManager(); // this may throw an IOException if it could not connect
    }
    
    private void generateFilterRequestParams(ObjectNode queryProperty) {
    	
    	queryProperty.set("where", nodeFactory.textNode("1=1"));
    };
    
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
    		String[] propertyNames = query.getPropertyNames();
    		String outFields = "";
    		for (int i = 0; i < propertyNames.length; i++) {
    			if (i > 0) outFields += ", ";
    			String propName = propertyNames[i];
    			outFields += propName;
    		}
    		queryProperty.set("outFields", nodeFactory.textNode(outFields));
    	}
    	
    	//sort
    	SortBy[] sortSpecs = query.getSortBy();
    	if (sortSpecs.length > 0 ) {
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

	
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		SimpleFeature feature = readFeature();
		LOGGER.log(Level.INFO, () -> "next(): successfully read feature, about to return it");
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
			
			StringReader reader = new StringReader(currentResponse.toString());
			FeatureJSON fj = new FeatureJSON();
			fj.setFeatureType(getFeatureType());
			currentFeatureCollection = fj.readFeatureCollection(reader);
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
	 * @return
	 * @throws IOException
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
		currentPage.close();
	}

}
