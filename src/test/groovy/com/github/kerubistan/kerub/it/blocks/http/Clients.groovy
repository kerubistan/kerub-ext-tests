package com.github.kerubistan.kerub.it.blocks.http

import cucumber.api.java.After
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.Assert
import org.junit.Before

class Clients {

	def private clients = new HashMap<String, Tuple>()

	static instance = new InheritableThreadLocal<Clients>() {
		@Override
		protected Clients initialValue() {
			return new Clients()
		}
	}

	HttpClient getClient(String clientId) {
		def client = clients.get(clientId)
		if(client == null) {
			def httpClient = HttpClientBuilder.create()
					.setUserAgent("Kerub External Tests")
					.setDefaultRequestConfig(
							RequestConfig.custom()
									.setConnectTimeout(100)
									.setSocketTimeout(200000)
									.setConnectionRequestTimeout(100000)
									.build()
					)
					.build()
			client = new Tuple(httpClient, null)
			clients.put(clientId, client)
		}
		return client[0] as HttpClient
	}

	String getHttpSessionId(String clientId) {
		def client = clients.get(clientId)
		if(client == null) {
			return null
		}
		return client[1] as String
	}

	void setHttpSessionId(String clientId, String sessionId) {
		def client = clients.get(clientId)
		Assert.assertNotNull(client)
		clients.put(clientId, new Tuple(client.get(0), sessionId))
	}

	String getHttpSessionId() {
		def client = clients.get(clientId)
		return client.get(1) as String
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
