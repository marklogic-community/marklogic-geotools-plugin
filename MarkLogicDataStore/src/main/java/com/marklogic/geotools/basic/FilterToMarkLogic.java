package com.marklogic.geotools.basic;

import org.apache.commons.collections4.IteratorUtils;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.And;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
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

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryBuilder.GeospatialOperator;
import com.marklogic.client.query.StructuredQueryBuilder.Point;
import com.marklogic.client.query.StructuredQueryDefinition;

public class FilterToMarkLogic implements FilterVisitor, ExpressionVisitor {

//	private StructuredQueryBuilder qb;
//	
//	public FilterToMarkLogic() {
//		qb = new StructuredQueryBuilder();
//	}
//	
//	public FilterToMarkLogic(StructuredQueryBuilder qb) {
//		this.qb = qb;
//	}
//
//	public FilterToMarkLogic(String options) {
//		qb = new StructuredQueryBuilder(options);
//	}
	


	protected FilterFactory2 ff;

    /** The schema the encoder will use */
	protected SimpleFeatureType featureType;

	// MarkLogic DB variables
	//protected DatabaseClient client;
	//protected QueryManager qm;
	protected StructuredQueryBuilder qb;

	public FilterToMarkLogic(DatabaseClient client) {
		//this.client = client;
		QueryManager qm = client.newQueryManager();
		qb = qm.newStructuredQueryBuilder();
		qb.geoRegionPath( // TODO: should this be in the constructor or some other setter, or nowhere?
			qb.pathIndex("/metadata/ctsRegion"),
			StructuredQueryBuilder.CoordinateSystem.WGS84);
		ff = CommonFactoryFinder.getFilterFactory2();
	}

    /**
     * Sets the featuretype the encoder is encoding for.
     *
     * <p>This is used for context for attribute expressions when encoding to a MarkLogic query.
     *
     * @param featureType
     */
    public void setFeatureType(SimpleFeatureType featureType) {
        this.featureType = featureType;
    }

    @Override
	public Object visit(NilExpression expression, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Add expression, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Divide expression, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Function expression, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Literal expression, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Multiply expression, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(PropertyName expression, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Subtract expression, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitNullFilter(Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(ExcludeFilter filter, Object extraData) {
		return qb.not(qb.and());
	}

	@Override
	public Object visit(IncludeFilter filter, Object extraData) {
		return qb.and();
	}

	@Override
	public Object visit(And filter, Object extraData) {
		return visit((BinaryLogicOperator) filter, "AND");
	}

    /**
     * Common implementation for BinaryLogicOperator filters. This way they're all handled
     * centrally.
     *
     * @param filter the logic statement to be turned into a MarkLogic query.
     * @param extraData extra filter data. Not modified directly by this method.
     */
    protected Object visit(BinaryLogicOperator filter, Object extraData) {
        String type = (String) extraData;

        java.util.Iterator<Filter> filters = filter.getChildren().iterator();
        StructuredQueryDefinition[] queries = (StructuredQueryDefinition[]) IteratorUtils.toArray(filters);
        
        // TODO: can i use an enum here to be safer?
        if ("AND".equals(type))
        	return qb.and(queries);
        else if ("OR".equals(type))
        	return qb.or(queries);
        else
        	throw new RuntimeException(); // TODO: what kind of excetion?
    }

    @Override
	public Object visit(Id filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Not filter, Object extraData) {
		StructuredQueryDefinition query = (StructuredQueryDefinition) filter.getFilter().accept(this, extraData);
		return qb.not(query);
	}

	@Override
	public Object visit(Or filter, Object extraData) {
		return visit((BinaryLogicOperator) filter, "OR");
	}

	@Override
	public Object visit(PropertyIsBetween filter, Object extraData) {
		// There is no "BETWEEN" in MarkLogic. Just delegate to GTE and LTE.
		Expression prop = filter.getExpression();
		Expression lowerBound = filter.getLowerBoundary();
		Expression upperBound = filter.getUpperBoundary();
		PropertyIsGreaterThanOrEqualTo gte = ff.greaterOrEqual(prop, lowerBound);
		PropertyIsLessThanOrEqualTo lte = ff.lessOrEqual(prop, upperBound);
		And and = ff.and(gte, lte);
		return and.accept(this, extraData);
	}

	@Override
	public Object visit(PropertyIsEqualTo filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(PropertyIsGreaterThan filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(PropertyIsLessThan filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(PropertyIsLike filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(PropertyIsNull filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(PropertyIsNil filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(BBOX filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Beyond filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Contains filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Crosses filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Disjoint filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(DWithin filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Equals filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Intersects filter, Object extraData) {
		// create the Intersects filter with a PropertyName that doesn't matter and a
		// geometry
		// TODO: should this be pulled out similar to FilterToSql?
		Expression e1 = filter.getExpression1();
		Expression e2 = filter.getExpression2();

		// swap operands if necessary
		if (e1 instanceof Literal && e2 instanceof PropertyName) {
			e1 = (PropertyName) filter.getExpression2();
			e2 = (Literal) filter.getExpression1();
		}

		// we don't actually care what the property name is in this case - all we want
		// is the geometry
		// TODO: we probably want to pull this logic into its own class
		Geometry geom = e2.evaluate(null, Geometry.class);
		Coordinate[] coords = geom.getCoordinates();
		int pointCount = geom.getNumPoints();
		Point[] points = new Point[pointCount];
		for (int i = 0; i < pointCount; i++) {
			// TODO: this assumes Coordinate objects were created with lat/long - confirm
			points[i] = qb.point(coords[i].x, coords[i].y);
		}

		StructuredQueryDefinition query = qb.geospatial(
				qb.geoRegionPath(qb.pathIndex("/metadata/ctsRegion"), StructuredQueryBuilder.CoordinateSystem.WGS84),
				GeospatialOperator.INTERSECTS,
				qb.polygon(points));
		return query;

	}

	@Override
	public Object visit(Overlaps filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Touches filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Within filter, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(After after, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(AnyInteracts anyInteracts, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Before before, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Begins begins, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(BegunBy begunBy, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(During during, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(EndedBy endedBy, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Ends ends, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(Meets meets, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(MetBy metBy, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(OverlappedBy overlappedBy, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(TContains contains, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(TEquals equals, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visit(TOverlaps contains, Object extraData) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

}
