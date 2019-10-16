package com.marklogic.geotools.basic;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.ValuesHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.RawCombinedQueryDefinition;
import com.marklogic.client.query.ValuesDefinition;

public class MarkLogicBasicFeatureSource extends ContentFeatureSource {
	private static final Logger LOGGER = Logging.getLogger(MarkLogicBasicFeatureSource.class);
	private JsonNode definingQuery;
	private JsonNode dbMetadata;
	private AttributeTypeBuilder attributeBuilder;
    
	public MarkLogicBasicFeatureSource(ContentEntry entry, Query query) {
		super(entry, query);
		attributeBuilder = new AttributeTypeBuilder(new FeatureTypeFactoryImpl());
		retrieveDBMetadata(entry, query);
	}
	
	protected void retrieveDBMetadata(ContentEntry entry, Query query) {
		// create a query builder for the query options
//	    StructuredQueryBuilder qb = new StructuredQueryBuilder();
//	    StructuredQueryDefinition querydef = qb.and(
//	    		qb.collection("typeDescriptors"),
//	    		qb.value(qb.jsonProperty("namespace"), entry.getName().getNamespaceURI()),
//	    		qb.value(qb.jsonProperty("typeName"), entry.getName().getLocalPart())
//	    	    );
	    JSONDocumentManager docMgr = getDataStore().getClient().newJSONDocumentManager();
	    JacksonHandle handle = new JacksonHandle();
	    docMgr.read(entry.getName().getNamespaceURI() + "/" + entry.getName().getLocalPart() + ".json", handle);
	    dbMetadata = handle.get();
//	    LOGGER.info("retrieveDBMetadata: dbMetadata: " + dbMetadata.toString());
	    definingQuery = dbMetadata.get("definingQuery");
	}

	@Override
	public MarkLogicDataStore getDataStore() {
		return (MarkLogicDataStore) super.getDataStore();
	}
	
	//make this a UDF later, for now just return values we've poked into each doc
	@Override
	protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
		LOGGER.log(Level.INFO, () -> "*******************************************************************");
		LOGGER.log(Level.INFO, () -> "in MarkLogicBasicFeatureSource:getBoundsInternal");

		DatabaseClient client = getDataStore().getClient();
		try {
			QueryManager qm = client.newQueryManager();

			StringHandle rawHandle =
						new StringHandle("{\"search\":{\"ctsquery\":" + definingQuery.toString() + "}}").withFormat(Format.JSON);
			LOGGER.log(Level.INFO, () -> "rawHandle:\n" + rawHandle.get());
			RawCombinedQueryDefinition querydef =
						qm.newRawCombinedQueryDefinition(rawHandle);

			ValuesDefinition vdef = qm.newValuesDefinition("box-west", "geotools");
			vdef.setQueryDefinition(querydef);
			vdef.setAggregate("min");

			ValuesHandle westH = qm.values(vdef, new ValuesHandle());
			float west = westH.getAggregate("min").get("xs:float", Float.class);

			vdef = qm.newValuesDefinition("box-east", "geotools");
			vdef.setQueryDefinition(querydef);
			vdef.setAggregate("max");
			ValuesHandle eastH = qm.values(vdef, new ValuesHandle());
			float east = eastH.getAggregate("max").get("xs:float", Float.class);

			vdef = qm.newValuesDefinition("box-south", "geotools");
			vdef.setQueryDefinition(querydef);
			vdef.setAggregate("min");
			ValuesHandle southH = qm.values(vdef, new ValuesHandle());
			float south = southH.getAggregate("min").get("xs:float", Float.class);

			vdef = qm.newValuesDefinition("box-north", "geotools");
			vdef.setQueryDefinition(querydef);
			vdef.setAggregate("max");
			ValuesHandle northH = qm.values(vdef, new ValuesHandle());
			float north = northH.getAggregate("max").get("xs:float", Float.class);

			LOGGER.log(Level.INFO, () -> "west: " + west);
			LOGGER.log(Level.INFO, () -> "east: " + east);
			LOGGER.log(Level.INFO, () -> "north: " + north);
			LOGGER.log(Level.INFO, () -> "south: " + south);
			LOGGER.log(Level.INFO, () -> "*******************************************************************");

			return new ReferencedEnvelope(west, east, south, north, DefaultGeographicCRS.WGS84);
		}
		catch (Exception ex) {
			LOGGER.log(Level.SEVERE, "unable to parse bounds", ex);
			return null;
		}
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		if (query.getFilter() == Filter.INCLUDE) {
			DatabaseClient client = getDataStore().getClient();
			try {
				QueryManager qm = client.newQueryManager();
				String JSON_OPTIONS = "\"options\": {\"return-results\": false}";
				StringHandle rawHandle =
							new StringHandle("{\"search\":{\"ctsquery\":" + definingQuery.toString() + "," + JSON_OPTIONS + "}}").withFormat(Format.JSON);
				LOGGER.info("rawHandle:\n" + rawHandle.get());
				RawCombinedQueryDefinition queryDef =
							qm.newRawCombinedQueryDefinition(rawHandle);

				LOGGER.info("getCountInternal(): running query\n" + rawHandle.get());
				SearchHandle resultsHandle = new SearchHandle();
				// run the search
				qm.search(queryDef, resultsHandle);
				LOGGER.info(resultsHandle.getQueryCriteria().toString());
				int count = (int) resultsHandle.getTotalResults();
				LOGGER.log(Level.INFO, () -> "getCountInternal(): query returned " + count + " results");
				return count;
			}
			catch (Exception ex) {
				LOGGER.log(Level.SEVERE,"unable to execute query", ex);
				return -1;
			}
	  }
		LOGGER.log(Level.INFO, () -> "Query is " + query.toString() + "; feature by feature count required for MarkLogic driver");
    return -1; // feature by feature scan required to count records
	}

	@Override
	protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query) throws IOException {
		return new MarkLogicFeatureReader(getState(), query, definingQuery.toString());
	}

	protected AttributeDescriptor buildAttributeDescriptor(String name, Class<?> binding) {
		AttributeDescriptor descriptor;
		attributeBuilder.setCRS(DefaultGeographicCRS.WGS84);
		attributeBuilder.setBinding(binding);
		attributeBuilder.setNamespaceURI(getDataStore().getNamespaceURI());
		Name nameImpl = new NameImpl(getDataStore().getNamespaceURI(), name);
		if (Geometry.class.isAssignableFrom(binding)) {
			GeometryType type = attributeBuilder.buildGeometryType();
			descriptor = attributeBuilder.buildDescriptor(nameImpl, type);
		}
		else {
			AttributeType type = attributeBuilder.buildType();
			descriptor = attributeBuilder.buildDescriptor(nameImpl, type);
		}
		return descriptor;
	}
	
	@Override
	protected SimpleFeatureType buildFeatureType() throws IOException {
		if (dbMetadata == null) {
			retrieveDBMetadata(this.entry, this.query);
		}
		
		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
//		LOGGER.info("Setting feature type builder name to " + entry.getName());
		builder.setName(entry.getName());
		builder.setSRS( "EPSG:4326" );
		builder.setNamespaceURI(getDataStore().getNamespaceURI());

		JsonNode schema = dbMetadata.get("schema");

		for (JsonNode node : schema) {
			String name = node.get("name").asText();
			Class<?> binding = toClass(node);

			AttributeDescriptor attrDesc = buildAttributeDescriptor(name, binding);
			builder.add(attrDesc);
		}
		// build the type (it is immutable and cannot be modified)
		return builder.buildFeatureType();
	}

	protected Class toClass(JsonNode node) {
		Class binding = String.class;
		String type = node.get("type").asText();

		if ("geometry".contentEquals(type)) {
			String geoType = node.get("geometryType").asText();
			if ("point".contentEquals(geoType)) {
				binding = Point.class;
			}
			else if ("linestring".contentEquals(geoType)) {
				binding = LineString.class;
			}
			else if ("polygon".contentEquals(geoType)) {
				binding = Polygon.class;
			}
			else if ("MultiPolygon".contentEquals(geoType)) {
				binding = MultiPolygon.class;
			}
		}
		else if ("string".contentEquals(type)) {
			binding = String.class;
		}
		else if ("int".contentEquals(type)) {
			binding = Integer.class;
		}
		else if ("float".contentEquals(type)) {
			binding = Float.class;
		}
		else if ("double".contentEquals(type)) {
			binding = Double.class;
		}
		else if ("boolean".contentEquals(type)) {
			binding = Boolean.class;
		}
		else if ("dateTime".contentEquals(type)) {
			binding = Date.class;
		}
		return binding;
	}

}
