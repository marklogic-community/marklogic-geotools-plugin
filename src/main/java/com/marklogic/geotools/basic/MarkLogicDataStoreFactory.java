package com.marklogic.geotools.basic;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;

import org.geotools.data.Parameter;
import org.geotools.util.KVP;
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

	public static final Param USER_AUTH_TYPE =
			new Param(
					"user-auth-type",
					String.class,
					"MarkLogic Server Authentication Type for Users: BasicAuthContext (Default), DigestAuthContext, Custom",
					false,
					"BasicAuthContext",
					new KVP(
							Parameter.OPTIONS,
							Arrays.asList(new String[] {"Basic", "Digest","PreAuthenticatedHeader"}))
			);

//	public static final Param CUSTOM_AUTH_HEADER =
//			new Param(
//					"custom-auth-header",
//					String.class,
//					"HTTP Header where authentication token is provided.",
//					false,
//					null
//			);

//	/**
//	 * TODO: Dynmically give the available Roles from the current Role Service in a list, instead of a text box.
//	 */
//	public static final Param CUSTOM_AUTH_DEFAULT_ROLE =
//			new Param(
//					"custom-auth-default-role",
//					String.class,
//					"Default GeoServer Role to assign to pre-authenticated users",
//					false,
//					null
//			);

	public static final Param ML_USER_HOST_PARAM =
			new Param(
					"user-hostname",
					String.class,
					"Host for the WFS users to query.",
					false,
					null
			);

	public static final Param ML_USER_PORT_PARAM =
			new Param(
					"user-port",
					Integer.class,
					"Port on the host for the WFS users to query.",
					false,
					null
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
		return new Param[] {ML_HOST_PARAM, ML_PORT_PARAM, ML_USERNAME_PARAM, ML_PASSWORD_PARAM, ML_DATABASE_PARAM,
				NAMESPACE_PARAM, USER_AUTH_TYPE, ML_USER_HOST_PARAM, ML_USER_PORT_PARAM};
	}

	@Override
	public boolean canProcess(Map<String, ?> params) {
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
	public DataStore createDataStore(Map<String, ?> params) throws IOException {
		for (String k : params.keySet()) {
			LOGGER.log(Level.INFO, () -> "param key: {}" + k);
		}
		return new MarkLogicDataStore(params);
	}

	@Override
	public DataStore createNewDataStore(Map<String, ?> params) {
		throw new UnsupportedOperationException("MarkLogic Datastore is read only");
	}
}
