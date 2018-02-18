package com.github.kerubistan.kerub.it.torture

import com.github.kerubistan.kerub.it.blocks.http.Clients
import com.github.kerubistan.kerub.it.blocks.http.HttpDefs
import cucumber.api.Scenario
import cucumber.api.java.Before
import cucumber.api.java.en.Then
import org.apache.http.HttpResponse
import org.junit.Assert

class TortureDefs {

	Scenario scenario

	@Before
	setScenario(Scenario scenario) {
		this.scenario = scenario
	}

	List<UUID> virtualNetworks = new ArrayList<>()
	List<UUID> virtualDisks = new ArrayList<>()
	List<UUID> virtualMachies = new ArrayList<>()

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
			def response = clients.getClient(sessionNr.toString()).execute(
					http.putSilent("s/r/vnet", """{
						  "@type" : "vnet",
						  "id" : "${id}",
						  "expectations" : [ ],
						  "name" : "virtual network-$it",
						  "owner" : null
						} """)
			)
			response.getEntity().content.close()

			Assert.assertEquals(200, response.statusLine.statusCode)

			if(it % 100 == 0) {
				scenario.write("${new Date()}: $it - last 1000 took ${System.currentTimeMillis() - chunkStart} ms")
				chunkStart = System.currentTimeMillis()
			}

		})

		scenario.write("Finished write: ${new Date()}")
		scenario.write("total time: ${System.currentTimeMillis() - start}")

		virtualNetworks = results
	}

	@Then("session (\\d+): user can read the virtual networks in random order (\\d+) times")
	def verifyReadVirtualNetworks(String sessionNr, int iterations) {
		readRandomOrder(sessionNr, iterations, virtualNetworks, "s/r/vnet")
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
			def response = clients.getClient(sessionNr.toString()).execute(
					http.putSilent("s/r/virtual-storage", """{
						  "@type" : "virtual-storage",
						  "id" : "${id}",
						  "expectations" : [ ],
						  "size" : ${4096 * 1024 * 1024},
						  "name" : "virtual disk-$it",
						  "owner" : null
						} """)
			)

			response.getEntity().content.close()
			Assert.assertEquals(200, response.statusLine.statusCode)

			if(it % 100 == 0) {
				scenario.write("${new Date()}: $it - last 1000 took ${System.currentTimeMillis() - chunkStart} ms")
				chunkStart = System.currentTimeMillis()
			}

		})

		scenario.write("Finished write: ${new Date()}")
		scenario.write("total time: ${System.currentTimeMillis() - start}")

		virtualNetworks = results
	}

	@Then("session (\\d+): user can read the virtual disks in random order (\\d+) times")
	def verifyReadVirtualDisks(String sessionNr, int iterations) {
		readRandomOrder(sessionNr, iterations, virtualNetworks, "s/r/virtual-storage")
	}

	@Then("session (\\d+): user can create virtual machines (\\d+) times")
	def verifyCreateVirtualMachines(String sessionNr, int iterations) {
	}

	@Then("session (\\d+): user can read the virtual machines in random order (\\d+) times")
	def verifyReadVirtualMachines(String sessionNr, int iterations) {
		readRandomOrder(sessionNr, iterations, virtualMachies, "s/r/vm")
	}

	private def readRandomOrder(String sessionNr, int iterations, List<UUID> ids, String uri) {
		def http = HttpDefs.instance.get()
		def clients = Clients.instance.get()
		def randomOrder = new ArrayList<>(ids)
		Collections.shuffle(randomOrder)
		scenario.write("Starting reads: ${new Date()}")
		(1..iterations).forEach {
			def start = System.currentTimeMillis()
			randomOrder.forEach {

				def response = clients.getClient(sessionNr).execute(
						http.getSilent("$uri/$it")
				)
				response.entity.getContent().close()

				Assert.assertEquals(200, response.statusLine.statusCode)

			}
			scenario.write("iteration $it took ${System.currentTimeMillis() - start}")
		}
		scenario.write("Finsihed read: ${new Date()}")
	}

	private def write(String sessionNr, Integer cnt, String uri, Closure fn) {
		List<UUID> results = new ArrayList<>(cnt)

		def start = System.currentTimeMillis()
		def http = HttpDefs.instance.get()
		def clients = Clients.instance.get()
		scenario.write("Starting write: ${new Date()}")

		def chunkStart = start
		(1..cnt).forEach({
			UUID id = UUID.randomUUID()
			results.add(id)
			clients.getClient(sessionNr.toString()).execute(
					http.putSilent(uri, fn())
			).getEntity().content.close()

			if(it % 100 == 0) {
				scenario.write("${new Date()}: $it - last 1000 took ${System.currentTimeMillis() - chunkStart} ms")
				chunkStart = System.currentTimeMillis()
			}

		})

		scenario.write("Finished write: ${new Date()}")
		scenario.write("total time: ${System.currentTimeMillis() - start}")

	}

}
