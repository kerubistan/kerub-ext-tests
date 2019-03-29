package com.github.kerubistan.kerub.it.blocks.wip

import cucumber.api.PendingException
import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

Then(~"I have to finish this story") {
	throw new PendingException("this story is unfinished")
}
