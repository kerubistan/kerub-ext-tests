package com.github.kerubistan.kerub.it.blocks.wip

import cucumber.api.PendingException
import cucumber.api.java.en.Then

class WorkDefs {
	@Then("I have to finish this story")
	void pending() {
		throw new PendingException("this story is unfinished")
	}
}
