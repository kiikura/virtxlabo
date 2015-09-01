package io.vertx.example.core.http.websockets;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.example.util.Runner;

public class Server extends AbstractVerticle {
	final Pattern sessionUrlPattern = Pattern.compile("/session/(\\w+)");

	// Convenience method so you can run it in your IDE
	public static void main(String[] args) {
		Runner.runExample(Server.class);
	}

	@Override
	public void start() throws Exception {
		final Logger logger = LoggerFactory.getLogger(Server.class);
		logger.info("start");
		final EventBus eventBus = vertx.eventBus();
		vertx.createHttpServer().websocketHandler(ws -> {
			logger.info("websocket:" + ws.path());
			final Matcher m = sessionUrlPattern.matcher(ws.path());
			if (!m.matches()) {
				ws.reject();
				return;
			}

			final String sessionRoom = m.group(1);
			final String id = ws.binaryHandlerID();

			logger.info("registering connection with id: " + id + " from session-room: " + sessionRoom);
			vertx.sharedData().getLocalMap("session.room." + sessionRoom).put(id, id);

			ws.closeHandler(new Handler<Void>() {
				@Override
				public void handle(final Void event) {
					logger.info("un-registering connection with id: " + id + " from session-room: " + sessionRoom);
					vertx.sharedData().getLocalMap("session.room." + sessionRoom).remove(id);
				}
			});

			ws.handler(buf -> {
				logger.info(String.format("%s: in=%s", id, buf));
				for(Object hid : vertx.sharedData().getLocalMap("session.room." + sessionRoom).keySet()){
					if(!id.equals(hid)){
						eventBus.send((String)hid, buf);
					}
				}

			});
		}).requestHandler(req -> {
			if (req.uri().equals("/"))
				req.response().sendFile("ws.html");
		}).listen(8080);
	}
}
