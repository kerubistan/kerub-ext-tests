package com.github.kerubistan.kerub.it.localstorage

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

@RunWith(Cucumber)
@CucumberOptions(
		features = [
				"classpath:com/github/kerubistan/kerub/it/localstorage/localstorage.feature"
		],
		glue = [
				"classpath:com.github.kerubistan.kerub.it.clustering",
				"classpath:com.github.kerubistan.kerub.it.blocks.virt",
				"classpath:com.github.kerubistan.kerub.it.blocks.exec",
				"classpath:com.github.kerubistan.kerub.it.blocks.hairy",
				"classpath:com.github.kerubistan.kerub.it.blocks.pack",
				"classpath:com.github.kerubistan.kerub.it.blocks.http",
				"classpath:com.github.kerubistan.kerub.it.blocks.tempdata",
				"classpath:com.github.kerubistan.kerub.it.blocks.wip"
		],
		plugin = [
				"pretty", "html:build/reports/cucumber/localstorage", "json:build/reports/cucumber/localstorage.json",
				"pretty:build/reports/cucumber/localstorage/cucumber.txt"
		]
)
class LocalStorageIT {
}
