/* GeoTools - The Open Source Java GIS Toolkit
 * http://geotools.org
 *
 * (C) 2010-2014, Open Source Geospatial Foundation (OSGeo)
 *
 * This file is hereby placed into the Public Domain. This means anyone is
 * free to do whatever they wish with this file. Use it well and enjoy!
 */
package com.marklogic.geotools.basic;

import static org.junit.Assert.assertTrue;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import com.marklogic.client.DatabaseClientFactory.SecurityContext;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.vividsolutions.jts.geom.Geometry;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.spatial.Intersects;

public class MarkLogicTest {

	protected DatabaseClient client;
	protected FilterFactory2 ff;
	protected FilterToMarkLogic filterToMarklogic;

	public MarkLogicTest() throws Exception {
        // create client
        Properties p = loadProperties();
        String hostname = (String) p.get("hostname");
        int port = (int) Integer.parseInt((String) p.get("port"));
        String database = (String) p.get("database");
        String username = (String) p.get("username");
        String password = (String) p.get("password");
        DigestAuthContext auth = new DatabaseClientFactory.DigestAuthContext(username, password);
        client = DatabaseClientFactory.newClient(hostname, port, database, auth);

        ff = CommonFactoryFinder.getFilterFactory2();
        filterToMarklogic = new FilterToMarkLogic(client);
	}

	public Properties loadProperties() throws Exception {
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
    public void testFeatureJSON() throws Exception {
    String s = new String ("{\"type\":\"Feature\", \"properties\":{\"SORT_NAME\":null, \"DSG\":\"PPL\", \"RC\":3, \"GENERIC\":null, \"ELEV\":null, \"LONG_\":43.461561, \"NT\":\"NS\", \"UTM\":null, \"UGI\":null, \"MF\":\"M\", \"UNI\":null, \"GMF\":null, \"FEATURE_ID\":10007550, \"FL\":null, \"OBJECTID\":17070, \"USID3\":\"X\", \"USID1\":\"YM50YAR50AX1443A4-83S\", \"USID2\":\"CIB 01 Imagery\", \"FULL_NAME\":\"الجنبعية\", \"COMMENTS\":null, \"FC\":null, \"POP\":null, \"SHORT_FORM\":null, \"LAT\":14.628441, \"LC\":\"ara\", \"ADM1\":8, \"JOG\":null, \"CC2\":null, \"CC1\":\"YM\", \"ADM2\":null, \"GCC\":null, \"UFI\":222390, \"PC\":5}, \"geometry\":{\"type\":\"Point\", \"coordinates\":[14.6284410000001, 43.4615610000001]}}");
    FeatureJSON j = new FeatureJSON();
    SimpleFeature sf = j.readFeature(s);
    System.out.println(sf);
    }
    
    @Test
    public void testDataStoreFactory() throws Exception {
        System.out.println("testDataStoreFactory start\n");
        Properties p = loadProperties();
        
        DataStore store = DataStoreFinder.getDataStore(p);
        
        System.out.println(store);
        String names[] = store.getTypeNames();
        System.out.println("typenames: " + names.length);
        System.out.println("typename[0]: " + names[0]);
        // example1 end
        System.out.println("\ntestDataStoreFactory end\n");
    }

    @Test
    public void testSchema() throws Exception {
        System.out.println("testSchema start\n");
        Properties p = loadProperties();

        DataStore store = DataStoreFinder.getDataStore(p);

        SimpleFeatureType type = store.getSchema("Land_Polygons");

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
    public void testBounds() throws Exception {
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
    public void example3() throws Exception {
        System.out.println("example3 start\n");
        long startTime = System.currentTimeMillis();
        Properties p = loadProperties();
        
        DataStore datastore = DataStoreFinder.getDataStore(p);

        Query query = new Query("http://marklogic.com:Clans");

        System.out.println("open feature reader");
        FeatureReader<SimpleFeatureType, SimpleFeature> reader =
                datastore.getFeatureReader(query, Transaction.AUTO_COMMIT);
        try {
            int count = 0;
            while (reader.hasNext()) {
                SimpleFeature feature = reader.next();
                System.out.println("  " + feature.getID() + " " + feature.getAttribute("ADM1"));
                count++;
            }
            System.out.println("close feature reader");
            System.out.println("read in " + count + " features");
        } 
        catch(Exception ex) {
        	ex.printStackTrace();
        	}
        finally {
            reader.close();
        }
        // example3 end
        System.out.println("\nexample3 elapsed Time: " + (System.currentTimeMillis() - startTime)/1000 + "\n");
    }

	@Test
	public void testIntersects() throws Exception {
		System.out.println("testIntersects start\n");
		long startTime = System.currentTimeMillis();

		Coordinate[] coordinates = new Coordinate[] {
				// I'm using lat/long... is that correct?
				new Coordinate(14.0, 43.0), new Coordinate(15.0, 43.0), new Coordinate(14.0, 44.0),
				new Coordinate(15.0, 44.0), new Coordinate(14.0, 43.0), };
		Intersects intersects = ff.intersects(ff.property("geom"), // this string doesn't actually matter
				ff.literal(new GeometryFactory().createPolygon(coordinates)));
		StructuredQueryDefinition query = (StructuredQueryDefinition) intersects.accept(filterToMarklogic, null);

		QueryManager qm = client.newQueryManager();
		SearchHandle results = new SearchHandle();
		qm.search(query, results);
		System.out.println(results.getTotalResults());
		// testIntersects end
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