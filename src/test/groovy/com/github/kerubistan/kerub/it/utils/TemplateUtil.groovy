package com.github.kerubistan.kerub.it.utils

import io.cucumber.datatable.DataTable


class TemplateUtil {
	static Map<String, Object> convert(DataTable table) {
		Map<String, Object> ret = new HashMap<>()
		for (List<String> row : table.cells()) {
			def format = row[1]
			def value = null
			if ("csv".equals(format)) {
				value = row[2].split(",")
			} else {
				value = row[2]
			}
			ret.put(row[0], value)
		}
		return ret
	}
}
