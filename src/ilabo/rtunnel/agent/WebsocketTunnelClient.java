package ilabo.rtunnel.agent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.util.Arrays;

import com.neovisionaries.ws.client.ProxySettings;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

/**
 * kawasaki-sanのクライアント実装RFC6455対応なのがいい。
 * WebSocketそのままな感じがいい。httpsプロキシに対応してそう。
 *
 * @author yumi
 *
 */
public class WebsocketTunnelClient {

	static final private int CONTENT_LENGTH = 1024 * 10;

	private boolean init = false;
	private boolean closed = false;

	private String uri;
	private WebSocket ws = null;

	private ConcurrentHashMap<Byte, BlockingQueue<byte[]>> insessions = new ConcurrentHashMap<>();

	// private int sendCount=CONTENT_LENGTH;

	public WebsocketTunnelClient(String uri) {
		this.uri = uri;
	}

//	public void setProxy(String host, int port) {
//		this.proxy = new Proxy(host, port);
//	}

	/**
	 * トンネルのソケット接続
	 * @throws WSTunnelException
	 */
	public void connect() {
		try{
		WebSocketFactory factory = new WebSocketFactory();
		ProxySettings settings = factory.getProxySettings();

		this.ws = factory.createSocket(uri);



		this.ws.addListener(new WebSocketAdapter(){
			@Override
			public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) {
				byte[] tunnelData = frame.getPayload();
				ByteBuffer buf = ByteBuffer.wrap(tunnelData);
				byte command = tunnelData[0];
				byte id = tunnelData[1];
				byte[] data = Arrays.copyOfRange(tunnelData, 2, tunnelData.length);
				BlockingQueue<byte[]> inFrameQueue = insessions.get(id);
				try {
					inFrameQueue.put(data);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

		});

		this.ws.connect();
		}catch(Exception e){
			throw new WSTunnelException(e);
		}
	}


	/**
	 * エンドツーエンドのTCP接続。
	 * @param i
	 * @throws IOException
	 */
	public void openChannel(byte id) throws IOException {
		byte[] command = new byte[]{Protocol.TUNNEL_OPEN, id};

		ws.sendBinary(command);
	}

	public void sendDisconnect(byte id) throws IOException {
		byte[] command = new byte[]{Protocol.TUNNEL_DISCONNECT, id};

		ws.sendBinary(command);
	}

	public void sendClose(byte id) throws IOException {
		byte[] command = new byte[]{Protocol.TUNNEL_CLOSE, id};
		ws.sendBinary(command);
	}


	public void write(byte id, byte[] data) throws IOException {
		//セグメンテーションは未考慮（tcpが頑張る想定）
		ByteBuffer buf = ByteBuffer.allocate(data.length + 1);
		buf.put(id);
		buf.put(data);
		ws.sendBinary(buf.array());
	}

	int buf_len = 0;

	public int read(byte id, byte[] foo, int s, int l ) throws IOException, InterruptedException {
		if (closed)
			return -1;

		BlockingQueue<byte[]> inFrameQueue = insessions.get(id);
		byte[] buf = null;
		while((buf=inFrameQueue.poll(1000L, TimeUnit.MINUTES))==null){
			System.out.println("nodata...");
		}
		System.out.println(buf);

//
//		try {
//			if (buf_len > 0) {
//				int len = buf_len;
//				if (l < buf_len) {
//					len = l;
//				}
//				int i = ib.receiveData(foo, s, len);
//				buf_len -= i;
//				return i;
//			}
//
//			int len = 0;
//			while (!closed) {
//				int i = ib.receiveData(foo, s, 1);
//				if (i <= 0) {
//					return -1;
//				}
//				int request = foo[s] & 0xff;
//				// System.out.println("request: "+request);
//				if ((request & Protocol.TUNNEL_SIMPLE) == 0) {
//					i = ib.receiveData(foo, s, 1);
//					len = (((foo[s]) << 8) & 0xff00);
//					i = ib.receiveData(foo, s, 1);
//					len = len | (foo[s] & 0xff);
//				}
//				// System.out.println("request: "+request);
//				switch (request) {
//				case Protocol.TUNNEL_DATA:
//					buf_len = len;
//					// System.out.println("buf_len="+buf_len);
//					if (l < buf_len) {
//						len = l;
//					}
//					int orgs = s;
//					while (len > 0) {
//						i = ib.receiveData(foo, s, len);
//						if (i < 0)
//							break;
//						buf_len -= i;
//						s += i;
//						len -= i;
//					}
//					// System.out.println("receiveData: "+(s-orgs));
//					return s - orgs;
//				case Protocol.TUNNEL_PADDING:
//					ib.receiveData(null, 0, len);
//					continue;
//				case Protocol.TUNNEL_ERROR:
//					byte[] error = new byte[len];
//					ib.receiveData(error, 0, len);
//					// System.out.println(new String(error, 0, len));
//					throw new IOException("JHttpTunnel: " + new String(error, 0, len));
//				case Protocol.TUNNEL_PAD1:
//					continue;
//				case Protocol.TUNNEL_CLOSE:
//					closed = true;
//					// close();
//					// System.out.println("CLOSE");
//					break;
//				case Protocol.TUNNEL_DISCONNECT:
//					// System.out.println("DISCONNECT");
//					continue;
//				default:
//					// System.out.println("request="+request);
//					// System.out.println(Integer.toHexString(request&0xff)+ "
//					// "+new Character((char)request));
//					throw new IOException(": protocol error 0x" + Integer.toHexString(request & 0xff));
//				}
//			}
//		} catch (IOException e) {
//			throw e;
//		} catch (Exception e) {
//			// System.out.println("JHttpTunnelClient.read: "+e);
//		}
		return -1;
	}

//	private InputStream in = null;
//
//	public InputStream getInputStream() {
//		if (in != null)
//			return in;
//		in = new InputStream() {
//			byte[] tmp = new byte[1];
//
//			public int read() throws IOException {
//				int i = JHttpTunnelClient.this.read(tmp, 0, 1);
//				return (i == -1 ? -1 : tmp[0]);
//			}
//
//			public int read(byte[] foo) throws IOException {
//				return JHttpTunnelClient.this.read(foo, 0, foo.length);
//			}
//
//			public int read(byte[] foo, int s, int l) throws IOException {
//				return JHttpTunnelClient.this.read(foo, s, l);
//			}
//		};
//		return in;
//	}
//
//	private OutputStream out = null;
//
//	public OutputStream getOutputStream() {
//		if (out != null)
//			return out;
//		out = new OutputStream() {
//			final byte[] tmp = new byte[1];
//
//			public void write(int foo) throws IOException {
//				tmp[0] = (byte) foo;
//				JHttpTunnelClient.this.write(tmp, 0, 1);
//			}
//
//			public void write(byte[] foo) throws IOException {
//				JHttpTunnelClient.this.write(foo, 0, foo.length);
//			}
//
//			public void write(byte[] foo, int s, int l) throws IOException {
//				JHttpTunnelClient.this.write(foo, s, l);
//			}
//		};
//		return out;
//	}
//
//	public void close() {
//		// System.out.println("close");
//		try {
//			sendClose();
//		} catch (Exception e) {
//		}
//		try {
//			ib.close();
//		} catch (Exception e) {
//		}
//		try {
//			ob.close();
//		} catch (Exception e) {
//		}
//		closed = true;
//	}
//
//	public void setInBound(InBound ib) {
//		this.ib = ib;
//	}
//
//	public void setOutBound(OutBound ob) {
//		this.ob = ob;
//	}

	/*
	 * public static void main(String[] arg){ try{
	 *
	 * if(arg.length==0){ System.err.println("Enter hostname[:port]");
	 * System.exit(1); }
	 *
	 * String host=arg[0]; int hport=8888; if(host.indexOf(':')!=-1){
	 * hport=Integer.parseInt(host.substring(host.lastIndexOf(':') + 1));
	 * host=host.substring(0, host.lastIndexOf(':')); }
	 *
	 * int port=2323; String _port=System.getProperty("F"); if(_port!=null){
	 * port=Integer.parseInt(_port); }
	 *
	 * String proxy_host=System.getProperty("P"); int proxy_port=8080;
	 * if(proxy_host!=null && proxy_host.indexOf(':')!=-1){
	 * proxy_port=Integer.parseInt(proxy_host.substring(proxy_host.lastIndexOf('
	 * :') + 1)); proxy_host=proxy_host.substring(0,
	 * proxy_host.lastIndexOf(':')); }
	 *
	 * ServerSocket ss=new ServerSocket(port); while(true){ final Socket
	 * socket=ss.accept(); socket.setTcpNoDelay(true);
	 *
	 * //System.out.println("accept: "+socket);
	 *
	 * final InputStream sin=socket.getInputStream(); final OutputStream
	 * sout=socket.getOutputStream();
	 *
	 * final JHttpTunnelClient jhtc=new JHttpTunnelClient(host, hport);
	 * if(proxy_host!=null){ jhtc.setProxy(proxy_host, proxy_port); }
	 *
	 * // jhtc.setInBound(new InBoundURL()); // jhtc.setOutBound(new
	 * OutBoundURL());
	 *
	 * jhtc.setInBound(new InBoundSocket()); jhtc.setOutBound(new
	 * OutBoundSocket());
	 *
	 * jhtc.connect(); final InputStream jin=jhtc.getInputStream(); final
	 * OutputStream jout=jhtc.getOutputStream();
	 *
	 * Runnable runnable=new Runnable(){ public void run(){ byte[] tmp=new
	 * byte[1024]; try{ while(true){ int i=jin.read(tmp); if(i>0){
	 * sout.write(tmp, 0, i); continue; } break; } } catch(Exception e){ } try{
	 * sout.close(); sin.close(); socket.close(); jin.close(); jhtc.close(); }
	 * catch(Exception e){ } } }; (new Thread(runnable)).start();
	 *
	 * byte[] tmp=new byte[1024]; try{ while(true){ int i=sin.read(tmp);
	 * if(i>0){ jout.write(tmp, 0, i); continue; } break; } } catch(Exception
	 * e){ } try{ socket.close(); jin.close(); jhtc.close(); } catch(Exception
	 * e){ } } } catch(JHttpTunnelException e){ System.err.println(e); }
	 * catch(IOException e){ System.err.println(e); } }
	 */

	public static void main(String[] args) throws Exception{
		WebsocketTunnelClient c = new WebsocketTunnelClient("ws://localhost:8080/session/hoge");
		c.connect();
		c.openChannel((byte)1);
		c.write((byte)1, "aiueo".getBytes());

	}
}
