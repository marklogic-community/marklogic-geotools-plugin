package com.marklogic.geotools.basic;

import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.geometry.Envelope;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geometry.jts.JTSFactoryFinder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

public class FilterToMarkLogic extends FilterToSQL {

	private static final Logger LOGGER = Logging.getLogger(FilterToMarkLogic.class);
	
	protected GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
	protected JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
	protected GeometryJSON geometryJson = new GeometryJSON();
	
	protected FilterToMarkLogicSpatial spatialFilter = null;
	
	public FilterToMarkLogic(Writer writer) {
		super(writer);
	}
	
	/** Handles the common case of a PropertyName,Literal geometry binary spatial operator. */
    protected Object visitBinarySpatialOperator(
            BinarySpatialOperator filter,
            PropertyName property,
            Literal geometry,
            boolean swapped,
            Object extraData) {
    	
    	String spatialRel = "esriSpatialRelContains";
    	ObjectNode esriGeometry = null;
    	ObjectNode queryNode = (ObjectNode)extraData;
    	
    	if (geometry.getValue() instanceof Envelope) {
    		esriGeometry = envelopeToEsri((Envelope)geometry.getValue());
    		queryNode.set("geometryType", nodeFactory.textNode("esriGeometryEnvelope"));
    	}
    	else {
	    	Geometry geo = (Geometry)geometry.getValue();
	    	
	    	if (filter instanceof Contains) {
	    		spatialRel = "esriSpatialRelContains";
	    	}
	    	else if (filter instanceof Intersects) {
	    		spatialRel = "esriSpatialRelIntersects";
	    	}
	    	else if (filter instanceof Crosses) {
	    		spatialRel = "esriSpatialRelCrosses";
	    	}
	    	else if (filter instanceof Overlaps) {
	    		spatialRel = "esriSpatialRelOverlaps";
	    	}
	    	else if (filter instanceof Touches) {
	    		spatialRel = "esriSpatialRelTouches";
	    	}
	    	else if (filter instanceof Within) {
	    		spatialRel = "esriSpatialRelWithin";
	    	}
	    	else {
	    		throw new RuntimeException(
	                    "Unsupported Geospatial Filter Type");
	    	}
	    	
	    	
	    	Geometries geomType = Geometries.get(geo);
	    	
	    	switch(geomType) {
	    	case POINT:
	    	case MULTIPOINT:
	    	case LINESTRING:
	    	case MULTILINESTRING:
	    	case POLYGON:
	    	case MULTIPOLYGON:
	    		esriGeometry = geometryToGeoJson(geo);
	    	default:
	    		throw new RuntimeException(
	                    "Unsupported Geometry Type: " + geomType.toString());
	    	}
    	}
    	try {
    		out.write("1=1");
    	}
    	catch(IOException e) {
    		throw new RuntimeException(IO_ERROR, e);
    	}
		ObjectNode ext = nodeFactory.objectNode();
		ext.set("geometry", esriGeometry);
		queryNode.set("extension", ext);
    	return extraData;
    }
/*	
    private ObjectNode pointToEsri(Point p) {
    	ObjectNode node = nodeFactory.objectNode();
    	node.set("x", nodeFactory.numberNode(p.getX()));
    	node.set("y", nodeFactory.numberNode(p.getY()));
    	
    	ObjectNode spatialReference = nodeFactory.objectNode();
    	spatialReference.set("wkid", nodeFactory.numberNode(4326));
    	node.set("spatialReference", spatialReference);
    	return node;
    }

    private ObjectNode multiPointToEsri(MultiPoint p) {
    	ObjectNode node = nodeFactory.objectNode();
    	ArrayNode pointArray = nodeFactory.arrayNode();
    	
    	for (Coordinate c : p.getCoordinates()) {
    		ArrayNode coordArray = nodeFactory.arrayNode();
    		coordArray.add(c.getX());
    		coordArray.add(c.getY());
    		pointArray.add(coordArray);
    	}
    	
    	node.set("points", pointArray);
    	ObjectNode spatialReference = nodeFactory.objectNode();
    	spatialReference.set("wkid", nodeFactory.numberNode(4326));
    	node.set("spatialReference", spatialReference);
    	return node;
    }
    
    private ObjectNode lineStringtoEsri(LineString l) {
    	return null;
    }

    private ObjectNode multiLineStringtoEsri(MultiLineString p) {
    	return null;
    }
    
    private ObjectNode multiPolygonToEsri(MultiPolygon p) {
    	return null;
    }
*/    
    private ObjectNode geometryToGeoJson(Geometry g) {
    	try {
    		StringWriter writer = new StringWriter();
    		geometryJson.write(g, writer);
    		ObjectMapper objectMapper = new ObjectMapper();
    	    ObjectNode json = (ObjectNode)objectMapper.readTree(writer.toString());
    	    return json;
    	}
    	catch (IOException ex) {
    		throw new RuntimeException("Unable to parse geometry: " + g.toString());
    	}
    }
    
    private ObjectNode envelopeToEsri(Envelope e) {
    	ObjectNode node = nodeFactory.objectNode();
    	node.set("xmin", nodeFactory.numberNode(e.getMinimum(0)));
    	node.set("xmax", nodeFactory.numberNode(e.getMaximum(0)));
    	node.set("ymin", nodeFactory.numberNode(e.getMinimum(1)));
    	node.set("ymax", nodeFactory.numberNode(e.getMaximum(1)));
    	
    	ObjectNode spatialReference = nodeFactory.objectNode();
    	spatialReference.set("wkid", nodeFactory.numberNode(4326));
    	node.set("spatialReference", spatialReference);
    	return node;
    }
    
    
//    /**
//     * Handles the more general case of two generic expressions.
//     *
//     * <p>The most common case is two PropertyName expressions, which happens during a spatial join.
//     */
//    protected Object visitBinarySpatialOperator(
//            BinarySpatialOperator filter, Expression e1, Expression e2, Object extraData) {
//        throw new RuntimeException(
//                "Subclasses must implement this method in order to handle geometries");
//    }
}
