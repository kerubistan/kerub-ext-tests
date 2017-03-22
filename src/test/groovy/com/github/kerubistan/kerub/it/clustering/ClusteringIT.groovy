package com.github.kerubistan.kerub.it.clustering

import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith


@RunWith(Cucumber)
@CucumberOptions(
		features = [
				"classpath:com/github/kerubistan/kerub/it/clustering/clustering.feature"
		],
		glue = [
				"classpath:com.github.kerubistan.kerub.it.clustering",
				"classpath:com.github.kerubistan.kerub.it.blocks.virt",
				"classpath:com.github.kerubistan.kerub.it.blocks.exec",
				"classpath:com.github.kerubistan.kerub.it.blocks.pack",
				"classpath:com.github.kerubistan.kerub.it.blocks.http"
		],
		plugin = [
				"pretty", "html:build/reports/cucumber/cluster"
		]
)
class ClusteringIT {}
