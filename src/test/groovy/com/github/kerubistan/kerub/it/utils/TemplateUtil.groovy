package com.github.kerubistan.kerub.it.utils

import cucumber.api.DataTable
import gherkin.formatter.model.DataTableRow

class TemplateUtil {
	static Map<String, Object> convert(DataTable table) {
		Map<String, Object> ret = new HashMap<>()
		for(DataTableRow row : table.gherkinRows) {
			def format = row.cells[1]
			def value = null
			if("csv".equals(format)) {
				value = row.cells[2].split(",")
			} else {
				value = row.cells[2]
			}
			ret.put(row.cells[0], value)
		}
		return ret
	}
}
