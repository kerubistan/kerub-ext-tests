package com.github.kerubistan.kerub.it.torture

import com.github.kerubistan.kerub.it.blocks.http.Clients
import com.github.kerubistan.kerub.it.blocks.http.HttpEnvironment
import com.github.kerubistan.kerub.it.blocks.scenario.ScenarioAccess
import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks
import org.junit.Assert

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

class TortureEnvironment {
	List<UUID> virtualNetworks = new ArrayList<>()
	List<UUID> virtualDisks = new ArrayList<>()
	List<UUID> virtualMachies = new ArrayList<>()

	void readRandomOrder(String sessionNr, int iterations, List<UUID> ids, String uri) {
		def clients = Clients.instance.get()
		def randomOrder = new ArrayList<>(ids)
		Collections.shuffle(randomOrder)
		ScenarioAccess.get().write("Starting reads: ${new Date()}")
		(1..iterations).forEach {
			def start = System.currentTimeMillis()
			randomOrder.forEach {

				def response = clients.getClient(sessionNr).execute(
						HttpEnvironment.instance.get().getSilent("$uri/$it")
				)
				response.entity.getContent().close()

				Assert.assertEquals(200, response.statusLine.statusCode)

			}
			ScenarioAccess.get().write("iteration $it took ${System.currentTimeMillis() - start}")
		}
		ScenarioAccess.get().write("Finsihed read: ${new Date()}")
	}

	void write(String sessionNr, Integer cnt, String uri, Closure fn) {
		List<UUID> results = new ArrayList<>(cnt)

		def start = System.currentTimeMillis()
		def clients = Clients.instance.get()
		ScenarioAccess.get().write("Starting write: ${new Date()}")

		def chunkStart = start
		(1..cnt).forEach({
			UUID id = UUID.randomUUID()
			results.add(id)
			clients.getClient(sessionNr.toString()).execute(
					HttpEnvironment.instance.get().putSilent(uri, fn())
			).getEntity().content.close()

			if(it % 100 == 0) {
				ScenarioAccess.get().write("${new Date()}: $it - last 1000 took ${System.currentTimeMillis() - chunkStart} ms")
				chunkStart = System.currentTimeMillis()
			}

		})

		ScenarioAccess.get().write("Finished write: ${new Date()}")
		ScenarioAccess.get().write("total time: ${System.currentTimeMillis() - start}")

	}

}

World {
	new TortureEnvironment()
}

Then(~/^session (\S+): user can create (\d+) virtual networks$/) {
	String sessionNr, int iterations ->

		List<UUID> results = new ArrayList<>(iterations)

		def start = System.currentTimeMillis()
		def clients = Clients.instance.get()
		scenario.write("Starting write: ${new Date()}")

		def chunkStart = start
		def _scenario = scenario
		(1..iterations).forEach({
			UUID id = UUID.randomUUID()
			results.add(id)
			def response = clients.getClient(sessionNr.toString()).execute(
					HttpEnvironment.instance.get().putSilent("s/r/vnet", """{
						  "@type" : "vnet",
						  "id" : "${id}",
						  "expectations" : [ ],
						  "name" : "virtual network-$it",
						  "owner" : null
						} """)
			)
			response.getEntity().content.close()

			Assert.assertEquals(200, response.statusLine.statusCode)

			if (it % 100 == 0) {
				_scenario.write("${new Date()}: $it - last 1000 took ${System.currentTimeMillis() - chunkStart} ms")
				chunkStart = System.currentTimeMillis()
			}

		})

		scenario.write("Finished write: ${new Date()}")
		scenario.write("total time: ${System.currentTimeMillis() - start}")

		virtualNetworks = results
}

Then(~/^session (\S+): user can read the virtual networks in random order (\d+) times$/) {
	String sessionNr, int iterations ->
		readRandomOrder(sessionNr, iterations, virtualNetworks, "s/r/vnet")
}

Then(~/^session (\S+): user can create (\d+) virtual disks$/) {
	int sessionNr, int iterations ->

		List<UUID> results = new ArrayList<>(iterations)

		def start = System.currentTimeMillis()
		def clients = Clients.instance.get()
		scenario.write("Starting write: ${new Date()}")
		def _scenario = scenario

		def chunkStart = start
		(1..iterations).forEach({
			UUID id = UUID.randomUUID()
			results.add(id)
			def response = clients.getClient(sessionNr.toString()).execute(
					HttpEnvironment.instance.get().putSilent("s/r/virtual-storage", """{
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

			if (it % 100 == 0) {
				_scenario.write("${new Date()}: $it - last 1000 took ${System.currentTimeMillis() - chunkStart} ms")
				chunkStart = System.currentTimeMillis()
			}

		})

		scenario.write("Finished write: ${new Date()}")
		scenario.write("total time: ${System.currentTimeMillis() - start}")

		virtualDisks = results
}

Then(~/^session (\S+): user can read the virtual disks in random order (\d+) times$/) {
	String sessionNr, int iterations ->
		readRandomOrder(sessionNr, iterations, virtualDisks, "s/r/virtual-storage")
}

Then(~/^session (\S+): user can create (\d+) virtual machines$/) {
	String sessionNr, int iterations ->
		scenario.write("TODO - not implemented yet")
}

Then(~/^session (\S+): user can read the virtual machines in random order (\\d+) times$/) {
	String sessionNr, int iterations ->
		readRandomOrder(sessionNr, iterations, virtualMachies, "s/r/vm")
}

