package com.marklogic.geotools.basic;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;

import org.geotools.util.logging.Logging;

public class MarkLogicDataStoreFactory implements DataStoreFactorySpi {

	private static final Logger LOGGER = Logging.getLogger(MarkLogicFeatureReader.class);

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
	public static final Param QUERY_MIME_TYPE_PARAM =
            new Param(
                    "queryMimeType",
                    String.class,
                    "application/json or application/xml",
                    false,
                    "application/json"
            );
	public static final Param LAYER_QUERY_PARAM =
            new Param(
                    "layerQuery",
                    String.class,
                    "Query to find available layers in the MarkLogic Database",
                    false,
                    "{\"query\": {\"queries\": [{\"collection-query\": {\"uri\": [\"typeDescriptors\"]}}]}}"
            );
    public static final Param BASE_QUERY_PARAM =
            new Param(
                    "baseQuery",
                    String.class,
                    "Serialized \"base\"query to limit results",
                    false,
                    "This should be a serialized query"
            );
    public static final Param OPTIONS_NAME_PARAM =
            new Param (
                    "optionsName",
                    String.class,
                    "MarkLogic Options definition",
                    false,
                    "geotools"
            );
    public static final Param TRANSFORM_NAME_PARAM =
            new Param (
                    "transformName",
                    String.class,
                    "Server side transformation to invoke for results",
                    false,
                    "geoJSONTransform"
            );
    public static final Param LAYER_QUERY_PROPERTY_PARAM =
            new Param(
                    "queryProperty",
                    String.class,
                    "The property in the Layer definition that identifies the query to be used for that layer",
                    false,
                    "definingQuery"
            );

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
		return new Param[] {ML_HOST_PARAM, ML_PORT_PARAM, ML_USERNAME_PARAM, ML_PASSWORD_PARAM, ML_DATABASE_PARAM, NAMESPACE_PARAM,
                OPTIONS_NAME_PARAM, TRANSFORM_NAME_PARAM, QUERY_MIME_TYPE_PARAM, BASE_QUERY_PARAM, LAYER_QUERY_PARAM, LAYER_QUERY_PROPERTY_PARAM };
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
//		String host = (String) ML_HOST_PARAM.lookUp(params);
//		Integer port = (Integer) ML_PORT_PARAM.lookUp(params);
//		String username = (String) ML_USERNAME_PARAM.lookUp(params);
//		String password = (String) ML_PASSWORD_PARAM.lookUp(params);
//		String database = (String) ML_DATABASE_PARAM.lookUp(params);
//		String namespace = (String) NAMESPACE_PARAM.lookUp(params);

		for (String k : params.keySet()) {
			LOGGER.log(Level.INFO, () -> "param key: {}" + k);
		}
//		SecurityContext c = new DatabaseClientFactory.DigestAuthContext(username, password);
//		return new MarkLogicDataStore(host, port, c, database, namespace);
		return new MarkLogicDataStore(params);
	}

	@Override
	public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
		throw new UnsupportedOperationException("MarkLogic Datastore is read only");
	}

}
