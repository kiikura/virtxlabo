package ilabo.rtunnel.agent;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

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
public class ServerAgent {

	private String fowardHost = "localhost";
	private int forwardPort = 22;

}

class Client extends Thread {

	private String sid;
	private String host;
	private int port;

	private int dataremain = 0;
	private byte command = 0;

	private Socket socket = null;
	private InputStream in;
	private OutputStream out;

	boolean connected = false;
	Client(String sid, String host, int port) {
		super();
		this.sid = sid;
		this.host = host;
		this.port = port;
	}



	boolean isConnected() {
		return connected;
	}

	void connect() {
		try {
			socket = new Socket(host, port);
			// System.out.println("socket: "+socket);
			in = socket.getInputStream();
			out = socket.getOutputStream();
			connected = true;
			start();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void send(byte[] foo, int s, int l) {
		// System.out.println("send: "+new String(foo, s, l));
		try {
			out.write(foo, s, l);
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
				int space = space();
				if (space > 0) {
					if (space > buf.length)
						space = buf.length;
					int i = in.read(buf, 0, space);
					// System.out.println("run read: "+i);
					if (i < 0) {
						break;
					}
					// System.out.println(new String(buf, 0, i));
					if (i > 0) {
						push(buf, 0, i);
						try {
							Thread.sleep(1);
						} catch (Exception ee) {
						}
						continue;
					}
				}
				while (true) {
					if (space() > 0)
						break;
					try {
						Thread.sleep(1000);
					} catch (Exception ee) {
					}
				}
			} catch (java.net.SocketException e) {
				// System.out.println(e);
				break;
			} catch (Exception e) {
				// System.out.println(e);
			}
		}
		close();
	}

	int buflen = 0;
	byte[] buf = new byte[10240];

	synchronized int space() {
		return buf.length - buflen;
	}

	synchronized void push(byte[] foo, int s, int l) {
		// System.out.println("push "+l);
		System.arraycopy(foo, s, buf, buflen, l);
		buflen += l;
	}

	synchronized int pop(byte[] foo, int s, int l) {
		if (buflen == 0) {
			// System.out.println("pop "+0);
			return 0;
		}
		if (l > buflen)
			l = buflen;
		System.arraycopy(buf, 0, foo, s, l);
		System.arraycopy(buf, l, buf, 0, buflen - l);
		buflen -= l;

		if (socket == null && buflen <= 0) {
			// removeClient(sid);
		}
		// System.out.println("pop: "+l);
		return l;
	}

	public void close() {
		try {
			in.close();
			out.close();
			socket.close();
			socket = null;
		} catch (Exception e) {
		}
		if (buflen == 0) {
			// removeClient(sid);
		}
	}

}
