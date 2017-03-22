package com.github.kerubistan.kerub.it.blocks.pack

import cucumber.api.java.en.Given
import org.slf4j.LoggerFactory

class PackDefs {

	private final static logger = LoggerFactory.getLogger(PackDefs)

	@Given("Kerub \"(.*)\" installed on (\\S+)")
	void installKerubPackageOnNode(String kerubPackName, String nodeAddr) {
		logger.info("TODO: install $kerubPackName on $nodeAddr")
	}
}
