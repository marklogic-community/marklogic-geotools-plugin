package com.marklogic.geotools.basic;

import java.io.IOException;

import org.geotools.data.jdbc.FilterToSQL;

public class FilterToMarkLogicScalar extends FilterToSQL {

	public String getEncodedFilter() throws IOException {
		out.close();
		return out.toString();
	}

}
