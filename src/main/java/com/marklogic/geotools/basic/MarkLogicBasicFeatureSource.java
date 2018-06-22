package com.marklogic.geotools.basic;

import java.io.IOException;
import java.util.Date;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import java.io.IOException;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.marker.JSONReadHandle;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawCombinedQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.fasterxml.jackson.databind.JsonNode;

public class MarkLogicBasicFeatureSource extends ContentFeatureSource {

	public MarkLogicBasicFeatureSource(ContentEntry entry, Query query) {
		super(entry, query);
		// TODO Auto-generated constructor stub
	}
	
	
	public MarkLogicDataStore getDataStore() {
		return (MarkLogicDataStore) super.getDataStore();
	}
	
	//make this a UDF later, for now just return the whole earth
	@Override
	protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
		// TODO Auto-generated method stub
		return new ReferencedEnvelope(-180.0, 180.0, -90.0, 90.0, DefaultGeographicCRS.WGS84);
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		if (query.getFilter() == Filter.INCLUDE) {
            DatabaseClient client = getDataStore().getClient();
            try {
            	QueryManager qm = client.newQueryManager();
            	String XML_OPTIONS = 
            		    "<options xmlns=\"http://marklogic.com/appservices/search\">" +
            		      "<return-results>false</return-results>" +
            		    "</options>";
            	QueryDefinition queryDef = getDataStore().createMarkLogicQueryDefinition(query, qm, XML_OPTIONS);
            	SearchHandle resultsHandle = new SearchHandle();
                // run the search
                qm.search(queryDef, resultsHandle);
                int count = (int) resultsHandle.getTotalResults();
                return count;
            } finally {
            	
            }
        }
        return -1; // feature by feature scan required to count records
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		// TODO Auto-generated method stub
		return new MarkLogicFeatureReader(getState(), query);
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(entry.getName());

        JSONDocumentManager docMgr = getDataStore().getClient().newJSONDocumentManager();
        JacksonHandle handle = new JacksonHandle();
        docMgr.read("geojson_typeDescriptor.json", handle);
        JsonNode json = handle.get();
        JsonNode schema = json.get("schema");
        
        for (JsonNode n : schema) {
        	String name = n.get("name").asText();
        	String type = n.get("type").asText();
        	if (type.contentEquals("geometry")) {
        		String geoType = n.get("geometryType").asText();
        		if (geoType.contentEquals("point")) { 
        			builder.add(name, Point.class);
        		}
        		else if (geoType.contentEquals("linestring")) {
        			builder.add(name, LineString.class);
        		}
        		else if (geoType.contentEquals("polygon")) {
        			builder.add(name, Polygon.class);
        		}
        	}
        	else if (type.contentEquals("string")) {
        		builder.add(name, String.class);
        	}
        	else if (type.contentEquals("int")) {
        		builder.add(name, Integer.class);
        	}
        	else if (type.contentEquals("float")) {
        		builder.add(name, Float.class);
        	}
        	else if (type.contentEquals("double")) {
        		builder.add(name, Double.class);
        	}
        	else if (type.contentEquals("boolean")) {
        		builder.add(name, Boolean.class);
        	}
        	else if (type.contentEquals("dateTime")) {
        		builder.add(name, Date.class);
        	}
        	else
        		builder.add(name, String.class);
        }
        // build the type (it is immutable and cannot be modified)
        final SimpleFeatureType SCHEMA = builder.buildFeatureType();
        return SCHEMA;
	}

}
