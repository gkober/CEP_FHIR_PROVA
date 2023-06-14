/**
 * 
 */
package com.dimse.REST_API;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * @author gerhard This class is meant to send events, generated out of a
 *         prova-service, directly to an attached websocket (so a web-client)
 *         https://docs.oracle.com/javaee/7/tutorial/websocket004.htm
 */

@ServerEndpoint(value = "/DiMSEWebsocket")
public class WebSocketServer{

	public WebSocketServer() {
		System.out.println("Calling the webservice-constructor");
	}

	private static Set<Session> connectedClients = new CopyOnWriteArraySet<>();

	@OnOpen
	public void onOpen(Session session) {
		System.out.println("WebSocket connection opened");
		System.out.println(session.getId());
		WebSocketServer.connectedClients.add(session);
		try {
			session.getBasicRemote().sendText("Hello, WebSocketClient");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@OnMessage
	public void onMessage(String message) {
		System.out.println("Received message: " + message);
		if (connectedClients.size() == 0) {
			System.out.println("no clients for notification connected");
		} else {
			System.out.println("number connectedClients " + connectedClients.size());
			for (Session client : connectedClients) {
				try {
					client.getBasicRemote().sendText(message);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@OnError
	public void onError(Session session, Throwable error) {
		System.out.println("onError happened...");
		error.printStackTrace();
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("Websocket connection closed");
		System.out.println("Sessions before removal of " + session.getId() + "is: " + connectedClients.size());
		connectedClients.remove(session);
		System.out.println("Active Sessions after removal " + connectedClients.size());
		
	}

}
