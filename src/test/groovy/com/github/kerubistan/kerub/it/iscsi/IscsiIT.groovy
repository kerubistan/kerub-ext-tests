package com.github.kerubistan.kerub.it.iscsi

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

@RunWith(Cucumber)
@CucumberOptions(
		features = [
				"classpath:com/github/kerubistan/kerub/it/iscsi/iscsi.feature"
		],
		glue = [
				"classpath:com.github.kerubistan.kerub.it.clustering",
				"classpath:com.github.kerubistan.kerub.it.blocks.scenario",
				"classpath:com.github.kerubistan.kerub.it.blocks.virt",
				"classpath:com.github.kerubistan.kerub.it.blocks.exec",
				"classpath:com.github.kerubistan.kerub.it.blocks.hairy",
				"classpath:com.github.kerubistan.kerub.it.blocks.pack",
				"classpath:com.github.kerubistan.kerub.it.blocks.http",
				"classpath:com.github.kerubistan.kerub.it.blocks.tempdata",
				"classpath:com.github.kerubistan.kerub.it.blocks.websocket",
				"classpath:com.github.kerubistan.kerub.it.blocks.wip"
		],
		plugin = [
				"pretty", "html:build/reports/cucumber/iscsi", "json:build/reports/cucumber/iscsi.json"
		]
)
class IscsiIT {}
