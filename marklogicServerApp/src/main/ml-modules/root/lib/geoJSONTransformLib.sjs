'use strict';
const geo = require('/MarkLogic/geospatial/geospatial');

function wrapGeoJSON(geoJSON) {
	var geoJSONObject;
	if (xdmp.nodeKind(geoJSON) == 'document') {
		geoJSONObject = geoJSON.toObject();
	} else {
		geoJSONObject = geoJSON;
    }

	if (!(geoJSONObject.hasOwnProperty("properties") && geoJSONObject.hasOwnProperty("geometry"))) {
		//it's not really geoJSON, so just return the original input
		return geoJSON;
	}

	var ctsGeometries = geo.parse(geoJSONObject.geometry);
	var centroids = [];
	var boxWest = null;
	var boxSouth = null;
	var boxNorth = null;
	var boxEast = null;

	for (const g of ctsGeometries) {
		centroids.push(geo.approxCenter(g));

		try {
            let box = geo.boundingBoxes(g, "box-percent=0");
            let w = xs.float(cts.boxWest(box));
            let s = xs.float(cts.boxSouth(box));
            let n = xs.float(cts.boxNorth(box));
            let e = xs.float(cts.boxEast(box));

            if (boxWest == null || w < boxWest) boxWest = w;
            if (boxSouth == null || s < boxSouth) boxSouth = s;
            if (boxNorth == null || n > boxNorth) boxNorth = n;
            if (boxEast == null || e > boxEast) boxEast = e;
		}
		catch (err) {
			xdmp.log("Warning, bounding box calculation failed");
		}
	}

	var boundingBox = {
				"boxWest":boxWest,
			    "boxSouth":boxSouth,
			    "boxNorth":boxNorth,
			    "boxEast":boxEast
			};

	var envelope = {
		geoJSON:geoJSONObject,
		metadata: {}
	};

	if (ctsGeometries) {envelope.metadata.ctsRegions = ctsGeometries;}
	if (centroids) {envelope.metadata.centroids = centroids;}
	if (boundingBox) {envelope.metadata.bbox = boundingBox;}

	return xdmp.unquote(xdmp.quote(envelope));
};

function mlcpTransform(content, context) {
	if (xdmp.nodeKind(content.value) == 'document' &&
      content.value.documentFormat == 'JSON') {
		content.value = wrapGeoJSON(content.value);
	}
	return content;
};

exports.wrapGeoJSON = wrapGeoJSON;
exports.transform = mlcpTransform;