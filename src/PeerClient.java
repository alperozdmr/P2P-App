
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.JOptionPane;

public class PeerClient {

    private final int port; 
    private DatagramSocket socket;
    private final List<InetAddress> peers; 
    private List<DiscoveredPeer> peer;
    private File downloadFolder;
    private static PeerServer peerServer;
    private final P2PApplication GUI;
    int totalChunks;
    
    

    public PeerClient(int port, P2PApplication gui, PeerServer server){
        this.port = port;
        this.GUI = gui;
        peers = new ArrayList<>();
        peer = new ArrayList<>();
        peerServer = server;
        
        try {	
            socket = new DatagramSocket(this.port);
            System.out.println("PeerClient initialized on port: " + port);
        } catch (SocketException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize socket on port: " + port);
        }
    }
    public List<DiscoveredPeer> getPeers() {
        return peer;
    }
    public File getDownloadedFolder() {return downloadFolder;}
    
    public List<String> getDownloadedFiles() {
        if (downloadFolder == null) {
            throw new IllegalStateException("Shared folder is not set.");
        }

        List<String> files = new ArrayList<>();
        for (File file : downloadFolder.listFiles()) {
            if (file.isFile()) {
                files.add(file.getName());
            }
        }
        return files;
    }
    public void setDownloadFolder(File downloadFolder) {
        if (downloadFolder != null && downloadFolder.isDirectory()) {
            this.downloadFolder = downloadFolder;
            System.out.println("Download folder set to : " + downloadFolder.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid download folder.");
        }
    }
    public void listenForPeer() {
    	new Thread(() -> {
            try {
                
                while (true) {
                	byte[] buffer = new byte[1024];
                	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), 0, packet.getLength());
                    InetAddress senderAddress = packet.getAddress();
                    
                    int senderPort = packet.getPort();
                    
                    if (message.startsWith("DISCOVER_PEERS:")) {
                     int discoveredPort = Integer.parseInt(message.split(":")[1]);
                        
                        if (!peers.contains(senderAddress) || senderPort != discoveredPort) {
                            peers.add(senderAddress);
                            peer.add(new DiscoveredPeer(discoveredPort,senderAddress));
                            System.out.println("New peer discovered: " + senderAddress + " port: " + discoveredPort + " my port: " + port);
                            broadcastForFiles(senderAddress,discoveredPort);
                        }
                    }
                    else if (message.startsWith("SEARCH:")) {
                        String query = message.split(":")[1];
                        String response = peerServer.searchFiles(query); 
                        byte[] responseBuffer = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, senderAddress, senderPort);
                        socket.send(responsePacket);
                    }
	                else if (message.startsWith("REQUEST_FILE")) {
	                    String fileName = message.split(":")[1];
	                    peerServer.startFileTrans(fileName);
	                }
	                else {
	                	GUI.addFounded(message);
	                	
	                }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } 
        }).start();
    }

   
    public void broadcastPresence() {
    	new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                String discoveryMessage = "DISCOVER_PEERS:" + this.port;
                byte[] buffer = discoveryMessage.getBytes();
                
                for(int i = 9800;i< 9900; i++) {
                	if(i != port) {
	                DatagramPacket packet = new DatagramPacket(
	                        buffer,
	                        buffer.length,
	                        InetAddress.getByName("255.255.255.255"), 
	                        i 
	                );
	                
	                socket.setBroadcast(true);
	                socket.send(packet);
	                }
                }
                System.out.println("Discovery message sent from port: " + port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    public void broadcastForFiles(InetAddress Ip, int portNum) {
    	new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {

            	List<String> sharedFiles = peerServer.getSharedFiles();
            	sharedFiles.addFirst("*******broadcast from*********" + port);
            	
    	        if (sharedFiles.isEmpty()) {
    	            JOptionPane.showMessageDialog(null, "No files found in the shared folder.", "Info", JOptionPane.INFORMATION_MESSAGE);
    	            return;
    	        }
    	        for (String file : sharedFiles) {
    	                
    	        	byte[] buffer = file.getBytes();
    	        	DatagramPacket packet = new DatagramPacket(
	                        buffer,
	                        buffer.length,
	                        Ip, 
	                        portNum
	                );
    	        	socket.send(packet);
    	        }
    	        
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    
    public void requestFile(String fileName, DiscoveredPeer peer) {
        try{
            String message = "REQUEST_FILE:" + fileName ;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, peer._ıp, peer._dicoveredPort);
            socket.send(packet);
            System.out.println("File request sent for: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(()->{
			receiveFile(peer , fileName);
		}).start();
    }

    
    public void receiveFile(DiscoveredPeer peer,String fileName) {
    	int percentage = 0;
    	try {
			File file = new File(downloadFolder,fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			else {
				System.out.println("Dosya hali hazırdas var");
				return;
			}
			RandomAccessFile rAF = new RandomAccessFile(file, "rw");
			Socket socket = new Socket(peer._ıp, peer._dicoveredPort);
			System.out.println(fileName + " has connected to server...");
			DataInputStream dIS = new DataInputStream(socket.getInputStream());
			DataOutputStream dOS = new DataOutputStream(socket.getOutputStream());
			int length = dIS.readInt();
			//700 byte
			System.out.println(fileName + " has read " + length + " for fileLength...");
			rAF.setLength(length);
			//file length = 700 byte
			int i;
			while ((i = dIS.readInt()) != -1) {
				System.out.println(fileName + " has read " + i + " for chunkID...");
				rAF.seek(i * 256000);
				int chunkLength = dIS.readInt();
				System.out.println(fileName + " has read " + chunkLength + " for chunkSize...");
				byte[] toReceive = new byte[chunkLength];
				dIS.readFully(toReceive);
				System.out.println(fileName + " has read " + chunkLength + " bytes for chunkID " + i + "...");
				percentage += chunkLength;
				 GUI.updateDownloadProgress(fileName, length, percentage);
				rAF.write(toReceive);
				dOS.writeInt(i);
				System.out.println(fileName + " has sent " + i + " for ACK...");
			}
			System.out.println(fileName + " has read " + i + " for chunkID...");
			rAF.close();
			socket.close();
		} catch (Exception e) {
			System.out.println("Dosya gönderilemedi");
		}
	
    }
    
    public String searchFileOnPeer(String fileName, DiscoveredPeer peer) throws IOException {
        String searchMessage = "SEARCH:" + fileName;
        DatagramSocket responseSocket = new DatagramSocket();
        byte[] buffer = searchMessage.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length,peer._ıp, peer._dicoveredPort);

       
        responseSocket.send(packet);
        System.out.println("Search request sent to peer: " + peer._ıp + " port :" + peer._dicoveredPort + " my port" + port);

       
        byte[] responseBuffer = new byte[1024];
        DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
        responseSocket.receive(responsePacket);

        String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
        System.out.println("Response received from peer: " + peer._ıp +"/"+peer._dicoveredPort);

        return response;
    }

    public void disconnect() {
    	new Thread(() -> {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("Disconnected from the network on port: " + port);
        }
    	 }).start();
    }
    	
}
