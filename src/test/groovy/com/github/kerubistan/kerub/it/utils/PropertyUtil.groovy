package com.github.kerubistan.kerub.it.utils

import io.cucumber.datatable.DataTable


class PropertyUtil {
	static Map<String, String> toMap(DataTable dataTable) {
		final Map<String, String> ret = new HashMap<>()
		def list = dataTable.cells().subList(1, dataTable.cells().size())
		for(def row in list) {
			ret.put(row.get(0), row.get(1))
		}
		return ret
	}
}
