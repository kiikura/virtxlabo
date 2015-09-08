package ilabo.rtunnel.agent;

import static java.util.logging.Level.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;
/**
 * クライアント側でトンネルINする人
 *
 * @author ken
 *
 */
public class ClientAgent {
	private static final Logger LOGGER = Logger.getLogger(ClientAgent.class.getName());

	public static void main(String[] args) throws Exception{
		LOGGER.info("ClientAgent start");
		ClientAgent agent = new ClientAgent();
		agent.open();
	}

	private ServerSocket socket = null;
	private WebsocketTunnelClient tunnel = null;
	private transient byte index = 0;
	ClientAgent() throws IOException{
		this.socket = new ServerSocket(5088);
		this.socket.setReuseAddress(true);
		this.tunnel = new WebsocketTunnelClient("ws://localhost:8080/session/hoge");
		this.tunnel.connect();
	}

	public void open() throws IOException{

		try {
            while(true) {
                final Socket s = socket.accept();
                final byte tid = ++index;
                final String tname = "Connection from " + s.getRemoteSocketAddress() + ":" +  s.getPort();
                TunnelConnection tcon = new TunnelConnection(s, tid, tunnel);
                Thread t = new Thread(tcon ,tname );
                t.start();

            }
        } finally {
            socket.close();
        }

	}

}


class TunnelConnection implements Runnable{
	private static final Logger LOGGER = Logger.getLogger(TunnelConnection.class.getName());
	Socket s;
	byte tid;
	WebsocketTunnelClient client;
	TunnelConnection(Socket s, byte tid, WebsocketTunnelClient client){
		this.s = s;
		this.tid = tid;
		this.client = client;
	}
	
	@Override
	public void run() {
        try {
        	LOGGER.info("client openChannel(): tid=" + tid);
        	Object b = client.openChannel(tid);
        	
        	//TODO: wait for handshake
        	synchronized(b){
        		b.wait(2000L);
        	}
        	
        	LOGGER.info("client channel: tid=" + tid + ", opened");
        	
        	OutputStream out = s.getOutputStream();
        	client.addResponseHandler(tid, out); //TODO I/Fの抽象化
        	
        	//req copier
        	InputStream in = s.getInputStream();
        	int len = 0;
        	byte[] buf = new byte[4096];
        	while((len=in.read(buf))>0){
        		client.write(tid, buf, 0, len);
        	}
        	
        	client.sendClose(tid);
        	
//            final OutputStream out = forwarder.connect(new RemoteOutputStream(SocketChannelStream.out(s)));
//            new CopyThread("Copier for "+s.getRemoteSocketAddress(),
//                    SocketChannelStream.in(s), out).start();
        } catch (IOException e) {
            // this happens if the socket connection is terminated abruptly.
            LOGGER.log(FINE,"Port forwarding session was shut down abnormally",e);
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
        	LOGGER.log(FINE,"Port forwarding session was shut down abnormally",e);
		}
    }
	
}
