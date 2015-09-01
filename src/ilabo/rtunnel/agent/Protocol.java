package ilabo.rtunnel.agent;

public class Protocol {

	  static final byte TUNNEL_OPEN=1;
	  static final byte TUNNEL_DATA=2;
	  static final byte TUNNEL_PADDING=3;
	  static final byte TUNNEL_ERROR=4;
	  static final byte TUNNEL_SIMPLE=0x40;
	  static final byte TUNNEL_PAD1=5|TUNNEL_SIMPLE;
	  static final byte TUNNEL_CLOSE=6|TUNNEL_SIMPLE;
	  static final byte TUNNEL_DISCONNECT=7|TUNNEL_SIMPLE;

}
