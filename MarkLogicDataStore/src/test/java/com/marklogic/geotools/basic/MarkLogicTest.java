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

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryDefinition;

import java.io.IOException;
import java.io.InputStream;

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
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.Intersects;

public class MarkLogicTest {

	protected DatabaseClient client;
	protected FilterFactory2 filterFactory;
	protected FilterToMarkLogic filterToMarklogic;
	protected Invocation.Builder invocationBuilder;

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
        filterToMarklogic = new FilterToMarkLogic(client);
        
        
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
      String s = "{\"type\":\"Feature\", \"properties\":{\"SORT_NAME\":null, \"DSG\":\"PPL\", \"RC\":3, \"GENERIC\":null, \"ELEV\":null, \"LONG_\":43.461561, \"NT\":\"NS\", \"UTM\":null, \"UGI\":null, \"MF\":\"M\", \"UNI\":null, \"GMF\":null, \"FEATURE_ID\":10007550, \"FL\":null, \"OBJECTID\":17070, \"USID3\":\"X\", \"USID1\":\"YM50YAR50AX1443A4-83S\", \"USID2\":\"CIB 01 Imagery\", \"FULL_NAME\":\"الجنبعية\", \"COMMENTS\":null, \"FC\":null, \"POP\":null, \"SHORT_FORM\":null, \"LAT\":14.628441, \"LC\":\"ara\", \"ADM1\":8, \"JOG\":null, \"CC2\":null, \"CC1\":\"YM\", \"ADM2\":null, \"GCC\":null, \"UFI\":222390, \"PC\":5}, \"geometry\":{\"type\":\"Point\", \"coordinates\":[14.6284410000001, 43.4615610000001]}}";
      FeatureJSON j = new FeatureJSON();
      SimpleFeature sf = j.readFeature(s);
      System.out.println(sf);
      assertEquals("feature", sf.getFeatureType().getTypeName());
      assertEquals("PPL", sf.getProperty("DSG").getValue());
      assertTrue(sf.getAttribute("geometry") instanceof Point);
      assertEquals( 14.6284410000001, ((Point)sf.getAttribute("geometry")).getX(), 0.0001);
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

        SimpleFeatureType type = store.getSchema("SOMNAS_BRIDGE_SUPPORT_BOAT_0");

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
        AttributeDescriptor cityDescriptor = type.getDescriptor("scalerank");
        System.out.println("attribute 'Land_Polygons'    name: " + cityDescriptor.getName());
        System.out.println("attribute 'Land_Polygons'    type: " + cityDescriptor.getType().toString());
        System.out.println("attribute 'Land_Polygons' binding: " + cityDescriptor.getType().getBinding());

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
    public void testBounds() throws IOException {
        System.out.println("testBounds start\n");
        Properties p = loadProperties();

        DataStore store = DataStoreFinder.getDataStore(p);

        SimpleFeatureType type = store.getSchema("Land_Polygons");

        FeatureSource fs = store.getFeatureSource("Land_Polygons");
        System.out.println("featureType  name: " + type.getName());
        System.out.println("featureType count: " + fs.getCount(new Query("", Filter.INCLUDE)));
        System.out.println("featureType bounds: " + store.getFeatureSource("Land_Polygons").getBounds());

        // example2 end
        System.out.println("\ntestBounds end\n");
    }
    
    //@Test
    public void example3() throws IOException {
        System.out.println("example3 start\n");
        long startTime = System.currentTimeMillis();
        Properties p = loadProperties();
        
        DataStore datastore = DataStoreFinder.getDataStore(p);

        Query query = new Query("http://marklogic.com:Clans");

        System.out.println("open feature reader");

        try (FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                 datastore.getFeatureReader(query, Transaction.AUTO_COMMIT)) {
          int count = 0;
          while (reader.hasNext()) {
            SimpleFeature feature = reader.next();
            System.out.println("  " + feature.getID() + " " + feature.getAttribute("ADM1"));
            count++;
          }
          System.out.println("close feature reader");
          System.out.println("read in " + count + " features");
        }

        // example3 end
        System.out.println("\nexample3 elapsed Time: " + (System.currentTimeMillis() - startTime)/1000 + "\n");
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
		StructuredQueryDefinition query = (StructuredQueryDefinition) intersects.accept(filterToMarklogic, null);
		
		JSONObject payload = filterToMarklogic.generatePayload();
		System.out.println(payload.toString());
		
		
		// hit the service
		Response response = invocationBuilder.post(Entity.entity(payload.toJSONString(), MediaType.APPLICATION_JSON));
		String result = response.readEntity(String.class);
		
		System.out.println(result);
		

/*		QueryManager qm = client.newQueryManager();
		SearchHandle results = new SearchHandle();
		qm.search(query, results);
		long totalResults = results.getTotalResults();
    System.out.println(totalResults);
		assertTrue(totalResults > 0);
*/		// testIntersects end
		System.out.println("\ntestIntersects elapsed Time: " + (System.currentTimeMillis() - startTime) / 1000 + "\n");
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