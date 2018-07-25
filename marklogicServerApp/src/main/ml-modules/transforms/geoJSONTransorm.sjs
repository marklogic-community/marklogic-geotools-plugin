'use strict';
const transformLib = require('/lib/geoJSONTransformLib.sjs');

function transformGeoJSON(context, params, content) 
{
	xdmp.log(context);
	if (context.inputType.search('json') >= 0) { //work only on JSON docs
		if (context.acceptTypes) { // output transform, return the geoJSON content only
			let obj = content.toObject();
			if (obj.hasOwnProperty("geoJSON"))
				return obj.geoJSON;
			else
				return content;
		}
		else { // input transform
			return transformLib.wrapGeoJSON(content);
		}
	}
	else //leave everything else alone
		return content;
};

exports.transform = transformGeoJSON;

