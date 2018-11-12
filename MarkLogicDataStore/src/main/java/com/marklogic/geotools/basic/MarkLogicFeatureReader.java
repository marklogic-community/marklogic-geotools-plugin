package com.marklogic.geotools.basic;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.admin.QueryOptionsManager;
import com.marklogic.client.document.DocumentManager;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawCombinedQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentState;

import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.geotools.geojson.feature.FeatureJSON;

public class MarkLogicFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

	  private static final Logger LOGGER = Logging.getLogger(MarkLogicFeatureReader.class);
	/** State used when reading file */
    protected ContentState state;
    
    protected DatabaseClient client;
    
    /** The next feature */
    private SimpleFeature next;
    
    /** The query that was submitted */
    private QueryDefinition query;
    
    private long index = 1;
    private long pageLength = 20;
    private DocumentPage currentPage = null;
    
    private DocumentManager docMgr;
    
    MarkLogicFeatureReader(ContentState contentState, Query query, String mlQuery) throws IOException {
        this.state = contentState;
			  LOGGER.log(Level.INFO, () -> "FeatureReader Query:\n" + query.toString());
        
        MarkLogicDataStore ml = (MarkLogicDataStore) contentState.getEntry().getDataStore();
        client = ml.getClient(); // this may throw an IOException if it could not connect
        docMgr = client.newJSONDocumentManager();
        docMgr.setReadTransform(new ServerTransform("geoJSONTransform"));
        
        docMgr.setPageLength(pageLength);
        String options = 
        	"<options xmlns=\"http://marklogic.com/appservices/search\">" +
  		      "<return-results>true</return-results>" +
        	  "<transform-results apply='raw' />" +
  		    "</options>";
        QueryManager queryMgr = client.newQueryManager();

        this.query = createMarkLogicQueryDefinition(query, queryMgr, options, mlQuery);
    }
    
    private QueryDefinition createMarkLogicQueryDefinition(Query query, QueryManager queryMgr, String options, String definingQuery) {
    	StringHandle rawHandle =
    		    new StringHandle("{\"search\":{\"ctsquery\":" + definingQuery + "}}").withFormat(Format.JSON);
			LOGGER.log(Level.INFO, () -> "rawHandle:\n" + rawHandle.get());
    	return queryMgr.newRawCombinedQueryDefinition(rawHandle);
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
		LOGGER.log(Level.FINE, () -> "next(): successfully read feature, about to return it");
		return feature;
	}

	@Override
	public boolean hasNext() throws IOException {
		if (next != null) {
			  return true;
    } else {
        next = readFeature(); // read next feature so we can check
			  LOGGER.log(Level.FINE, () -> "hasNext(): set next to readFeature(), returning");
        return next != null;
    }
	}

	private void readNextPage() {
		InputStreamHandle handle = new InputStreamHandle();
		LOGGER.log(Level.FINE, () -> "readNextPage(): query: " + query.toString());
		currentPage = docMgr.search(query, index, handle);
	}
	
	private SimpleFeature readNextFeature() throws IOException {
		InputStreamHandle h = new InputStreamHandle();
		currentPage.nextContent(h);
		FeatureJSON fj = new FeatureJSON();
		InputStream s = h.get();
		
		LOGGER.log(Level.FINE, () -> "about to read stream into feature:");
		SimpleFeature feature = fj.readFeature(s);
		LOGGER.log(Level.FINE, () -> "successfully read stream into feature");
		index++;
		return feature;
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
    else if (currentPage.hasNextPage()) {
    	readNextPage();
    	return readNextFeature();
    }
    else {
    	close();
    	return null;
    }
  }
    	
	@Override
	public void close() throws IOException {
		currentPage.close();
	}

}
