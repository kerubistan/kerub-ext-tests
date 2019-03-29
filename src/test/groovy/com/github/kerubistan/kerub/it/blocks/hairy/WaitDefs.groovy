package com.github.kerubistan.kerub.it.blocks.hairy

import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

Given(~/^we wait (\d+) seconds$/) {
	int seconds ->
		Thread.sleep(seconds * 1000)
}
