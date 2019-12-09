package com.marklogic.geotools.basic;

import java.io.IOException;
import java.io.StringReader;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentState;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
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
    private long pageLength = 20;
    private int maxFeatures;
    private int featuresRead = 0;
    private JsonNode currentResponse;
    private FeatureCollection<?, ?> currentFeatureCollection;
    private FeatureIterator<?> currentPage = null;
    
    
    MarkLogicFeatureReader(ContentState contentState, Query query, String serviceName, int layerId) throws IOException {
        this.state = contentState;
			  LOGGER.log(Level.INFO, () -> "FeatureReader Query:\n" + query.toString());
        
	    this.serviceName = serviceName;
	    this.layerId = layerId;
	    this.sqlQuery = "1=1"; //translate the Query above into sql
	    if (query.isMaxFeaturesUnlimited()) {
	    	this.maxFeatures = Integer.MAX_VALUE;
	    }
	    else {
	    	this.maxFeatures = query.getMaxFeatures();
	    }
	    if (query.getStartIndex() != null) {
	    	this.index = query.getStartIndex();
	    }
	    
        MarkLogicDataStore ml = (MarkLogicDataStore) contentState.getEntry().getDataStore();
        geoQueryServices = ml.getGeoQueryServiceManager(); // this may throw an IOException if it could not connect
    }
    
    
	@Override
	public SimpleFeatureType getFeatureType() {
		return state.getFeatureType();
	}

	
	@Override
	public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
		SimpleFeature feature;
		if (next != null) {
				feature = next;
				next = null;
		} else {
				feature = readFeature();
		}
		LOGGER.log(Level.INFO, () -> "next(): successfully read feature, about to return it");
		return feature;
	}

	@Override
	public boolean hasNext() throws IOException {
		if (next != null && featuresRead <= maxFeatures) {
			  return true;
    } else {
        next = readFeature(); // read next feature so we can check
			  LOGGER.log(Level.INFO, () -> "hasNext(): set next to readFeature(), returning");
        return next != null;
    }
	}

	private void readNextPage() throws IOException {
		LOGGER.log(Level.INFO, () -> "readNextPage(): serviceName: " + serviceName + "\n"
				+ "layerId: " + layerId + "\n"
				+ "sqlQuery: " + sqlQuery + "\n"
				+ "index: " + index + "\n"
				+ "pageLength: " + pageLength
				);
		
		try {
			currentResponse = geoQueryServices.getFeatures(
					serviceName,
					layerId,
					"1=1",
					index,
					pageLength);
			
			StringReader reader = new StringReader(currentResponse.toString());
			FeatureJSON fj = new FeatureJSON();
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
