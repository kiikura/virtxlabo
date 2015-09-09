package ilabo.rtunnel.agent;

import static java.util.logging.Level.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * クライアント側でトンネルINする人 Listenポートごとにインスタンス化される感じ
 * 
 * @author ken
 *
 */
public class ClientAgent implements TunnelHandler {
	private static final Logger LOGGER = Logger.getLogger(ClientAgent.class.getName());

	public static void main(String[] args) throws Exception {
		LOGGER.info("ClientAgent start");
		ClientAgent agent = new ClientAgent();
		agent.open();
	}

	private ServerSocket socket = null;
	private WebsocketTunnelClient tunnel = null;
	private transient byte index = 0;

	private ConcurrentHashMap<Byte, TCPChannel> sessions = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Byte, AtomicInteger> handShakeMonitors = new ConcurrentHashMap<>();

	ClientAgent() throws IOException {
		this.socket = new ServerSocket(5088);
		this.socket.setReuseAddress(true);
		this.tunnel = new WebsocketTunnelClient("ws://localhost:8080/session/hoge");
		this.tunnel.connect(this);
	}

	public void open() throws IOException {

		try {
			while (true) {
				final Socket s = socket.accept();
				final byte sid = ++index;
				final String tname = "channel from " + s.getRemoteSocketAddress() + ":" + s.getPort();
				TCPChannel channel = new TCPChannel(s, sid, tunnel);
				sessions.put(sid, channel);
				Thread t = new Thread(channel, tname);
				t.start();

			}
		} finally {
			socket.close();
		}

	}

	@Override
	public void receiveOpen(byte sid) {
		LOGGER.info("receiveOpen: sid=" + sid);
		TCPChannel channel = sessions.get(sid);
		if (channel != null) {
			channel.markOpened();
		}else{
			LOGGER.warning("channel not exists: sid = " + sid);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ilabo.rtunnel.agent.TunnelHandler#receiveData(byte, byte[])
	 */
	@Override
	public void receiveData(byte sid, byte[] buf, int offset, int length) {
		LOGGER.info("receiveData: sid=" + sid);
		TCPChannel tcpChannel = this.sessions.get(sid);
		try {
			if (tcpChannel == null) {
				tunnel.sendClose(sid);
				return;
			}

			tcpChannel.write(buf, offset, length);
		} catch (IOException e) {
			try {
				tunnel.sendClose(sid);
			} catch (IOException e1) {
			}
			if(tcpChannel != null){
				tcpChannel.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ilabo.rtunnel.agent.TunnelHandler#receiveClose(byte)
	 */
	@Override
	public void receiveClose(byte sid) {
		TCPChannel tcpChannel = this.sessions.get(sid);
		if (tcpChannel != null) {
			tcpChannel.close();
			this.sessions.remove(sid);
		}
	}

	class TCPChannel implements Runnable {
		private final Logger LOGGER = Logger.getLogger(TCPChannel.class.getName());
		private Socket socket;
		private byte sid;
		private WebsocketTunnelClient tunnel;
		private InputStream in;
		private OutputStream out;
		private transient boolean opened = false;
		
		TCPChannel(Socket socket, byte sid, WebsocketTunnelClient tunnel) throws IOException {
			this.socket = socket;
			this.sid = sid;
			this.tunnel = tunnel;

			this.in = socket.getInputStream();
			this.out = socket.getOutputStream();
		}

		boolean isOpened(){
			return this.opened;
		}
		void markOpened() {
			LOGGER.info("markOpened()");
			synchronized(this){
				this.opened = true;
				this.notifyAll();// ハンドシェーク完了通知
			}
		}

		void write(byte[] buf, int offset, int length) throws IOException {
			out.write(buf, offset, length);
			out.flush();
		}

		@Override
		public void run() {

			try {
				LOGGER.info("client openChannel(): sid=" + sid);
				

				// wait for handshake
				synchronized (this) {
					tunnel.openChannel(sid);
					LOGGER.info("waiting for open");
					this.wait();// handshake timeout
				}
				if (!this.isOpened()) {
					LOGGER.info("open handshake is timeout.");
					close();
					return;
				}

				LOGGER.info("channel: sid=" + sid + ", opened");

				// req copier
				InputStream in = socket.getInputStream();
				int len = 0;
				byte[] buf = new byte[4096];
				while ((len = in.read(buf)) > 0) {
					tunnel.write(sid, buf, 0, len);
				}

				tunnel.sendClose(sid);

			} catch (IOException e) {
				// this happens if the socket connection is terminated abruptly.
				LOGGER.log(FINE, "Port forwarding session was shut down abnormally", e);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				LOGGER.log(FINE, "Port forwarding session was shut down abnormally", e);
			}

			close();
		}

		public void close() {
			try {
				in.close();
				out.close();
				socket.close();
			} catch (Exception e) {
			}
			sessions.remove(sid);
		}
	}

}
