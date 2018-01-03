package com.github.kerubistan.kerub.it.torture

import com.github.kerubistan.kerub.it.utils.TestUtils
import cucumber.api.java.en.Then

class TortureDefs {
	@Then("session (\\d+): user can create (\\d+) virtual networks")
	def verifyCreateVirtualNetworks(int sessionNr, int iterations) {
		(1..iterations).forEach({
			TestUtils.TODO("not finished")
		})
	}

	@Then("session (\\d+): user can read the (\\d+) virtual networks in random order")
	def verifyReadVirtualNetworks(int sessionNr, int iterations) {
		TestUtils.TODO("not finished")
	}

	@Then("session (\\d+): user can create (\\d+) virtual disks")
	def verifyCreateVirtualDisks(int sessionNr, int iterations) {
		(1..iterations).forEach({
			TestUtils.TODO("not finished")
		})
	}

	@Then("session (\\d+): user can read the (\\d+) virtual disks in random order")
	def verifyReadVirtualDisks(int sessionNr, int iterations) {
		TestUtils.TODO("not finished")
	}

	@Then("session (\\d+): user can create (\\d+) virtual machines")
	def verifyCreateVirtualMachines(int sessionNr, int iterations) {
		(1..iterations).forEach({
			TestUtils.TODO("not finished")
		})
	}

	@Then("session (\\d+): user can read the (\\d+) virtual machines in random order")
	def verifyReadVirtualMachines(int sessionNr, int iterations) {
		(1..iterations).forEach({
			TestUtils.TODO("not finished")
		})
	}

}
