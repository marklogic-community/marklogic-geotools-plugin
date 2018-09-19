package com.marklogic.geotools.basic;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;

import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.SecurityContext;

public class MarkLogicDataStoreFactory implements DataStoreFactorySpi {

	Boolean isAvailable = null;
	public static final Param ML_HOST_PARAM = 
			new Param(
					"hostname",
					String.class,
					"MarkLogic Server Hostname",
					true,
					"localhost");
	public static final Param ML_PORT_PARAM =
            new Param(
                    "port",
                    Integer.class,
                    "MarkLogic Server Port",
                    true,
                    8000);
	public static final Param ML_USERNAME_PARAM =
			new Param(
					"username",
					String.class,
					"MarkLogic Server Username",
					true,
					null);
	public static final Param ML_PASSWORD_PARAM =
			new Param(
					"password",
					String.class,
					"MarkLogic Server Password",
					true,
					null);
	public static final Param ML_DATABASE_PARAM =
			new Param(
					"database",
					String.class,
					"MarkLogic Database Name",
					false,
					null
					);
	public static final Param NAMESPACE_PARAM =
			new Param(
					"namespace",
					String.class,
					"MarkLogic Datastore Namespace",
					false,
					null
					);


	
	public MarkLogicDataStoreFactory() {}
	
	@Override
	public String getDisplayName() {
		return "MarkLogic (Basic)";

	}

	@Override
	public String getDescription() {
		return "Basic MarkLogic Data Store";
	}

	@Override
	public Param[] getParametersInfo() {
		return new Param[] {ML_HOST_PARAM, ML_PORT_PARAM, ML_USERNAME_PARAM, ML_PASSWORD_PARAM, ML_DATABASE_PARAM, NAMESPACE_PARAM};
	}

	@Override
	public boolean canProcess(Map<String, Serializable> params) {
		try {
            String host = (String) ML_HOST_PARAM.lookUp(params);
            if (host == null) return false;
            Integer port = (Integer) ML_PORT_PARAM.lookUp(params);
            if (port == null) return false;
            String username = (String) ML_USERNAME_PARAM.lookUp(params);
            if (username == null) return false;
            String password = (String) ML_PASSWORD_PARAM.lookUp(params);
            if (password == null) return false;
            String database = (String) ML_DATABASE_PARAM.lookUp(params);
            
            String namespace = (String) NAMESPACE_PARAM.lookUp(params);

            return true;
        } catch (Exception e) {
            // ignore as we are expected to return true or false
        }
        return false;
	}

	@Override
	public synchronized boolean isAvailable() {
		if (isAvailable == null) {
            try {
                Class markLogicReaderType = Class.forName("com.marklogic.geotools.basic.MarkLogicFeatureReader");
                isAvailable = true;
            } catch (ClassNotFoundException e) {
                isAvailable = false;
            }
        }
        return isAvailable;
	}

	@Override
	public Map<Key, ?> getImplementationHints() {
		return Collections.emptyMap();
	}

	@Override
	public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
		String host = (String) ML_HOST_PARAM.lookUp(params);
        Integer port = (Integer) ML_PORT_PARAM.lookUp(params);
        String username = (String) ML_USERNAME_PARAM.lookUp(params);
        String password = (String) ML_PASSWORD_PARAM.lookUp(params);
        String database = (String) ML_DATABASE_PARAM.lookUp(params);
        String namespace = (String) NAMESPACE_PARAM.lookUp(params);
        
        for (String k : params.keySet()) {
            System.out.println("param key: " + k);
         }
        SecurityContext c = new DatabaseClientFactory.DigestAuthContext(username, password);
        MarkLogicDataStore ds = new MarkLogicDataStore(host, port, c, database, namespace);
        return ds;
	}

	@Override
	public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
		throw new UnsupportedOperationException("MarkLogic Datastore is read only");
	}

}
