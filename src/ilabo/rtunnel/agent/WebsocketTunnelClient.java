package ilabo.rtunnel.agent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.neovisionaries.ws.client.ProxySettings;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

/**
 * kawasaki-sanのクライアント実装RFC6455対応なのがいい。 WebSocketそのままな感じがいい。httpsプロキシに対応してそう。
 *
 * @author yumi
 *
 */
public class WebsocketTunnelClient {

	static final private Logger L = Logger.getLogger(WebsocketTunnelClient.class.getName());

	private String uri;
	private WebSocket ws = null;

	// private int sendCount=CONTENT_LENGTH;

	public WebsocketTunnelClient(String uri) {
		this.uri = uri;
	}

	// public void setProxy(String host, int port) {
	// this.proxy = new Proxy(host, port);
	// }

	/**
	 * トンネルのソケット接続
	 * 
	 * @throws WSTunnelException
	 */
	public void connect(final TunnelHandler handler) {
		try {
			WebSocketFactory factory = new WebSocketFactory();
			ProxySettings settings = factory.getProxySettings();

			this.ws = factory.createSocket(uri);
			this.ws.getSocket().setTcpNoDelay(true);

			this.ws.addListener(new WebSocketAdapter() {
				@Override
				public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) {

					byte[] tunnelData = frame.getPayload();

					byte command = tunnelData[0];
					byte sid = tunnelData[1];
					L.info(String.format("%d:<--[%d]",sid,command));
					switch (command) {
					case Protocol.TUNNEL_OPEN:
						handler.receiveOpen(sid);
						break;
					case Protocol.TUNNEL_DATA:
						handler.receiveData(sid, tunnelData, 2, frame.getPayloadLength() - 2);
						break;
					case Protocol.TUNNEL_CLOSE:
						handler.receiveClose(sid);
						break;
					}

				}

			});

			this.ws.connect();
		} catch (Exception e) {
			throw new WSTunnelException(e);
		}
	}

	/**
	 * エンドツーエンドのTCP接続。
	 * 
	 * @param i
	 * @throws IOException
	 * @return wait用のObject
	 */
	public void openChannel(byte id) throws IOException {

		byte[] command = new byte[] { Protocol.TUNNEL_OPEN, id };

		ws.sendBinary(command);
		
		L.info(String.format("%d:-->[%d]",command[1],command[0]));
	}

	public void sendDisconnect(byte id) throws IOException {
		byte[] command = new byte[] { Protocol.TUNNEL_DISCONNECT, id };

		ws.sendBinary(command);
		L.info(String.format("%d:-->[%d]",command[1],command[0]));
	}

	public void sendClose(byte id) throws IOException {
		byte[] command = new byte[] { Protocol.TUNNEL_CLOSE, id };
		ws.sendBinary(command);
		L.info(String.format("%d:-->[%d]",command[1],command[0]));
	}

	public void write(byte id, byte[] data, int offset, int length) throws IOException {
		// セグメンテーションは未考慮（tcpが頑張る想定）
		byte[] command = new byte[] { Protocol.TUNNEL_DATA, id };
		ByteBuffer buf = ByteBuffer.allocate(length + command.length);
		buf.put(command);
		buf.put(data, offset, length);
		ws.sendBinary(buf.array());
		L.info(String.format("%d:-->[%d]",command[1],command[0]));
	}

}
