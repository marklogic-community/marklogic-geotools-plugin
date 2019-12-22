/* GeoTools - The Open Source Java GIS Toolkit
 * http://geotools.org
 *
 * (C) 2010-2014, Open Source Geospatial Foundation (OSGeo)
 *
 * This file is hereby placed into the Public Domain. This means anyone is
 * free to do whatever they wish with this file. Use it well and enjoy!
 */
package com.marklogic.geotools.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeometryFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.factory.Hints;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Intersects;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class MarkLogicTest {

	protected DatabaseClient client;
	protected FilterFactory2 filterFactory;
	protected Invocation.Builder invocationBuilder;
	JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
	
	public MarkLogicTest() throws IOException {
        // create client
        Properties p = loadProperties();
        String hostname = (String) p.get("hostname");
        int port = Integer.parseInt((String) p.get("port"));
        String database = (String) p.get("database");
        String username = (String) p.get("username");
        String password = (String) p.get("password");
        DigestAuthContext auth = new DatabaseClientFactory.DigestAuthContext(username, password);
        client = DatabaseClientFactory.newClient(hostname, port, database, auth);

        filterFactory = CommonFactoryFinder.getFilterFactory2();
        
        
		Client client = ClientBuilder.newClient();
		HttpAuthenticationFeature authentication = HttpAuthenticationFeature.digest(username, password);
		client.register(authentication);
		WebTarget webTarget = client.target("http://" + hostname + ":" + port + "/LATEST");
		WebTarget koopWebTarget = webTarget.path("resources/KoopProvider");
		invocationBuilder = koopWebTarget.request(MediaType.APPLICATION_JSON);

	}

	public Properties loadProperties() throws IOException {
		InputStream propFile = MarkLogicTest.class.getResourceAsStream("marklogic.properties");
        Properties p = new Properties();
        p.load(propFile);
        return p;
	}
	
    @Test
    public void test() throws Exception {
    	/*
        List<String> cities = new ArrayList<>();
        InputStream propFile = MarkLogicTest.class.getResourceAsStream("marklogic.properties");
        Properties p = new Properties();
        p.load(propFile);
        
        try (FileReader reader = new FileReader(file)) {
            CsvReader locations = new CsvReader(reader);
            locations.readHeaders();
            while (locations.readRecord()) {
                cities.add(locations.get("CITY"));
            }
        }
        assertTrue(cities.contains("Victoria"));
        */
    	assertTrue(true);
    }

    @Test
    public void testFeatureJSON() throws IOException {
      String s = "{\"type\":\"FeatureCollection\", \"metadata\":{\"name\":\"TestJoin\", \"maxRecordCount\":5000, \"fields\":[{\"name\":\"AGGREGATE_BANNER_MARKING\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"OBJECTID\", \"type\":\"Integer\", \"alias\":null}, {\"name\":\"GRAPH_SUBJECT_ID\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"GEOMETRY_WKT\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"OBSERVATION_TYPE\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"OBJ_TYPE\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"ACTIVITY_NAME\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"ACTIVITY_TYPE\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"DEPLOYMENT_STATUS\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"ENTITY_ACTIVITY_CONFIDENCE\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"ENTITY_CLASS_CONFIDENCE\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"ENTITY_LOC_CONFIDENCE\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"GENERAL_OBJECT_TYPE\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"INFORMATION_VIEW\", \"type\":\"String\", \"alias\":null, \"length\":1024}, {\"name\":\"IS_CONCEALED\", \"type\":\"Integer\", \"alias\":null}, {\"name\":\"R_FACILITY_1_GEOMETRY_WKT\", \"type\":\"String\", \"length\":1024}, {\"name\":\"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\", \"type\":\"String\", \"length\":1024}, {\"name\":\"R_FACILITY_1_LEGAL_NAME\", \"type\":\"String\", \"length\":1024}, {\"name\":\"R_FACILITY_1_NAME\", \"type\":\"String\", \"length\":1024}, {\"name\":\"R_ORGANISATION_1_ACTIVITY_TYPE\", \"type\":\"String\", \"length\":1024}, {\"name\":\"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\", \"type\":\"String\", \"length\":1024}, {\"name\":\"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\", \"type\":\"String\", \"length\":1024}, {\"name\":\"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\", \"type\":\"String\", \"length\":1024}], \"limitExceeded\":true, \"idField\":\"OBJECTID\", \"displayField\":\"NAME\"}, \"filtersApplied\":{\"geometry\":true, \"where\":true, \"offset\":true, \"limit\":true}, \"features\":[{\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1557369558, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":null, \"ACTIVITY_TYPE\":null, \"DEPLOYMENT_STATUS\":\"not deployed\", \"ENTITY_ACTIVITY_CONFIDENCE\":null, \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":\"HIGH\", \"GENERAL_OBJECT_TYPE\":null, \"INFORMATION_VIEW\":\"AS ADVERTISED\", \"IS_CONCEALED\":true, \"R_FACILITY_1_GEOMETRY_WKT\":\"LINESTRING (19 27,20 28)\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_FACILITY_1_LEGAL_NAME\":\"w6ZkYsC\", \"R_FACILITY_1_NAME\":\"Compaq\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"CEREMONY COMMISSIONING\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"LOW\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}, {\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1559195138, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":\"AUj7M\", \"ACTIVITY_TYPE\":null, \"DEPLOYMENT_STATUS\":null, \"ENTITY_ACTIVITY_CONFIDENCE\":null, \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":\"HIGH\", \"GENERAL_OBJECT_TYPE\":null, \"INFORMATION_VIEW\":null, \"IS_CONCEALED\":true, \"R_FACILITY_1_GEOMETRY_WKT\":\"POLYGON ((22 55.5,23 55.5,23 56.5,22 56.5,22 55.5))\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_FACILITY_1_LEGAL_NAME\":\"0wUwxeJ\", \"R_FACILITY_1_NAME\":\"Cincom\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"AGRICULTURE IRRIGATION\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"LOW\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"LOW\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}, {\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1554038580, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":null, \"ACTIVITY_TYPE\":\"AGRICULTURE CROP DRYING\", \"DEPLOYMENT_STATUS\":\"not deployed\", \"ENTITY_ACTIVITY_CONFIDENCE\":\"LOW\", \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":null, \"GENERAL_OBJECT_TYPE\":\"TyHXb\", \"INFORMATION_VIEW\":null, \"IS_CONCEALED\":false, \"R_FACILITY_1_GEOMETRY_WKT\":\"POLYGON ((24 28,25 28,25 29,24 29,24 28))\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_FACILITY_1_LEGAL_NAME\":\"JsjDFGl\", \"R_FACILITY_1_NAME\":\"Tucows\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"BARRICADING\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"CONFIRMED\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"LOW\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}, {\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1556154762, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":\"TzMZh\", \"ACTIVITY_TYPE\":null, \"DEPLOYMENT_STATUS\":null, \"ENTITY_ACTIVITY_CONFIDENCE\":\"LOW\", \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":\"LOW\", \"GENERAL_OBJECT_TYPE\":\"z9cJo\", \"INFORMATION_VIEW\":\"AS COLLABORATED\", \"IS_CONCEALED\":null, \"R_FACILITY_1_GEOMETRY_WKT\":\"POLYGON ((22 55.5,23 55.5,23 56.5,22 56.5,22 55.5))\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_FACILITY_1_LEGAL_NAME\":\"qg8lo6G\", \"R_FACILITY_1_NAME\":\"Cincom\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"CEREMONY COMMISSIONING\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"CONFIRMED\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"LOW\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}, {\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1555240090, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":\"ZrNCs\", \"ACTIVITY_TYPE\":\"BIO-MANUFACTURING\", \"DEPLOYMENT_STATUS\":\"not deployed\", \"ENTITY_ACTIVITY_CONFIDENCE\":null, \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":\"HIGH\", \"GENERAL_OBJECT_TYPE\":\"F1Bwr\", \"INFORMATION_VIEW\":\"AS ADVERTISED\", \"IS_CONCEALED\":true, \"R_FACILITY_1_GEOMETRY_WKT\":\"POLYGON ((34 61,35 61,35 62,34 62,34 61))\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"CONFIRMED\", \"R_FACILITY_1_LEGAL_NAME\":\"PQwIMww\", \"R_FACILITY_1_NAME\":\"CKX, Inc.\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"AGRICULTURE HARVESTING\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"LOW\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"LOW\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}, {\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1559538302, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":null, \"ACTIVITY_TYPE\":\"DISASTER RESPONSE EMERGENCY OPERATIONS\", \"DEPLOYMENT_STATUS\":\"not deployed\", \"ENTITY_ACTIVITY_CONFIDENCE\":\"CONFIRMED\", \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":\"CONFIRMED\", \"GENERAL_OBJECT_TYPE\":null, \"INFORMATION_VIEW\":null, \"IS_CONCEALED\":true, \"R_FACILITY_1_GEOMETRY_WKT\":\"POLYGON ((24 28,25 28,25 29,24 29,24 28))\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_FACILITY_1_LEGAL_NAME\":\"QjGHue7\", \"R_FACILITY_1_NAME\":\"Tucows\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"CEREMONY COMMISSIONING\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"LOW\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}, {\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1553735537, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":\"UQd59\", \"ACTIVITY_TYPE\":null, \"DEPLOYMENT_STATUS\":null, \"ENTITY_ACTIVITY_CONFIDENCE\":\"CONFIRMED\", \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":null, \"GENERAL_OBJECT_TYPE\":\"CWduN\", \"INFORMATION_VIEW\":null, \"IS_CONCEALED\":true, \"R_FACILITY_1_GEOMETRY_WKT\":\"LINESTRING (15 27,16 28)\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"LOW\", \"R_FACILITY_1_LEGAL_NAME\":\"Dozb5vB\", \"R_FACILITY_1_NAME\":\"Grundig\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"ACCURACY TESTING\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"MODERATE\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"HIGH\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}, {\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1551397072, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":null, \"ACTIVITY_TYPE\":null, \"DEPLOYMENT_STATUS\":null, \"ENTITY_ACTIVITY_CONFIDENCE\":null, \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":\"LOW\", \"GENERAL_OBJECT_TYPE\":null, \"INFORMATION_VIEW\":null, \"IS_CONCEALED\":null, \"R_FACILITY_1_GEOMETRY_WKT\":\"POLYGON ((22 55.5,23 55.5,23 56.5,22 56.5,22 55.5))\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_FACILITY_1_LEGAL_NAME\":\"2T3GR05\", \"R_FACILITY_1_NAME\":\"Cincom\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"CBWMATERIAL DESTRUCTION\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"LOW\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}, {\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1558759818, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":null, \"ACTIVITY_TYPE\":\"BIO-MANUFACTURING\", \"DEPLOYMENT_STATUS\":null, \"ENTITY_ACTIVITY_CONFIDENCE\":null, \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":null, \"GENERAL_OBJECT_TYPE\":\"UAbMy\", \"INFORMATION_VIEW\":null, \"IS_CONCEALED\":null, \"R_FACILITY_1_GEOMETRY_WKT\":\"LINESTRING (15 27,16 28)\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"LOW\", \"R_FACILITY_1_LEGAL_NAME\":\"n1baB7l\", \"R_FACILITY_1_NAME\":\"Grundig\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"COUNTER SMUGGLING OPERATIONS\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"HIGH\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"CONFIRMED\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"LOW\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}, {\"type\":\"Feature\", \"properties\":{\"AGGREGATE_BANNER_MARKING\":\"UNCLASSIFIED\", \"OBJECTID\":1558708533, \"GRAPH_SUBJECT_ID\":\"guide://9999/ad632f86-f02b-4eef-9098-693a4dce6ff5\", \"GEOMETRY_WKT\":\"POLYGON ((33.5 40,34.5 40,34.5 41,33.5 41,33.5 40))\", \"OBSERVATION_TYPE\":\"soc:ObjectObservation\", \"OBJ_TYPE\":\"somnas:SparkIgnitionEngine\", \"ACTIVITY_NAME\":null, \"ACTIVITY_TYPE\":null, \"DEPLOYMENT_STATUS\":null, \"ENTITY_ACTIVITY_CONFIDENCE\":\"HIGH\", \"ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"ENTITY_LOC_CONFIDENCE\":null, \"GENERAL_OBJECT_TYPE\":null, \"INFORMATION_VIEW\":null, \"IS_CONCEALED\":true, \"R_FACILITY_1_GEOMETRY_WKT\":\"LINESTRING (17 31,18 32)\", \"R_FACILITY_1_ENTITY_CLASS_CONFIDENCE\":\"CONFIRMED\", \"R_FACILITY_1_LEGAL_NAME\":\"4IgUbiS\", \"R_FACILITY_1_NAME\":\"Accenture\", \"R_ORGANISATION_1_ACTIVITY_TYPE\":\"AGRICULTURE HARVESTING\", \"R_ORGANISATION_1_ENTITY_CLASS_CONFIDENCE\":\"MODERATE\", \"R_ORGANISATION_1_ENTITY_IDENT_CONFIDENCE\":\"LOW\", \"R_ORGANISATION_1_ENTITY_LOC_CONFIDENCE\":\"MODERATE\"}, \"geometry\":{\"type\":\"Polygon\", \"coordinates\":[[[33.5, 40], [34.5, 40], [34.5, 41], [33.5, 41], [33.5, 40]]]}}]}";
      FeatureJSON j = new FeatureJSON();
      FeatureCollection<SimpleFeatureType, SimpleFeature> fc = j.readFeatureCollection(new StringReader(s));
      FeatureIterator<SimpleFeature> iter = fc.features();
      while (iter.hasNext()) {
    	  SimpleFeature f = iter.next();
    	  System.out.println(f);
    	  System.out.println(f.getFeatureType().getTypeName());
    	  System.out.println(f.getProperty("OBJECTID").getValue());
    	  System.out.println(f.getProperty("AGGREGATE_BANNER_MARKING").getValue());
    	  System.out.println(f.getAttribute("geometry") instanceof Polygon);
    	  System.out.println(f.getAttribute("geometry").toString());
      }
      /*
      assertEquals("feature", sf.getFeatureType().getTypeName());
      assertEquals("PPL", sf.getProperty("DSG").getValue());
      assertTrue(sf.getAttribute("geometry") instanceof Point);
      assertEquals( 14.6284410000001, ((Point)sf.getAttribute("geometry")).getX(), 0.0001);
      */
    }
    
    @Test
    public void testDataStoreFactory() throws IOException {
        System.out.println("testDataStoreFactory start\n");
        Properties p = loadProperties();
        
        DataStore store = DataStoreFinder.getDataStore(p);
        
        System.out.println(store);
        String[] names = store.getTypeNames();
        assertNotEquals("At least one type is retrieved", 0, names.length);

        System.out.println("typenames: " + names.length);
        System.out.println("typename[0]: " + names[0]);
        // example1 end
        System.out.println("\ntestDataStoreFactory end\n");
    }

    @Test
    public void testSchema() throws IOException {
        System.out.println("testSchema start\n");
        Properties p = loadProperties();

        DataStore store = DataStoreFinder.getDataStore(p);

        SimpleFeatureType type = store.getSchema("TEST_JOIN_0");

        System.out.println("featureType  name: " + type.getName());
        System.out.println("featureType attribute count: " + type.getAttributeCount());

        System.out.println("featuretype attributes list:");
        // access by list
        for (AttributeDescriptor descriptor : type.getAttributeDescriptors()) {
            System.out.print("  " + descriptor.getName());
            System.out.print(
                    " (" + descriptor.getMinOccurs() + "," + descriptor.getMaxOccurs() + ",");
            System.out.print((descriptor.isNillable() ? "nillable" : "manditory") + ")");
            System.out.print(" type: " + descriptor.getType().getName());
            System.out.println(" binding: " + descriptor.getType().getBinding().getSimpleName());
        }
        // access by index
        AttributeDescriptor attributeDescriptor = type.getDescriptor(0);
        System.out.println("attribute 0    name: " + attributeDescriptor.getName());
        System.out.println("attribute 0    type: " + attributeDescriptor.getType().toString());
        System.out.println("attribute 0 binding: " + attributeDescriptor.getType().getBinding());

        // access by name
        AttributeDescriptor activityTypeDescriptor = type.getDescriptor("ACTIVITY_TYPE");
        System.out.println("attribute 'ACTIVITY_TYPE'    name: " + activityTypeDescriptor.getName());
        System.out.println("attribute 'ACTIVITY_TYPE'    type: " + activityTypeDescriptor.getType().toString());
        System.out.println("attribute 'ACTIVITY_TYPE' binding: " + activityTypeDescriptor.getType().getBinding());

        // default geometry
        GeometryDescriptor geometryDescriptor = type.getGeometryDescriptor();
        System.out.println("default geom    name: " + geometryDescriptor.getName());
        System.out.println("default geom    type: " + geometryDescriptor.getType().toString());
        System.out.println("default geom binding: " + geometryDescriptor.getType().getBinding());
        System.out.println(
                "default geom     crs: "
                        + CRS.toSRS(geometryDescriptor.getCoordinateReferenceSystem()));

        // example2 end
        System.out.println("\ntestSchema end\n");
    }
    
    @Test
    public void testCount() throws Exception {
    	System.out.println("testBounds start\n");
        Properties p = loadProperties();

        DataStore store = DataStoreFinder.getDataStore(p);

        SimpleFeatureType type = store.getSchema("TEST_JOIN_0");
        SimpleFeatureSource source = store.getFeatureSource(new NameImpl("TEST_JOIN_0"));
        int count = source.getCount(new Query("", Filter.INCLUDE));
        System.out.println("TEST_JOIN_0 count: " + count);
        System.out.println("\ntestCount end\n");
    }
    
    @Test
    public void testBounds() throws IOException {
        System.out.println("testBounds start\n");
        Properties p = loadProperties();

        DataStore store = DataStoreFinder.getDataStore(p);

        SimpleFeatureType type = store.getSchema("TEST_JOIN_0");

        FeatureSource fs = store.getFeatureSource("TEST_JOIN_0");
        System.out.println("featureType  name: " + type.getName());
        System.out.println("featureType count: " + fs.getCount(new Query("", Filter.INCLUDE)));
        System.out.println("featureType bounds: " + store.getFeatureSource("TEST_JOIN_0").getBounds());

        // example2 end
        System.out.println("\ntestBounds end\n");
    }
    
    @Test
    public void testSimpleFeatureReader() throws IOException {
        Query q = new Query("TEST_JOIN_0", Filter.INCLUDE);
        q.setMaxFeatures(60);
        q.setStartIndex(1);
    	_testSimpleFeatureReader(q);
    }
    
    private void _testSimpleFeatureReader(Query q) throws IOException {
        System.out.println("testSimpleFeatureReader start\n");
        long startTime = System.currentTimeMillis();
        Properties p = loadProperties();
        
        DataStore store = DataStoreFinder.getDataStore(p);
        SimpleFeatureType type = store.getSchema("TEST_JOIN_0");
        SimpleFeatureSource source = store.getFeatureSource(new NameImpl("TEST_JOIN_0"));

//        SimpleFeatureCollection fc = source.getFeatures(q);

        System.out.println("open feature reader");

        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                 store.getFeatureReader(q, Transaction.AUTO_COMMIT)) {
          int count = 0;
          while (reader.hasNext()) {
        	System.out.println("reading feature...");
            SimpleFeature feature = reader.next();
            System.out.println("Feature ID: " + feature.getID());
            count++;
          }
          System.out.println("close feature reader");
          System.out.println("read in " + count + " features");
        }

        // example3 end
        System.out.println("\ntestSimpleFeatureReader elapsed Time: " + (System.currentTimeMillis() - startTime)/1000 + "\n");
    }

    @Test 
    public void testFilterToSQLFeatureReader() {
    	System.out.println("testFilterToSQLFeatureReader start\n");
    	long startTime = System.currentTimeMillis();
    	
    	try {
    		Filter f = CQL.toFilter("IS_CONCEALED >= 1 AND OBJECTID is null");
    		System.out.println(f.toString());
    		
    		Query q = new Query("TEST_JOIN_0", f);
    		q.setSortBy(new SortBy[] {SortBy.NATURAL_ORDER});
    		_testSimpleFeatureReader(q);
    	}
    	catch(Exception ex) {
    		ex.printStackTrace();
    	}
    	
    	System.out.println("\ntestFilterToSQLFeatureReader elapsed Time: " + (System.currentTimeMillis() - startTime) / 1000 + "\n");
    }
    
    @Test
    public void testFilterToSQL() {
    	System.out.println("testFilterToSQL start\n");
    	long startTime = System.currentTimeMillis();
    	
    	try {
    		Filter f = CQL.toFilter("attName >= 5");
    		//Filter geo = 
    		StringWriter writer = new StringWriter();
    		FilterToSQL f2s = new FilterToSQL(writer);
    		//String sql = f2s.encodeToString(f);l
    		f.accept(f2s, null);
    		System.out.println(writer.getBuffer());
    		
    		//f.accept(f2s, null);
    		    	}
    	catch(Exception ex) {
    		ex.printStackTrace();
    	}
    	System.out.println("\ntestFilterToSQL elapsed Time: " + (System.currentTimeMillis() - startTime) / 1000 + "\n");
    	
    }
    
	@Test
	public void testIntersects() {
		System.out.println("testIntersects start\n");
		long startTime = System.currentTimeMillis();

		Coordinate[] coordinates = new Coordinate[] {
				// I'm using lat/long... is that correct?
				new Coordinate(14.0, 43.0), new Coordinate(15.0, 43.0), new Coordinate(14.0, 44.0),
				new Coordinate(15.0, 44.0), new Coordinate(14.0, 43.0), };
		Intersects intersects = filterFactory.intersects(filterFactory.property("geom"), // this string doesn't actually matter
				//filterFactory.literal(new GeometryFactory().createPolygon(coordinates)));
				filterFactory.literal(new GeometryFactory().createPoint(new Coordinate(-118.005124, 34.110102))));

		StringWriter w = new StringWriter();
		FilterToMarkLogic filterToMarklogic = new FilterToMarkLogic(w);
		ObjectNode queryNode = nodeFactory.objectNode();
		Object o = intersects.accept(filterToMarklogic, queryNode);

		System.out.println("Output of SQL Portion:" + w.toString());
		System.out.println("Esri query parameters: " + queryNode.toString());
		//then we'll call the service, but right now we just want to debug and see if we're getting this thing to work
		//properly
		

/*		QueryManager qm = client.newQueryManager();
		SearchHandle results = new SearchHandle();
		qm.search(query, results);
		long totalResults = results.getTotalResults();
    System.out.println(totalResults);
		assertTrue(totalResults > 0);
*/		// testIntersects end
		System.out.println("\ntestIntersects elapsed Time: " + (System.currentTimeMillis() - startTime) / 1000 + "\n");
	}
	
	@Test
	public void testBBOX() {
		System.out.println("testBBOX start\n");
		long startTime = System.currentTimeMillis();

		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
		
		BBOX bbox = filterFactory.bbox(filterFactory.property("geom"),
				filterFactory.literal(new Envelope2D(
						new DirectPosition2D(DefaultGeographicCRS.WGS84,0.0, 10.0),
						new DirectPosition2D(DefaultGeographicCRS.WGS84,10.0, 20.0)
						)));
		StringWriter w = new StringWriter();
		FilterToMarkLogic filterToMarklogic = new FilterToMarkLogic(w);
		ObjectNode queryNode = nodeFactory.objectNode();
		Object o = bbox.accept(filterToMarklogic, queryNode);
		
		System.out.println("Output of SQL Portion:" + w.toString());
		System.out.println("Esri query parameters: " + queryNode.toString());
		//then we'll call the service, but right now we just want to debug and see if we're getting this thing to work
		//properly
		

/*		QueryManager qm = client.newQueryManager();
		SearchHandle results = new SearchHandle();
		qm.search(query, results);
		long totalResults = results.getTotalResults();
    System.out.println(totalResults);
		assertTrue(totalResults > 0);
*/		// testIntersects end
		System.out.println("\testBBOX elapsed Time: " + (System.currentTimeMillis() - startTime) / 1000 + "\n");
	}
	
	@Test
	public void testBBOXWithFeatureReader() throws IOException {
		System.out.println("testBBOX start\n");
		long startTime = System.currentTimeMillis();
		
		BBOX bbox = filterFactory.bbox(filterFactory.property("geom"),
				filterFactory.literal(new Envelope2D(
						new DirectPosition2D(DefaultGeographicCRS.WGS84,0.0, 10.0),
						new DirectPosition2D(DefaultGeographicCRS.WGS84,10.0, 20.0)
						)));
		
		Query q = new Query("TEST_JOIN_0", bbox);
		q.setSortBy(new SortBy[] {SortBy.NATURAL_ORDER});
		_testSimpleFeatureReader(q);
		System.out.println("\testBBOXWithFeatureReader elapsed Time: " + (System.currentTimeMillis() - startTime) / 1000 + "\n");
	}
	
	@Test
	public void testPoint() {
		System.out.println("testPoint start\n");
		long startTime = System.currentTimeMillis();

		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
		
		Intersects intersects = filterFactory.intersects(filterFactory.property("geom"),
				filterFactory.literal(new GeometryFactory().createPoint(new Coordinate(-118.005124, 34.110102))));
		StringWriter w = new StringWriter();
		FilterToMarkLogic filterToMarklogic = new FilterToMarkLogic(w);
		ObjectNode queryNode = nodeFactory.objectNode();
		Object o = intersects.accept(filterToMarklogic, queryNode);
		
		System.out.println("Output of SQL Portion:" + w.toString());
		System.out.println("Esri query parameters: " + queryNode.toString());
		//then we'll call the service, but right now we just want to debug and see if we're getting this thing to work
		//properly
		

/*		QueryManager qm = client.newQueryManager();
		SearchHandle results = new SearchHandle();
		qm.search(query, results);
		long totalResults = results.getTotalResults();
    System.out.println(totalResults);
		assertTrue(totalResults > 0);
*/		// testIntersects end
		System.out.println("\testPoint elapsed Time: " + (System.currentTimeMillis() - startTime) / 1000 + "\n");
	}
	
/*
    @Test
    public void example4() throws Exception {
        System.out.println("example4 start\n");
        InputStream propFile = MarkLogicTest.class.getResourceAsStream("marklogic.properties");
        Properties p = new Properties();
        p.load(propFile);

        // example4 start
        Map<String, Serializable> params = new HashMap<>();
        params.put("file", file);
        DataStore store = DataStoreFinder.getDataStore(params);

        FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        Set<FeatureId> selection = new HashSet<>();
        selection.add(ff.featureId("locations.7"));

        Filter filter = ff.id(selection);
        Query query = new Query("locations", filter);

        FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                store.getFeatureReader(query, Transaction.AUTO_COMMIT);

        try {
            while (reader.hasNext()) {
                SimpleFeature feature = reader.next();
                System.out.println("feature " + feature.getID());

                for (Property property : feature.getProperties()) {
                    System.out.print("\t");
                    System.out.print(property.getName());
                    System.out.print(" = ");
                    System.out.println(property.getValue());
                }
            }
        } finally {
            reader.close();
        }
        // example4 end
        System.out.println("\nexample4 end\n");
    }

    @Test
    public void example5() throws Exception {
        System.out.println("example5 start\n");
        InputStream propFile = MarkLogicTest.class.getResourceAsStream("marklogic.properties");
        Properties p = new Properties();
        p.load(propFile);
        // example5 start
        Map<String, Serializable> params = new HashMap<>();
        params.put("file", file);
        DataStore store = DataStoreFinder.getDataStore(params);

        SimpleFeatureSource featureSource = store.getFeatureSource("locations");

        Filter filter = CQL.toFilter("CITY = 'Denver'");
        SimpleFeatureCollection features = featureSource.getFeatures(filter);
        System.out.println("found :" + features.size() + " feature");
        SimpleFeatureIterator iterator = features.features();
        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                System.out.println(feature.getID() + " default geometry " + geometry);
            }
        } catch (Throwable t) {
            iterator.close();
        }

        // example5 end
        System.out.println("\nexample5 end\n");
    }

    @Test
    public void example6() throws Exception {
        System.out.println("example6 start\n");
        InputStream propFile = MarkLogicTest.class.getResourceAsStream("marklogic.properties");
        Properties p = new Properties();
        p.load(propFile);

        // example6 start
        Map<String, Serializable> params = new HashMap<>();
        params.put("file", file);
        DataStore store = DataStoreFinder.getDataStore(params);

        SimpleFeatureSource featureSource = store.getFeatureSource("locations");
        SimpleFeatureCollection featureCollection = featureSource.getFeatures();

        List<String> list = new ArrayList<>();
        try (SimpleFeatureIterator features = featureCollection.features(); ) {
            while (features.hasNext()) {
                list.add(features.next().getID());
            }
        } // try-with-resource will call features.close()

        System.out.println("           List Contents: " + list);
        System.out.println("    FeatureSource  count: " + featureSource.getCount(Query.ALL));
        System.out.println("    FeatureSource bounds: " + featureSource.getBounds(Query.ALL));
        System.out.println("FeatureCollection   size: " + featureCollection.size());
        System.out.println("FeatureCollection bounds: " + featureCollection.getBounds());

        // Load into memory!
        DefaultFeatureCollection collection = DataUtilities.collection(featureCollection);
        System.out.println("         collection size: " + collection.size());
        // example6 end
        System.out.println("\nexample6 end\n");
    }
    */
}