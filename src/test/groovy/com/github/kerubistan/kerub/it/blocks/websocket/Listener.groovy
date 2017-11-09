package com.github.kerubistan.kerub.it.blocks.websocket

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.WebSocket

@WebSocket
class Listener {
	@OnWebSocketConnect
	void connect(Session session) {

	}
	@OnWebSocketClose
	void close(int code, String msg) {

	}
}
