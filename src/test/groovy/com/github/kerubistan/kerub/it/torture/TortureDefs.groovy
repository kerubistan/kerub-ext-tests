package com.github.kerubistan.kerub.it.torture

import com.github.kerubistan.kerub.it.blocks.http.Clients
import com.github.kerubistan.kerub.it.blocks.http.HttpDefs
import com.github.kerubistan.kerub.it.utils.TestUtils
import cucumber.api.Scenario
import cucumber.api.java.Before
import cucumber.api.java.en.Then

class TortureDefs {

	Scenario scenario

	@Before
	setScenario(Scenario scenario) {
		this.scenario = scenario
	}

	List<UUID> virtualNetworks = new ArrayList<>()

	@Then("session (\\d+): user can create (\\d+) virtual networks")
	def verifyCreateVirtualNetworks(String sessionNr, int iterations) {

		List<UUID> results = new ArrayList<>(iterations)

		def start = System.currentTimeMillis()
		def http = HttpDefs.instance.get()
		def clients = Clients.instance.get()
		scenario.write("Starting write: ${new Date()}")

		def chunkStart = start
		(1..iterations).forEach({
			UUID id = UUID.randomUUID()
			results.add(id)
			clients.getClient(sessionNr.toString()).execute(
					http.putSilent("s/r/vnet", """{
						  "@type" : "vnet",
						  "id" : "${id}",
						  "expectations" : [ ],
						  "name" : "virtual network-$it",
						  "owner" : null
						} """)
			).getEntity().content.close()

			if(it % 100 == 0) {
				scenario.write("${new Date()}: $it - last 1000 took ${System.currentTimeMillis() - chunkStart} ms")
				chunkStart = System.currentTimeMillis()
			}

		})

		scenario.write("Finished write: ${new Date()}")
		scenario.write("total time: ${System.currentTimeMillis() - start}")

		virtualNetworks = results
	}

	@Then("session (\\d+): user can read the (\\d+) virtual networks in random order")
	def verifyReadVirtualNetworks(String sessionNr, int iterations) {

		def http = HttpDefs.instance.get()
		def clients = Clients.instance.get()
		def randomOrder = new ArrayList<>(virtualNetworks)
		Collections.shuffle(randomOrder)
		randomOrder.forEach {
			clients.getClient(sessionNr).execute(
					http.get("s/r/vnet/$it")
			)
		}

	}

	@Then("session (\\d+): user can create (\\d+) virtual disks")
	def verifyCreateVirtualDisks(int sessionNr, int iterations) {

		List<UUID> results = new ArrayList<>(iterations)

		def start = System.currentTimeMillis()
		def http = HttpDefs.instance.get()
		def clients = Clients.instance.get()
		scenario.write("Starting write: ${new Date()}")

		def chunkStart = start
		(1..iterations).forEach({
			UUID id = UUID.randomUUID()
			results.add(id)
			clients.getClient(sessionNr.toString()).execute(
					http.putSilent("s/r/virtual-storage", """{
						  "@type" : "virtual-storage",
						  "id" : "${id}",
						  "expectations" : [ ],
						  "name" : "virtual disk-$it",
						  "owner" : null
						} """)
			).getEntity().content.close()

			if(it % 100 == 0) {
				scenario.write("${new Date()}: $it - last 1000 took ${System.currentTimeMillis() - chunkStart} ms")
				chunkStart = System.currentTimeMillis()
			}

		})

		scenario.write("Finished write: ${new Date()}")
		scenario.write("total time: ${System.currentTimeMillis() - start}")

		virtualNetworks = results
	}

	@Then("session (\\d+): user can read the (\\d+) virtual disks in random order")
	def verifyReadVirtualDisks(String sessionNr, int iterations) {
		def http = HttpDefs.instance.get()
		def clients = Clients.instance.get()
		def randomOrder = new ArrayList<>(virtualNetworks)
		Collections.shuffle(randomOrder)
		randomOrder.forEach {
			clients.getClient(sessionNr).execute(
					http.get("s/r/virtual-storage/$it")
			)
		}
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
