package com.marklogic.geotools.basic;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawCombinedQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.client.query.Tuple;
import com.marklogic.client.query.ValuesDefinition;

public class MarkLogicDataStore extends ContentDataStore {
	DatabaseClient client;
	DatabaseClientFactory.SecurityContext context;
	
	public MarkLogicDataStore(String host, int port, DatabaseClientFactory.SecurityContext securityContext, String database, String namespace) {
		client = DatabaseClientFactory.newClient(host, port, securityContext);
		setNamespaceURI(namespace);
	}
	
	DatabaseClient getClient() {
		return client;
	}
	
	//Update this to return a better description of our data, this is just a placeholder for now
	protected List<Name> createTypeNames() throws IOException {
		
		System.out.println("**************************************************************************");
		System.out.println("createTypeNames called!");
		System.out.println("Datastore namespace: " + getNamespaceURI());
		System.out.println("**************************************************************************");
		
		QueryManager queryMgr = client.newQueryManager();
		StructuredQueryBuilder qb = queryMgr.newStructuredQueryBuilder();
		ValuesDefinition vdef = queryMgr.newValuesDefinition("typeNamePlusNamespace", "geotools");
		vdef.setQueryDefinition(qb.collection("typeDescriptors"));
		TuplesHandle results = queryMgr.tuples(vdef, new TuplesHandle());
		ArrayList<Name> nameList = new ArrayList<Name>();
		Tuple[] tuples = results.getTuples();
		
		for (Tuple t : tuples) {
			//Name n = new NameImpl("http://www.opengeospatial.net/cite", v.get("string", String.class));
			//nameList.add(n);
			String ns = t.getValues()[0].get(String.class);
			String localname = t.getValues()[1].get(String.class);
			Name n = new NameImpl(ns, localname);
			nameList.add(n);
			System.out.println("**************************************************************************");
			System.out.println("Adding " + n.toString() + " to typeNames");
			System.out.println("**************************************************************************");
			
		}
		
		System.out.println("**************************************************************************");
		System.out.println("Names returned: " + Arrays.toString(nameList.toArray()));
		System.out.println("**************************************************************************");
		return nameList;
    }
	
	@Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
        System.out.println("MarkLogicDataStore.createFeatureSource: entry.getName() = " + entry.getName().getNamespaceURI() + ":" + entry.getName().getLocalPart());
		return new MarkLogicBasicFeatureSource(entry, Query.ALL);
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
	
}
