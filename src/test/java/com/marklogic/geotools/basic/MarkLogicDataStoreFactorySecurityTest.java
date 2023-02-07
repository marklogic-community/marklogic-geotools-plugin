package com.marklogic.geotools.basic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotEquals;

public class MarkLogicDataStoreFactorySecurityTest  {

    protected DatabaseClient adminClient;
    protected DatabaseClient userClient;

    public MarkLogicDataStoreFactorySecurityTest() throws IOException {
        Properties p = loadProperties();
        String hostname = (String) p.get("hostname");
        int port = Integer.parseInt((String) p.get("port"));
        String database = (String) p.get("database");
        String username = (String) p.get("username");
        String password = (String) p.get("password");
        DatabaseClientFactory.DigestAuthContext auth = new DatabaseClientFactory.DigestAuthContext(username, password);
        adminClient = DatabaseClientFactory.newClient(hostname, port, database, auth);
    }

	private Map<String, ?> propertiesToMap(Properties props) {
		Map<String, Serializable> map = new HashMap<>();
		props.keySet().forEach(key -> map.put((String)key, props.getProperty((String)key)));
		return map;
	}

    public Properties loadProperties() throws IOException {
        InputStream propFile = MarkLogicDataStoreFactorySecurityTest.class.getResourceAsStream("marklogic-security.properties");
        Properties p = new Properties();
        p.load(propFile);
        return p;
    }

    @Test
    public void testDataStoreFactory() throws IOException {
        System.out.println("testDataStoreFactory start\n");
        DataStore store = DataStoreFinder.getDataStore(propertiesToMap(loadProperties()));

        System.out.println(store);
        String[] names = store.getTypeNames();
        assertNotEquals("At least one type is retrieved", 0, names.length);

        System.out.println("typenames: " + names.length);
        System.out.println("typename[0]: " + names[0]);
        // example1 end
        System.out.println("\ntestDataStoreFactory end\n");
    }

    @Test
    public void testSimpleFeatureReader() throws IOException {

        String credentials = "<credentials><gx_attr_dn>admin</gx_attr_dn><groups>admin</groups><env_rel_to>USA</env_rel_to></credentials>:";
        String encoded = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(encoded,"")
        );
        Query q = new Query("TEST_JOIN_0", Filter.INCLUDE);
        q.setMaxFeatures(60);
        q.setStartIndex(1);
        _testSimpleFeatureReader(q);
    }

    private void _testSimpleFeatureReader(Query q) throws IOException {
        System.out.println("testSimpleFeatureReader start\n");
        long startTime = System.currentTimeMillis();

        DataStore store = DataStoreFinder.getDataStore(propertiesToMap(loadProperties()));
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
}
