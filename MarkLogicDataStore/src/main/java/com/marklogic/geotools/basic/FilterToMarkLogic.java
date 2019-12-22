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
import java.util.ArrayList;
import java.util.Iterator;
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
    	ObjectNode esriExtGeometry = null;
    	ObjectNode queryNode = (ObjectNode)extraData;
    	/*
    	 * Sample of what queryNode might look like after this method, with a 
    	 * polygon input.  
    	 * The "where" part will be set at the end, after all the FilterToMarkLogic
    	 * logic is done.  This method only sets the spatialRel, geometryType, geometry,
    	 * and ext properties.  The "geometry" property should be the esri-formatted geometry
    	 * in accordance with the FeatureServer Layer Query spec.  The "extension"."geometry"
    	 * is a copy of the "geometry" property but with additional "type" and "coordinates" 
    	 * properties to make it a GeoJSON object.
    	 * 
    {
    	"query": {
        "spatialRel": "esriSpatialRelIntersects",
        "where": "1=1",
        "geometryType": "esriGeometryPolygon",
        "geometry": {
            "rings": [
                [
                    [-97.06138, 32.837],
                    [-97.06133, 32.836],
                    [-97.06124, 32.834],
                    [-97.06127, 32.832],
                    [-97.06138, 32.837]
                ],
                [
                    [-97.06326, 32.759],
                    [-97.06298, 32.755],
                    [-97.06153, 32.749],
                    [-97.06326, 32.759]
                ]
            ],
            "spatialReference": {
                "wkid": 4326
            }
        },
        "extension": {
            "geometry": {
                "rings": [
                    [
                        [-97.06138, 32.837],
                        [-97.06133, 32.836],
                        [-97.06124, 32.834],
                        [-97.06127, 32.832],
                        [-97.06138, 32.837]
                    ],
                    [
                        [-97.06326, 32.759],
                        [-97.06298, 32.755],
                        [-97.06153, 32.749],
                        [-97.06326, 32.759]
                    ]
                ],
                "spatialReference": {
                    "wkid": 4326
                },
                "type": "MultiPolygon",
                "coordinates": [
                    [
                        [
                            [-97.06138, 32.837],
                            [-97.06133, 32.836],
                            [-97.06124, 32.834],
                            [-97.06127, 32.832],
                            [-97.06138, 32.837]
                        ]
                    ],
                    [
                        [
                            [-97.06326, 32.759],
                            [-97.06153, 32.749],
                            [-97.06298, 32.755],
                            [-97.06326, 32.759]
                        ]
                    ]
                ]
            }
        }
    }
}
    	 */

		ObjectNode ext = nodeFactory.objectNode();
		
    	if (geometry.getValue() instanceof Envelope) {
    		Envelope env = (Envelope)geometry.getValue();
    		esriExtGeometry = envelopeToEsri(env);
    		esriGeometry = envelopeToEsriPolygon(env);
    		
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
	    	
	    	queryNode.set("spatialRel", nodeFactory.textNode(spatialRel));
	    	Geometries geomType = Geometries.get(geo);
	    	switch(geomType) {
	    	case POINT:
	    		queryNode.set("geometryType", nodeFactory.textNode("esriGeometryPoint"));
	    		esriGeometry = pointToEsri((Point)geo);
	    		break;
	    	case MULTIPOINT:
	    		queryNode.set("geometryType", nodeFactory.textNode("esriGeometryMultipoint"));
	    		esriGeometry = multiPointToEsri((MultiPoint)geo);
	    		break;
	    	case LINESTRING:
	    		queryNode.set("geometryType", nodeFactory.textNode("esriGeometryPolyline"));
	    		esriGeometry = lineStringToEsri((LineString)geo);
	    		break;
	    	case MULTILINESTRING:
	    		queryNode.set("geometryType", nodeFactory.textNode("esriGeometryPolyline"));
	    		esriGeometry = multiLineStringtoEsri((MultiLineString)geo);
	    		break;
	    	case POLYGON:
	    		queryNode.set("geometryType", nodeFactory.textNode("esriGeometryPolygon"));
	    		esriGeometry = polygonToEsri((Polygon)geo);
	    		break;
	    	case MULTIPOLYGON:
	    		queryNode.set("geometryType", nodeFactory.textNode("esriGeometryPolygon"));
	    		esriGeometry = multiPolygonToEsri((MultiPolygon)geo);
	    		break;
	    	default:
	    		throw new RuntimeException(
	                    "Unsupported Geometry Type: " + geomType.toString());
	    	}
	    	esriExtGeometry = geometryToGeoJson(geo);
    		//also need to create the actual esri format, ugh
    	}
    	try {
    		out.write("1=1");
    	}
    	catch(IOException e) {
    		throw new RuntimeException(IO_ERROR, e);
    	}
		queryNode.set("geometry", esriGeometry);
		
		if (esriGeometry != null) {
			Iterator<String> fieldNames = esriGeometry.fieldNames();
			while (fieldNames.hasNext()) {
				String fieldName = fieldNames.next();
				esriExtGeometry.set(fieldName, esriGeometry.get(fieldName));
			}
		}
    	//esriExtGeometry.set("geometry", esriGeometry);

		ext.set("geometry", esriExtGeometry);
		queryNode.set("extension", ext);
    	return extraData;
    }
	
    private ObjectNode pointToEsri(Point p) {
    	ObjectNode node = nodeFactory.objectNode();
    	node.set("x", nodeFactory.numberNode(p.getX()));
    	node.set("y", nodeFactory.numberNode(p.getY()));
    	
    	ObjectNode spatialReference = nodeFactory.objectNode();
    	spatialReference.set("wkid", nodeFactory.numberNode(4326));
    	node.set("spatialReference", spatialReference);
    	return node;
    }

    private void setSpatialReference(ObjectNode node, int spatialRef) {
    	ObjectNode spatialReference = nodeFactory.objectNode();
    	spatialReference.set("wkid", nodeFactory.numberNode(4326));
    	node.set("spatialReference", spatialReference);
    }
    
    private ArrayNode pointsToCoordinateArray(Coordinate[] coords) {
    	ArrayNode pointArray = nodeFactory.arrayNode();
    	for (Coordinate c : coords) {
    		ArrayNode coordArray = nodeFactory.arrayNode();
    		coordArray.add(c.getX());
    		coordArray.add(c.getY());
    		pointArray.add(coordArray);
    	}
    	return pointArray;
    }
    
    private ObjectNode multiPointToEsri(MultiPoint p) {
    	ObjectNode node = nodeFactory.objectNode();
    	ArrayNode pointArray = pointsToCoordinateArray(p.getCoordinates());
    	
    	node.set("points", pointArray);
    	setSpatialReference(node, 4326);
    	return node;
    }
    
    private ArrayNode lineStringToEsriArray(LineString l) {
    	ArrayNode pointArray = pointsToCoordinateArray(l.getCoordinates());
    	ArrayNode linestringArray = nodeFactory.arrayNode();
    	linestringArray.add(pointArray);
    	return linestringArray;
    }
    
    private ObjectNode lineStringToEsri(LineString l) {
    	ObjectNode node = nodeFactory.objectNode();
    	ArrayNode linestringArray = lineStringToEsriArray(l);
    	
    	node.set("paths", linestringArray);
    	setSpatialReference(node, 4326);
    	return node;
    }

    private ObjectNode multiLineStringtoEsri(MultiLineString mls) {
    	ObjectNode node = nodeFactory.objectNode();
    	ArrayNode multiLineStringArray = nodeFactory.arrayNode();
    	for (int i = 0; i < mls.getNumGeometries(); i++) {
    		LineString l = (LineString)mls.getGeometryN(i);
        	ArrayNode linestringArray = lineStringToEsriArray(l);
        	multiLineStringArray.add(linestringArray);
    	}
    	node.set("paths", multiLineStringArray);
    	setSpatialReference(node, 4326);
    	return node;
    }
    
    private void addPolygonToRingsArray(Polygon p, ArrayNode rings) {
    	ArrayNode exteriorRing = lineStringToEsriArray(p.getExteriorRing());
    	rings.add(exteriorRing);
    	
    	for (int i = 0; i < p.getNumInteriorRing(); i++) {
    		ArrayNode interiorRing = lineStringToEsriArray(p.getInteriorRingN(i));
    		rings.add(interiorRing);
    	}
    }
    
    private ObjectNode polygonToEsri(Polygon p) {
    	ObjectNode node = nodeFactory.objectNode();
    	ArrayNode rings = nodeFactory.arrayNode();
    	
    	addPolygonToRingsArray(p, rings);
    	
    	node.set("rings", rings);
    	setSpatialReference(node, 4326);
    	return node;
    }
    
    private ObjectNode envelopeToEsriPolygon(Envelope e) {
    	Coordinate[] coordList = new Coordinate[5];
    	coordList[0] = new Coordinate(e.getMinimum(0), e.getMinimum(1));
    	coordList[1] = new Coordinate(e.getMaximum(0), e.getMinimum(1));
    	coordList[2] = new Coordinate(e.getMaximum(0), e.getMaximum(1));
    	coordList[3] = new Coordinate(e.getMinimum(0), e.getMaximum(1));
    	coordList[4] = new Coordinate(e.getMinimum(0), e.getMinimum(1));
    	
    	Polygon p = geometryFactory.createPolygon(coordList);
    	ObjectNode geoJson = geometryToGeoJson(p);
    	return geoJson;
    }
    
    private ObjectNode multiPolygonToEsri(MultiPolygon mp) {
    	ObjectNode node = nodeFactory.objectNode();
    	ArrayNode rings = nodeFactory.arrayNode();
    	
    	for (int i = 0; i < mp.getNumGeometries(); i++) {
    		Polygon p = (Polygon)mp.getGeometryN(i);
    		addPolygonToRingsArray(p, rings);
    	}
    	
    	node.set("rings", rings);
    	setSpatialReference(node, 4326);
    	return node;
    }
  
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
