package ilabo.rtunnel.agent;

public interface TunnelHandler {

	void receiveOpen(byte sid);

	void receiveData(byte sid, byte[] buf, int offset, int length);

	void receiveClose(byte sid);

}