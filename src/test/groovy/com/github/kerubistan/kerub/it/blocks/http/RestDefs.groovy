package com.github.kerubistan.kerub.it.blocks.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kerubistan.kerub.it.blocks.tempdata.TempDefs
import cucumber.api.Scenario
import cucumber.api.java.Before
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

}
