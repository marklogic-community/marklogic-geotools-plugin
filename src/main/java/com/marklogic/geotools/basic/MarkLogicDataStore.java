package com.marklogic.geotools.basic;


import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.geotools.data.Query;
import org.geotools.data.store.ContentDataStore;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.ForbiddenUserException;
import com.marklogic.client.ResourceNotFoundException;
import com.marklogic.client.ResourceNotResendableException;
import com.marklogic.client.admin.QueryOptionsManager;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.InputStreamHandle;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.MatchDocumentSummary;
import com.marklogic.client.query.MatchLocation;
import com.marklogic.client.query.MatchSnippet;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawCombinedQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.client.MarkLogicServerException;

public class MarkLogicDataStore extends ContentDataStore {
	DatabaseClient client;
	DatabaseClientFactory.SecurityContext context;
	
	public MarkLogicDataStore(String host, int port, DatabaseClientFactory.SecurityContext securityContext, String database) {
		client = DatabaseClientFactory.newClient(host, port, securityContext);
	}
	
	DatabaseClient getClient() {
		return client;
	}
	
	//Update this to return a better description of our data, this is just a placeholder for now
	protected List<Name> createTypeNames() throws IOException {
        Name typeName = new NameImpl("MarkLogicGeoJSON");
        return Collections.singletonList(typeName);
    }
	
	@Override
    protected ContentFeatureSource createFeatureSource(ContentEntry entry) throws IOException {
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
