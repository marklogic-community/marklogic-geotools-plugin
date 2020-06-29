package com.marklogic.geotools.basic;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
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
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.geotools.util.factory.Hints;
import org.geotools.util.factory.Hints.Key;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

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
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.client.query.ValuesDefinition;

public class MarkLogicBasicFeatureSource extends ContentFeatureSource {
	private static final Logger LOGGER = Logging.getLogger(MarkLogicBasicFeatureSource.class);
	private JsonNode dbMetadata;
	private String serviceName;
	private int layerId;
	private String idField;
	private String geometryColumn;
	private AttributeTypeBuilder attributeBuilder;
	
	private GeoQueryServiceManager geoQueryServices = getDataStore().getGeoQueryServiceManager();
    
	public MarkLogicBasicFeatureSource(ContentEntry entry, Query query) {
		super(entry, query);
		if (LOGGER.isLoggable(Level.FINE)) {
			LOGGER.fine("In MarkLogicBasicFeatureSource()");
		}
		attributeBuilder = new AttributeTypeBuilder(new FeatureTypeFactoryImpl());
		retrieveDBMetadata(entry, query);
		HashSet<Key> hints = new HashSet<Hints.Key>();
		hints.add(Hints.JTS_GEOMETRY_FACTORY);
        hints.add(Hints.JTS_COORDINATE_SEQUENCE_FACTORY);
        hints.add(Hints.GEOMETRY_DISTANCE);
        this.hints = Collections.unmodifiableSet(hints);
	}
	
	@Override 
	public QueryCapabilities buildQueryCapabilities() {
		return new QueryCapabilities() {
			public boolean isJoiningSupported() {return false;}
			public boolean isOffsetSupported() {return true;}
			public boolean isReliableFIDSupported() {return true;}
			public boolean isUseProvidedFIDSupported() {return false;}
			public boolean isVersionSupported() {return false;}
			public boolean supportsSorting(SortBy[] sortAttributes) {return true;}
		};
	}
	
	@Override
    protected boolean canOffset() {
        return true;
    }

    @Override
    protected boolean canLimit() {
        return true;
    }

    @Override
    protected boolean canRetype() {
        return true;
    }

    @Override
    protected boolean canSort() {
        return true;
    }

    @Override
    protected boolean canFilter() {
        return true;
    }
    
	protected void retrieveDBMetadata(ContentEntry entry, Query query) {
		
		try {
			dbMetadata=geoQueryServices.getLayerSchema(entry.getName().getLocalPart());
			serviceName = dbMetadata.get("serviceName").asText();
			layerId = dbMetadata.get("metadata").get("id").asInt();
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("serviceName: " + serviceName + " layerId: " + layerId);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

		JsonNode extent = dbMetadata.get("metadata").get("extent");
		LOGGER.log(Level.INFO, () -> "Extent: " + extent.toString());
		
		ReferencedEnvelope env = new ReferencedEnvelope(
				extent.get("xmin").asDouble(), 
				extent.get("xmax").asDouble(),
				extent.get("ymin").asDouble(),
				extent.get("ymax").asDouble(),
				DefaultGeographicCRS.WGS84);
		return env;
	}

	@Override
	protected int getCountInternal(Query query) throws IOException {
		if (query.getFilter() == Filter.INCLUDE) {
			try {
				int count = geoQueryServices.getLayerFeatureCount(serviceName, layerId);
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
		return new MarkLogicFeatureReader(getState(), query, serviceName, layerId, idField, geometryColumn);
	}

	protected AttributeDescriptor buildAttributeDescriptor(String name, Class<?> binding) {
		AttributeDescriptor descriptor;
		//attributeBuilder.setCRS(DefaultGeographicCRS.WGS84);
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
		builder.setName(entry.getName());
		builder.setCRS( DefaultGeographicCRS.WGS84 );
		builder.setNamespaceURI(getDataStore().getNamespaceURI());

		JsonNode metadata = dbMetadata.get("metadata");
		JsonNode schema = metadata.get("fields");
		
		idField = metadata.get("idField").asText();
		builder.add(idField, Integer.class);
		
		builder.setDefaultGeometry("geometry");
		/*
		JsonNode geometryColumnNode = metadata.get("geometrySource");
		if (geometryColumnNode == null) {
			JsonNode geometryNode = metadata.get("geometry");
			if (geometryNode != null) {
				JsonNode geometryColumnSourceNode = geometryNode.get("source");
				if (geometryColumnSourceNode != null) {
					geometryColumnNode = geometryColumnSourceNode.get("column");
				}
			}
		}
		if (geometryColumnNode != null) {
			geometryColumn = geometryColumnNode.asText();
		}
		*/
		
//		Class<?> geoBinding = geometryToClass(metadata.get("geometryType").asText());
//		builder.add("the_geom", geoBinding);
		
		for (JsonNode node : schema) {
			String name = node.get("name").asText();
			if (builder.get(name) == null) {
				Class<?> binding = toClass(node);
			
				AttributeDescriptor attrDesc = buildAttributeDescriptor(name, binding);
				builder.add(name, binding);
			}
		}
		// build the type (it is immutable and cannot be modified)
		return builder.buildFeatureType();
		
	}

	protected Class<?> geometryToClass(String geoType) {
		Class<?> binding = String.class;

		if ("Point".equalsIgnoreCase(geoType)) {
			binding = MultiPoint.class;
		}
		else if ("LineString".equalsIgnoreCase(geoType)) {
			binding = MultiLineString.class;
		}
		else if ("Polygon".equalsIgnoreCase(geoType)) {
			binding = MultiPolygon.class;
		}
		else if ("MultiPolygon".equalsIgnoreCase(geoType)) {
			binding = MultiPolygon.class;
		}
		return binding;
	}
	
	protected Class<?> toClass(JsonNode node) {
		Class<?> binding = String.class;
		String type = node.get("type").asText();

		if ("geometry".equalsIgnoreCase(type)) {
			String geoType = node.get("geometryType").asText();
			if ("point".equalsIgnoreCase(geoType)) {
				binding = MultiPoint.class;
			}
			else if ("linestring".equalsIgnoreCase(geoType)) {
				binding = MultiLineString.class;
			}
			else if ("polygon".equalsIgnoreCase(geoType)) {
				binding = MultiPolygon.class;
			}
			else if ("multipolygon".equalsIgnoreCase(geoType)) {
				binding = MultiPolygon.class;
			}
		}
		else if ("String".equalsIgnoreCase(type)) {
			binding = String.class;
		}
		else if ("Integer".equalsIgnoreCase(type)) {
			binding = Integer.class;
		}
		else if ("Double".equalsIgnoreCase(type)) {
			binding = Double.class;
		}
		else if ("boolean".equalsIgnoreCase(type)) {
			binding = Boolean.class;
		}
		else if ("Date".equalsIgnoreCase(type)) {
			binding = Date.class;
		}
		return binding;
	}

}
