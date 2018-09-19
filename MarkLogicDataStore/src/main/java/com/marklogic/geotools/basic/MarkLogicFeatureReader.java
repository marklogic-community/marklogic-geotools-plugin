package com.marklogic.geotools.basic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import com.marklogic.client.DatabaseClient;
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
import com.marklogic.client.document.DocumentRecord;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentState;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.geotools.geojson.feature.FeatureJSON;

public class MarkLogicFeatureReader implements FeatureReader<SimpleFeatureType, SimpleFeature> {

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
    
    private JSONDocumentManager docMgr;
    
    MarkLogicFeatureReader(ContentState contentState, Query query, String mlQuery) throws IOException {
        this.state = contentState;
        System.out.println("FeatureReader Query:\n" + query.toString());
        
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
    	StructuredQueryBuilder b = queryMgr.newStructuredQueryBuilder();
    	
    	StringHandle rawHandle = 
    		    new StringHandle("{\"search\":{\"ctsquery\":" + definingQuery + "}}").withFormat(Format.JSON);
    	System.out.println("rawHandle:\n" + rawHandle.get());
    	RawCombinedQueryDefinition querydef =
    		    queryMgr.newRawCombinedQueryDefinition(rawHandle);
    	
    	return querydef;
    }
    
	@Override
	public SimpleFeatureType getFeatureType() {
		return (SimpleFeatureType) state.getFeatureType();
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
//        System.out.println("next(): successfully read feature, about to return it");
        return feature;
	}

	@Override
	public boolean hasNext() throws IOException {
		if (next != null) {
            return true;
        } else {
            next = readFeature(); // read next feature so we can check
//            System.out.println("hasNext(): set next to readFeature(), returning");
            return next != null;
        }
	}

	private void readNextPage() {
		InputStreamHandle handle = new InputStreamHandle();
		System.out.println("readNextPage(): query: " + query.toString());
		currentPage = docMgr.search(query, index, handle);
	}
	
	private SimpleFeature readNextFeature() throws IOException {
		InputStreamHandle h = new InputStreamHandle();
		currentPage.nextContent(h);
		FeatureJSON fj = new FeatureJSON();
		InputStream s = h.get();
		
//		System.out.println("about to read stream into feature:");
		SimpleFeature feature = fj.readFeature(s);
//		System.out.println("successfully read stream into feature");
		index++;
		return feature;
	}
	
	
	 /** Read a GeoJSON doc from MarkLogic and parse into a SimpleFeature */
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
		// TODO Auto-generated method stub

	}

}
