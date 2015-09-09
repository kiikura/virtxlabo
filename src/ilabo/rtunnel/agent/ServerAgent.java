package ilabo.rtunnel.agent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * サーバ側でプロクシする人 管理サービスに対する直接のTCPクライアント（代理）
 *
 * httptunnelのサーバプロセスと同じような動きをする。 ただしトンネルパスからみてもクライアントなので注意
 *
 * tunnel->tunnel client -> tcp agent client -> target tcp server
 *
 * @author yumi
 *
 */
public class ServerAgent implements TunnelHandler {
	public static void main(String[] args)throws Exception{
		
		ServerAgent s = new ServerAgent();
		
	}
	
	
	private static final Logger LOGGER = Logger.getLogger(ServerAgent.class.getName());
	private String fowardHost = "173.194.117.152";
	private int forwardPort = 80;
	private WebsocketTunnelClient tunnel = null;
	private transient byte index = 0;
	private ConcurrentHashMap<Byte, TCPClient> sessions = new ConcurrentHashMap<>();
	ServerAgent(){
		this.tunnel = new WebsocketTunnelClient("ws://localhost:8080/session/hoge");
		this.tunnel.connect(this);
	}
	
	/* (non-Javadoc)
	 * @see ilabo.rtunnel.agent.TunnelHandler#receiveOpen(byte)
	 */
	@Override
	public void receiveOpen(byte sid){
		LOGGER.info("receiveOpen: sid=" + sid);
		try {
			TCPClient tcpClient = new TCPClient(tunnel, sid, fowardHost, forwardPort);
			tcpClient.connect();
			sessions.put(sid, tcpClient);
			tunnel.openChannel(sid); //handshake response;
			LOGGER.info("channel handshake response:" + sid);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/* (non-Javadoc)
	 * @see ilabo.rtunnel.agent.TunnelHandler#receiveData(byte, byte[])
	 */
	@Override
	public void receiveData(byte sid, byte[] buf, int offset, int length){
		LOGGER.info("receiveData: sid=" + sid);
		try {
			TCPClient tcpClient = this.sessions.get(sid);
			if(tcpClient == null){
				tunnel.sendClose(sid);
				return;
			}
			
			tcpClient.write(buf, offset, length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see ilabo.rtunnel.agent.TunnelHandler#receiveClose(byte)
	 */
	@Override
	public void receiveClose(byte sid){
		TCPClient tcpClient = this.sessions.get(sid);
		if(tcpClient != null){
			tcpClient.close();
			this.sessions.remove(sid);
		}
	}
	
	
	
	class TCPClient extends Thread {
		private final Logger LOGGER = Logger.getLogger(TCPClient.class.getName());
		private WebsocketTunnelClient tunnel;
		private byte sid;
		private String host;
		private int port;


		private Socket socket = null;
		private InputStream in;
		private OutputStream out;

		byte[] buf = new byte[10240];
		boolean connected = false;
		
		TCPClient(WebsocketTunnelClient tunnel, byte sid, String host, int port) {
			super();
			this.tunnel = tunnel;
			this.sid = sid;
			this.host = host;
			this.port = port;
		}



		boolean isConnected() {
			return connected;
		}

		void connect() throws IOException {
			LOGGER.info("connect: " + this.sid);
			socket = new Socket(host, port);
			// System.out.println("socket: "+socket);
			in = socket.getInputStream();
			out = socket.getOutputStream();
			connected = true;
			start();
		}

		void write(byte[] buf, int offset, int length) {
			try {
				out.write(buf, offset, length);
				out.flush();
			} catch (Exception e) {
				System.out.println(e);
			}
		}

		@Override
		public void run() {
			byte[] buf = new byte[1024];
			while (true) {
				try {
					
						int l = in.read(buf);
						if (l < 0) {
							break;
						}

						tunnel.write(this.sid, buf, 0, l);
				
				} catch (java.net.SocketException e) {
					// System.out.println(e);
					break;
				} catch (Exception e) {
					// System.out.println(e);
				}
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


