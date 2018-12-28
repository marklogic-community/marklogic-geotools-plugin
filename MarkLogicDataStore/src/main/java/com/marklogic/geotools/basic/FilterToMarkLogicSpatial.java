package com.marklogic.geotools.basic;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.filter.And;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class FilterToMarkLogicSpatial implements FilterVisitor, ExpressionVisitor {

	protected String geometryType;
	protected String spatialRel;
	protected JSONObject queryExtension;
	
	public JSONObject generatePayloadQueryExtension() {
		return queryExtension;
	}
	
	public String generatePayloadQueryGeometryType() {
		return geometryType;
	}
	
	public String generatePayloadQuerySpatialRel() {
		return spatialRel;
	}
	
	@Override
	public Object visit(NilExpression expression, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Add expression, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Divide expression, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Function expression, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Literal expression, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Multiply expression, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyName expression, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Subtract expression, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNullFilter(Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(ExcludeFilter filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(IncludeFilter filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(And filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Id filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Not filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Or filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsBetween filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsEqualTo filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsGreaterThan filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsLessThan filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsLike filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsNull filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(PropertyIsNil filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(BBOX filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Beyond filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Contains filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Crosses filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Disjoint filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(DWithin filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Equals filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Intersects filter, Object extraData) {
		// create the Intersects filter with a PropertyName that doesn't matter and a geometry
		// TODO: should this be pulled out similar to FilterToSql?
		Expression propertyNameExpression = filter.getExpression1();
		Expression literalExpression = filter.getExpression2();

		// swap operands if necessary
		if (propertyNameExpression instanceof Literal && literalExpression instanceof PropertyName) {
			propertyNameExpression = filter.getExpression2();
			literalExpression = filter.getExpression1();
		}

		// we don't actually care what the property name is in this case - all we want
		// is the geometry
		// TODO: we probably want to pull this logic into its own class
		Geometry geom = literalExpression.evaluate(null, Geometry.class);
		// TODO: for now assume a single point
		geom.getGeometryType(); // TODO: use this instead of assuming point?
		Coordinate coord = geom.getCoordinate();
		
		geometryType = "Point"; // TODO: make this an enum?
		spatialRel = "esrispatialrelintersects"; // TODO: make this an enum?
		
		queryExtension = new JSONObject();
		JSONObject geometry = new JSONObject();
		geometry.put("type", geometryType);
		JSONArray jsonCoord = new JSONArray();
		jsonCoord.add(coord.x);
		jsonCoord.add(coord.y);
		geometry.put("coordinates", jsonCoord);
		queryExtension.put("geometry", geometry);

		
		
		return null;
	}

	@Override
	public Object visit(Overlaps filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Touches filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Within filter, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(After after, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(AnyInteracts anyInteracts, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Before before, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Begins begins, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(BegunBy begunBy, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(During during, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(EndedBy endedBy, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Ends ends, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(Meets meets, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(MetBy metBy, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(OverlappedBy overlappedBy, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(TContains contains, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(TEquals equals, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(TOverlaps contains, Object extraData) {
		// TODO Auto-generated method stub
		return null;
	}

}
