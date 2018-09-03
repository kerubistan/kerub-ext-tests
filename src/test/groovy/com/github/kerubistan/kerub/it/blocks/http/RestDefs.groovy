package com.github.kerubistan.kerub.it.blocks.http

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.TextNode
import com.github.kerubistan.kerub.it.blocks.tempdata.TempDefs
import com.github.kerubistan.kerub.it.sizes.Sizes
import cucumber.api.DataTable
import cucumber.api.Scenario
import cucumber.api.java.Before
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import org.junit.Assert

class RestDefs {
	Scenario scenario = null

	@Before
	setScenario(Scenario scenario) {
		this.scenario = scenario
	}

	@Then("session (\\S+): user can download kerub controller public ssh key to temp (\\S+)")
	void downloadAndSetSshKey(String sessionId, String tempName) {
		def client = Clients.instance.get().getClient(sessionId)
		def response = client.execute(HttpDefs.instance.get().get("/s/r/host/helpers/controller-pubkey"))
		Assert.assertEquals(200, response.getStatusLine().statusCode)
		def text = response.entity.content.getText().trim().with { it.substring(0, it.indexOf("#")) }
		scenario.write("controller public key:\n $text")
		TempDefs.instance.get().setData(tempName, text)
	}

	@Then("session (\\S+): user can fetch public key for (\\S+) into temp (\\S+)")
	void getPublicKey(String sessionId, String address, String tempName) {
		def client = Clients.instance.get().getClient(sessionId)
		def response = client.execute(HttpDefs.instance.get().get("/s/r/host/helpers/pubkey?address=$address"))

		Assert.assertEquals(200, response.getStatusLine().statusCode)
		def text = response.entity.content.getText()
		scenario.write("public key: $text")
		def tree = new ObjectMapper().readTree(text)
		def pkey = tree.get("fingerprint").toString()

		TempDefs.instance.get().setData(tempName, pkey)
	}

	@Given("session (\\S+): within (\\d+) seconds the host details will be extended with the discovered details:")
	void checkHostDetails(String sessionId, Integer timeoutSecs, DataTable validations) {
		def client = Clients.instance.get().getClient(sessionId)

		def start = System.currentTimeMillis()

		def tree = null
		while(
				System.currentTimeMillis() - start < timeoutSecs * 1000
						&& (tree == null || tree.get("result").any { it.get("capabilities") == null })) {
			def response = client.execute(HttpDefs.instance.get().get("/s/r/host"))
			Assert.assertEquals(200, response.statusLine.statusCode)
			tree = new ObjectMapper().readTree(response.entity.content)
			response.entity.content.close()
		}

		scenario.write("hosts response: ")
		scenario.write(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(tree))
		//TODO: check all requirements in capabilities

		if(System.currentTimeMillis() - start >= timeoutSecs * 1000) {
			throw new Exception("did not make it within $timeoutSecs seconds, probably never will")
		}

	}

	@Then("session (\\S+): user can join host (\\S+) using public key and fingerprint (\\S+) and store ID in temp (\\S+)")
	void joinHostWithPublicKey(String sessionId, String address, String fingerprint, String tempName) {
		def client = Clients.instance.get().getClient(sessionId)
		def hostId = UUID.randomUUID()
		def response = client.execute(HttpDefs.instance.get()
				.put(
				"/s/r/host/join-pubkey",
				"""
				{ 
					"host": {
						"@type": "host",
						"id": "${hostId}",
						"address":"$address",
						"publicKey" : ${TempDefs.instance.get().getData(fingerprint)},
						"dedicated" : true
					},
					"powerManagement" : []
				}"""
				)
		)

		def responseText = response.entity.content.getText("ASCII")
		scenario.write("status:" + response.getStatusLine())
		scenario.write("response:" + responseText)
		Assert.assertEquals(200, response.getStatusLine().statusCode)
		def host = new ObjectMapper().readTree(responseText)

		TempDefs.instance.get().setData(tempName, hostId.toString())
	}

	@Then("session (\\S+): host identified by key (\\S+) should have (\\S+) storage capability registered with size around (\\S+) \\+\\-(\\S+)")
	void verifyStorageCapability(
			String sessionId,
			String hostKey,
			String storageType,
			String sizeEstimate,
			String sizePrecision,
			DataTable props) {
		def capabilities = null
		def attempt = 1
		while(capabilities == null && attempt < 10) {
			def hostId = TempDefs.instance.get().getData(hostKey)
			def client = Clients.instance.get().getClient(sessionId)
			scenario.write("checking /s/r/host/$hostId")
			def response = client.execute(HttpDefs.instance.get().get("/s/r/host/$hostId"))
			def responseText = response.entity.content.getText("ASCII")
			scenario.write("response: ${response.getStatusLine().statusCode}\n $responseText")

			def host = new ObjectMapper().readTree(responseText)
			capabilities = host.get("capabilities")
		}
		Assert.assertNotNull(capabilities)
		def storageCapabilities = capabilities.get("storageCapabilities")
		Assert.assertNotNull(storageCapabilities)

		def storage = storageCapabilities.find {
			def rows = props.raw().subList(1, props.raw().size())
			((it["@type"] as TextNode).textValue() == storageType) && matches(rows, it)
		}
		Assert.assertNotNull(storage)
		Assert.assertEquals(
				Sizes.toSize(sizeEstimate).toDouble(),
				(storage["size"] as LongNode).longValue().toDouble(),
				Sizes.toSize(sizePrecision).toDouble(),
		)
	}

	static boolean matches(List<List<String>> expected, JsonNode actual) {
		for(row in expected) {
			if((actual[row[0]] as TextNode).textValue() != row[1].trim()) {
				return false
			}
		}
		return true
	}
}
