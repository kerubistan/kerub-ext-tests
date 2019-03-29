package com.github.kerubistan.kerub.it.blocks.scenario

import cucumber.api.Scenario
import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)


class ScenarioEnvironment {
	Scenario scenario
}

World {
	new ScenarioEnvironment()
}

Before { Scenario scenario ->
	setScenario(scenario)
	ScenarioAccess.scenarioEnvironment.set(scenario)
}

After {
	ScenarioAccess.scenarioEnvironment.remove()
}