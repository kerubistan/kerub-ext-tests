package com.github.kerubistan.kerub.it.blocks.websocket

import com.github.kerubistan.kerub.it.blocks.http.Clients
import cucumber.api.groovy.EN
import cucumber.api.groovy.Hooks
import org.eclipse.jetty.util.HttpCookieStore
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.client.WebSocketClient

this.metaClass.mixin(Hooks)
this.metaClass.mixin(EN)

class WebSocketEnvironment {
	Map<String, Tuple> clients = new HashMap<>()
}

World {
	new WebSocketEnvironment()
}

After {
	for(kv in clients) {
		kv.key // WTF?
	}
}

Then(~/^session (\S+): user can connect to websocket$/) {
	String sessionId ->
		WebSocketClient client = new WebSocketClient()
		client.start()
		client.cookieStore = new HttpCookieStore()
		def appRoot = applicationRoot

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

Then(~/^session (\S+): websocket subscribe to (\S+)$/) {
	String sessionId, String channel ->
		(clients.get(sessionId).get(1) as Session).remote.sendString(
				""" { "@type" : "subscribe", "channel" : "$channel"} """
		)
}
