package com.github.kerubistan.kerub.it.blocks.scenario

import cucumber.api.Scenario

class ScenarioAccess {
	public static ThreadLocal<Scenario> scenarioEnvironment = new InheritableThreadLocal<>();

	static def get() {
		return scenarioEnvironment.get()
	}
}
