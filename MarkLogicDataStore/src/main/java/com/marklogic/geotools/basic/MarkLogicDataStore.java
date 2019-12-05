package com.marklogic.geotools.basic;


import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;

import com.marklogic.client.query.*;
import org.geotools.data.Query;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.TuplesHandle;

public class MarkLogicDataStore extends ContentDataStore {

	DatabaseClient client;
	private String layerQueryPropertyName;
	private String transformName;
	private String optionsName;
	private Format queryFormat; // Only Valid Values are "Format.XML" or "Format.JSON"
	private StringHandle layerQuery;
	private StringHandle baseQuery;


	/**
	 *
	 * @param host
	 * @param port
	 * @param securityContext
	 * @param database
	 * @param namespace
	 */
	public MarkLogicDataStore(String host, int port, DatabaseClientFactory.SecurityContext securityContext, String database, String namespace) {
		setupClient(host,port,securityContext,database);
		setNamespaceURI(namespace);
	}

	/**
	 * Creates a MarkLogic Data Store object using a MAP.  Should be passed from the MarkLogic Data Store Fatory class.
	 * @param map Map<String, Serializable> with necessary details to create the data store.
	 * @throws IOException
	 */
	public MarkLogicDataStore(Map<String, Serializable> map) throws IOException{
		// extract properties
		String host = (String) MarkLogicDataStoreFactory.ML_HOST_PARAM.lookUp(map);
		Integer port = (Integer) MarkLogicDataStoreFactory.ML_PORT_PARAM.lookUp(map);
		String username = (String) MarkLogicDataStoreFactory.ML_USERNAME_PARAM.lookUp(map);
		String password = (String) MarkLogicDataStoreFactory.ML_PASSWORD_PARAM.lookUp(map);
		String database = (String) MarkLogicDataStoreFactory.ML_DATABASE_PARAM.lookUp(map);
		String namespace = (String) MarkLogicDataStoreFactory.NAMESPACE_PARAM.lookUp(map);

		Format localFmt = Format.getFromMimetype((String) MarkLogicDataStoreFactory.QUERY_MIME_TYPE_PARAM.lookUp(map));
		if (localFmt.equals(Format.JSON) || localFmt.equals(Format.XML)) {
			this.queryFormat = localFmt;
		}

		if (Objects.nonNull((String) MarkLogicDataStoreFactory.LAYER_QUERY_PARAM.lookUp(map))) {
			this.layerQuery = new StringHandle((String) MarkLogicDataStoreFactory.LAYER_QUERY_PARAM.lookUp(map)).withFormat(this.queryFormat);
		}

		if (Objects.nonNull((String) MarkLogicDataStoreFactory.BASE_QUERY_PARAM.lookUp(map))) {
			this.baseQuery = new StringHandle((String) MarkLogicDataStoreFactory.BASE_QUERY_PARAM.lookUp(map)).withFormat(this.queryFormat);
		}

		if (Objects.nonNull((String) MarkLogicDataStoreFactory.OPTIONS_NAME_PARAM.lookUp(map))) {
			this.optionsName = (String) MarkLogicDataStoreFactory.OPTIONS_NAME_PARAM.lookUp(map);
		}

		if (Objects.nonNull((String) MarkLogicDataStoreFactory.TRANSFORM_NAME_PARAM.lookUp(map))) {
			this.transformName = (String) MarkLogicDataStoreFactory.TRANSFORM_NAME_PARAM.lookUp(map);
		}

		if (Objects.nonNull((String) MarkLogicDataStoreFactory.LAYER_QUERY_PROPERTY_PARAM.lookUp(map))) {
			this.layerQueryPropertyName = (String) MarkLogicDataStoreFactory.LAYER_QUERY_PROPERTY_PARAM.lookUp(map);
		}

		setupClient(host,port,new DatabaseClientFactory.DigestAuthContext(username,password), database);
		setNamespaceURI(namespace);
	}

	private void setupClient(String host, int port, DatabaseClientFactory.SecurityContext securityContext, String database) {
		if (Objects.nonNull(database)) {
			client = DatabaseClientFactory.newClient(host, port, database, securityContext);
		} else {
			client = DatabaseClientFactory.newClient(host, port, securityContext);
		}
	}
	
	DatabaseClient getClient() {
		return client;
	}

	/**
	 * Update this to return a better description of our data, this is just a placeholder for now
	 * @return
	 * @throws IOException
	 */
	protected List<Name> createTypeNames() throws IOException {

		LOGGER.info("**************************************************************************");
		LOGGER.info("createTypeNames called!");
		LOGGER.info("Datastore namespace: " + getNamespaceURI());
		LOGGER.info("**************************************************************************");

// 		QueryManager queryMgr = client.newQueryManager();
// 		StructuredQueryBuilder qb = queryMgr.newStructuredQueryBuilder();
// 		ValuesDefinition vdef = queryMgr.newValuesDefinition("typeNamePlusNamespace", this.optionsName);
// 		RawCombinedQueryDefinition querydef = queryMgr.newRawCombinedQueryDefinition(this.layerQuery);
// //		vdef.setQueryDefinition(qb.collection("typeDescriptors"));
// 		vdef.setQueryDefinition(querydef);
// 		TuplesHandle results = queryMgr.tuples(vdef, new TuplesHandle());
// 		List<Name> nameList = new ArrayList<>();
// 		Tuple[] tuples = results.getTuples();
		
		GeoQueryServiceManager geoQueryServices = new GeoQueryServiceManager(client);
		
		
		try {
			List<Name> nameList;
			nameList = geoQueryServices.getLayerNames();
			
			ArrayList<String> uris = new ArrayList<String>();
			for (Name n : nameList) {
				uris.add(n.getURI());
			}
			LOGGER.info("**************************************************************************");
			LOGGER.log(Level.INFO, () -> "Names returned: " + Arrays.toString(nameList.toArray()));
			LOGGER.info("**************************************************************************");

			return nameList;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new ArrayList<Name>();
		}
    }
	
	@Override
  protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
    LOGGER.info("MarkLogicDataStore.createFeatureSource: entry.getName() = " + entry.getName().getNamespaceURI() + ":" + entry.getName().getLocalPart());
		return new MarkLogicBasicFeatureSource(entry, Query.ALL, this.layerQueryPropertyName);
  }
	

	protected QueryDefinition createMarkLogicQueryDefinition(Query gtQuery, QueryManager queryMgr, String options) {

	    // create a query builder for the query options
	    StructuredQueryBuilder qb = new StructuredQueryBuilder();

	    // build a search definition
	    StructuredQueryDefinition querydef = qb.collection("geojson");   
	    StringHandle queryHandle = new StringHandle().with(
	    				"<search xmlns=\"http://marklogic.com/appservices/search\">" +
	                    querydef.serialize() + 
	                    options +
	                "</search>").withFormat(Format.XML);

	    RawCombinedQueryDefinition query =
	            queryMgr.newRawCombinedQueryDefinition(queryHandle);
	    return query;
	}

	public String getLayerQueryPropertyName() {
		return layerQueryPropertyName;
	}

	public void setLayerQueryPropertyName(String layerQueryPropertyName) {
		this.layerQueryPropertyName = layerQueryPropertyName;
	}

	public String getTransformName() {
		return transformName;
	}

	public void setTransformName(String transformName) {
		this.transformName = transformName;
	}

	public String getOptionsName() {
		return optionsName;
	}

	public void setOptionsName(String optionsName) {
		this.optionsName = optionsName;
	}

	public Format getQueryFormat() {
		return queryFormat;
	}

	public void setQueryFormat(Format queryFormat) {
		this.queryFormat = queryFormat;
	}

	public StringHandle getLayerQuery() {
		return layerQuery;
	}

	public void setLayerQuery(StringHandle layerQuery) {
		this.layerQuery = layerQuery;
	}

	public StringHandle getBaseQuery() {
		return baseQuery;
	}

	public void setBaseQuery(StringHandle baseQuery) {
		this.baseQuery = baseQuery;
	}
}
