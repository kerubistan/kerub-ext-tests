package com.github.kerubistan.kerub.it.blocks.http

import cucumber.api.java.After
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.HttpClients
import org.junit.Before

class Clients {

	def clients = new HashMap<String, HttpClient>()

	static instance = new InheritableThreadLocal<Clients>()

	static HttpClient getTestClient(String clientId) {
		return instance.get().getClient(clientId)
	}

	HttpClient getClient(String clientId) {
		def client = clients.get(clientId)
		if(client == null) {
			client = new HttpClients().createDefault()
			clients.put(clientId, client)
		}
		return client
	}

	@After
	void cleanup() {
		instance.set(this)
	}

	@Before
	void setup() {
		instance.remove()
	}

}
