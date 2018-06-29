package com.marklogic.geotools.basic;

import java.io.IOException;
import java.util.Date;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.ValuesHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawCombinedQueryDefinition;
import com.marklogic.client.query.ValuesDefinition;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MarkLogicBasicFeatureSource extends ContentFeatureSource {

	private JsonNode definingQuery;
	private JsonNode dbMetadata;
	
	public MarkLogicBasicFeatureSource(ContentEntry entry, Query query) {
		super(entry, query);
		retrieveDBMetadata(entry, query);
	}
	
	protected void retrieveDBMetadata(ContentEntry entry, Query query) {
		// create a query builder for the query options
//	    StructuredQueryBuilder qb = new StructuredQueryBuilder();
//	    StructuredQueryDefinition querydef = qb.and(
//	    		qb.collection("typeDescriptors"),
//	    		qb.value(qb.jsonProperty("namespace"), entry.getName().getNamespaceURI()),
//	    		qb.value(qb.jsonProperty("typeName"), entry.getName().getLocalPart())
//	    	    );
	    JSONDocumentManager docMgr = getDataStore().getClient().newJSONDocumentManager();
	    JacksonHandle handle = new JacksonHandle();
	    docMgr.read(entry.getName().getNamespaceURI() + "/" + entry.getName().getLocalPart() + ".json", handle);
	    dbMetadata = handle.get();
	    System.out.println("retrieveDBMetadata: dbMetadata: " + dbMetadata.toString());
	    definingQuery = dbMetadata.get("definingQuery");
	}
	
	public MarkLogicDataStore getDataStore() {
		return (MarkLogicDataStore) super.getDataStore();
	}
	
	//make this a UDF later, for now just return values we've poked into each doc
	@Override
	protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
		System.out.println("*******************************************************************");
		System.out.println("in MarkLogicBasicFeatureSource:getBoundsInternal");
		
        DatabaseClient client = getDataStore().getClient();
        try {
        	QueryManager qm = client.newQueryManager();
        	
        	StringHandle rawHandle = 
        		    new StringHandle("{\"search\":{\"query\":" + definingQuery.toString() + "}}").withFormat(Format.JSON);
        	System.out.println("rawHandle:\n" + rawHandle.get());
        	RawCombinedQueryDefinition querydef =
        		    qm.newRawCombinedQueryDefinition(rawHandle);

        	ValuesDefinition vdef = qm.newValuesDefinition("box-west", "geotools");
        	vdef.setQueryDefinition(querydef);
        	vdef.setAggregate("min");
        	
        	ValuesHandle westH = qm.values(vdef, new ValuesHandle());
        	float west = westH.getAggregate("min").get("xs:float", Float.class);
        	
        	vdef = qm.newValuesDefinition("box-east", "geotools");
        	vdef.setQueryDefinition(querydef);
        	vdef.setAggregate("max");
        	ValuesHandle eastH = qm.values(vdef, new ValuesHandle());
        	float east = eastH.getAggregate("max").get("xs:float", Float.class);
        	
        	vdef = qm.newValuesDefinition("box-south", "geotools");
        	vdef.setQueryDefinition(querydef);
        	vdef.setAggregate("min");
        	ValuesHandle southH = qm.values(vdef, new ValuesHandle());
        	float south = southH.getAggregate("min").get("xs:float", Float.class);
        	
        	vdef = qm.newValuesDefinition("box-north", "geotools");
        	vdef.setQueryDefinition(querydef);
        	vdef.setAggregate("max");
        	ValuesHandle northH = qm.values(vdef, new ValuesHandle());
        	float north = northH.getAggregate("max").get("xs:float", Float.class);
        	
        	System.out.println("west: " + west);
        	System.out.println("east: " + east);
        	System.out.println("north: " + north);
        	System.out.println("south: " + south);
        	System.out.println("*******************************************************************");
    		
        	return new ReferencedEnvelope(west, east, south, north, DefaultGeographicCRS.WGS84);
        } 
        catch (Exception ex) {
        	ex.printStackTrace();
        	return null;
        }
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		if (query.getFilter() == Filter.INCLUDE) {
            DatabaseClient client = getDataStore().getClient();
            try {
            	QueryManager qm = client.newQueryManager();
            	String JSON_OPTIONS = "\"options\": {\"return-results\": false}";      	
            	StringHandle rawHandle = 
            		    new StringHandle("{\"search\":{\"ctsquery\":" + definingQuery.toString() + "," + JSON_OPTIONS + "}}").withFormat(Format.JSON);
            	System.out.println("rawHandle:\n" + rawHandle.get());
            	RawCombinedQueryDefinition queryDef =
            		    qm.newRawCombinedQueryDefinition(rawHandle);
            	
            	System.out.println("getCountInternal(): running query\n" + rawHandle.get());
            	SearchHandle resultsHandle = new SearchHandle();
                // run the search
                qm.search(queryDef, resultsHandle);
                System.out.println(resultsHandle.getQueryCriteria().toString());
                int count = (int) resultsHandle.getTotalResults();
                System.out.println("getCountInternal(): query returned " + count + " results");
                return count;
            } 
            catch (Exception ex) {
            	ex.printStackTrace();
            	return -1;
            }
        }
		System.out.println("Query is " + query.toString() + "; feature by feature count required for MarkLogic driver");
        return -1; // feature by feature scan required to count records
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		// TODO Auto-generated method stub
		return new MarkLogicFeatureReader(getState(), query, definingQuery.toString());
	}

	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		if (dbMetadata == null) {
			retrieveDBMetadata(this.entry, this.query);
		}
		
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(entry.getName());
        builder.setSRS( "EPSG:4326" );
        builder.setNamespaceURI(getDataStore().getNamespaceURI());

        JsonNode schema = dbMetadata.get("schema");
        
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
        		else if (geoType.contentEquals("MultiPolygon")) {
        			builder.add(name, MultiPolygon.class);
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
