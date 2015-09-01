package ilabo.rtunnel.agent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * クライアント側でトンネルINする人
 *
 * @author ken
 *
 */
public class ClientAgent {
	private ServerSocket serverSocket = null;
	public void open() throws IOException{
		serverSocket = new ServerSocket(5088);
		Socket socket = serverSocket.accept();


	}

}
