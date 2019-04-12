package com.github.kerubistan.kerub.it.torture

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

@RunWith(Cucumber)
@CucumberOptions(
		features = [
				"classpath:com/github/kerubistan/kerub/it/torture/fileuploads.feature",
				"classpath:com/github/kerubistan/kerub/it/torture/controller-storage-torture.feature"
//				"classpath:com/github/kerubistan/kerub/it/torture/controller-restarts-torture.feature"
		],
		glue = [
				"classpath:com.github.kerubistan.kerub.it.clustering",
				"classpath:com.github.kerubistan.kerub.it.torture",
				"classpath:com.github.kerubistan.kerub.it.blocks.scenario",
				"classpath:com.github.kerubistan.kerub.it.blocks.virt",
				"classpath:com.github.kerubistan.kerub.it.blocks.exec",
				"classpath:com.github.kerubistan.kerub.it.blocks.pack",
				"classpath:com.github.kerubistan.kerub.it.blocks.http",
				"classpath:com.github.kerubistan.kerub.it.blocks.tempdata",
				"classpath:com.github.kerubistan.kerub.it.blocks.websocket"
		],
		plugin = [
				"pretty", "html:build/reports/cucumber/torture", "json:build/reports/cucumber/torture.json"
		]
)

class TortureIT {}
