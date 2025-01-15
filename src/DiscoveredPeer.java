
import java.io.*;
import java.net.*;
import java.util.*;


public class DiscoveredPeer {
	public int _dicoveredPort;
	public InetAddress _ıp;
	
	
	public DiscoveredPeer(int dicoveredPort, InetAddress ıp) {
		_dicoveredPort = dicoveredPort;
		_ıp = ıp;
	}
	public int getPort() {
		return _dicoveredPort;
	}
	public InetAddress getIp() {
		return _ıp;
	}
}
