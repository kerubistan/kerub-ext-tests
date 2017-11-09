package com.github.kerubistan.kerub.it.blocks.websocket

import com.github.kerubistan.kerub.it.blocks.http.Clients
import com.github.kerubistan.kerub.it.blocks.http.HttpDefs
import cucumber.api.Scenario
import cucumber.api.java.After
import cucumber.api.java.Before
import cucumber.api.java.en.Then
import org.eclipse.jetty.util.HttpCookieStore
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.client.WebSocketClient

class WebSocketDefs {

	Scenario scenario = null
	Map<String, Tuple> clients = new HashMap<>()

	@Before
	setScenario(Scenario scenario) {
		this.scenario = scenario
	}

	@After
	cleanup() {
		for(kv in clients) {
			kv.key
		}
	}

	@Then("session (\\S+): user can connect to websocket")
	verifyWebsocketConnection(String sessionId) {
		WebSocketClient client = new WebSocketClient()
		client.start()
		client.cookieStore = new HttpCookieStore()
		def appRoot = HttpDefs.instance.get().applicationRoot

		client.cookieStore.add(
				new URI(appRoot),
				new HttpCookie("JSESSIONID", Clients.instance.get().getHttpSessionId(sessionId)
				)
		)
		def wsUrl = appRoot
				.replace("http://", "ws://")
				.replace("https://", "wss://")
				.toString() + "/ws"
		scenario.write("connecting to $wsUrl")
		def listener = new Listener()
		def session = client.connect(listener, new URI(wsUrl)).get()
		clients.put(sessionId, new Tuple(client, session, listener))
	}

	@Then("session (\\S+): websocket subscribe to (\\S+)")
	subscribeToChannel(String sessionId, String channel) {
		(clients.get(sessionId).get(1) as Session).remote.sendString(
				""" { "@type" : "subscribe", "channel" : "$channel"} """
		)
	}

}
