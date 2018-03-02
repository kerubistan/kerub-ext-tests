package com.github.kerubistan.kerub.it.blocks.hairy

import cucumber.api.java.en.Given

class WaitDefs {
	@Given("we wait (\\d+) seconds")
	def waitSome(Integer seconds) {
		Thread.sleep(seconds * 1000)
	}
}
