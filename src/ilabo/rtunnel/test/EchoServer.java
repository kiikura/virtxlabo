package ilabo.rtunnel.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EchoServer {

	private final int port;
	private List<Socket> sockets = Collections.synchronizedList(new ArrayList<>());

	public EchoServer(int port){
		this.port = port;
	}

	public void start() throws IOException{
		ServerSocket socket = new ServerSocket(port);
		while(true){
			final Socket s = socket.accept();
			sockets.add(s);
			Thread l = new Thread(() ->{
				try {
					InputStream in = s.getInputStream();
					OutputStream out = s.getOutputStream();
					byte[] buf = new byte[4096];
					while(true){
						int len = in.read(buf);
						out.write(buf, 0, len);
						System.out.println(new String(buf));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			l.start();
		}
	}


	public static void main(String[] args) throws Exception {
		EchoServer server = new EchoServer(23);
		server.start();

	}

}
