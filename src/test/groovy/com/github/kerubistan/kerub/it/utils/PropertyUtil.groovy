package com.github.kerubistan.kerub.it.utils

import cucumber.api.DataTable

class PropertyUtil {
	static Map<String, String> toMap(DataTable dataTable) {
		final Map<String, String> ret = new HashMap<>()
		def list = dataTable.raw().subList(1, dataTable.raw().size())
		for(def row in list) {
			ret.put(row.get(0), row.get(1))
		}
		return ret
	}
}
