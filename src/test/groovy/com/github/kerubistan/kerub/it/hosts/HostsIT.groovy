package com.github.kerubistan.kerub.it.hosts

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith

@RunWith(Cucumber)
@CucumberOptions(
		features = [
				"classpath:com/github/kerubistan/kerub/it/hosts/hosts.feature",
				"classpath:com/github/kerubistan/kerub/it/hosts/updated-all.feature"
		],
		glue = [
				"classpath:com.github.kerubistan.kerub.it.clustering",
				"classpath:com.github.kerubistan.kerub.it.blocks.virt",
				"classpath:com.github.kerubistan.kerub.it.blocks.exec",
				"classpath:com.github.kerubistan.kerub.it.blocks.hairy",
				"classpath:com.github.kerubistan.kerub.it.blocks.pack",
				"classpath:com.github.kerubistan.kerub.it.blocks.http",
				"classpath:com.github.kerubistan.kerub.it.blocks.tempdata",
				"classpath:com.github.kerubistan.kerub.it.blocks.websocket"
		],
		plugin = [
				"pretty", "html:build/reports/cucumber/hosts", "json:build/reports/cucumber/hosts.json"
		]
)

class HostsIT {}
