
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.security.MessageDigest;

public class PeerServer {

    private File sharedFolder;   
    private final int serverPort;
    private final P2PApplication GUI;
    private ServerSocket serverSocket;
    Socket connectionSocket;

    public PeerServer(int _port ,P2PApplication _GUI ) {
        serverPort = _port;
        GUI=_GUI;
        sharedFolder = null;
        try {
        	serverSocket = new ServerSocket(serverPort);
			System.out.println(">>> Server is running on "+ serverPort );
		} catch (Exception e) {
			e.printStackTrace();
		}
        
    }
    public void startFileTrans(String fileName) {
	      while(true) {
	    	try {
				connectionSocket = serverSocket.accept();
				new Thread(()->{
					try {
						sendFileChunk(connectionSocket , fileName);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
	      }
    }
    // Paylaşılan klasörü ayarla
    public void setSharedFolder(File sharedFolder) {
        if (sharedFolder != null && sharedFolder.isDirectory()) {
            this.sharedFolder = sharedFolder;
            //System.out.println("Shared folder set to in file manager : " + sharedFolder.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid shared folder.");
        }
    }
    public File getSharedFolder() {
    	return sharedFolder;
    }

    public List<String> getSharedFiles() {
        if (sharedFolder == null) {
            throw new IllegalStateException("Shared folder is not set.");
        }

        List<String> files = new ArrayList<>();
        for (File file : sharedFolder.listFiles()) {
            if (file.isFile()) {
                files.add(file.getName());
            }
        }
        return files;
    }
	 public void sendFileChunk(Socket socket, String fileName) throws IOException {
		 try {
			 File file = new File(sharedFolder, fileName);
	            if (!file.exists() || !file.isFile()) {
	                System.out.println("File not found: " + fileName);
	                return;
	            }
				System.out.println(">>> " + socket.getInetAddress().getHostAddress() + " has connected...");
				System.out.println(">>> Sending data...");				
				//so you can both read and write to its instance
				RandomAccessFile rAF = new RandomAccessFile(file, "r");
				int length = (int) file.length(); //700 byte

				int chunkCount = (int) Math.ceil(length / 256000.0);
				//2.7 -> 3
				//to track which chunks have been sent (0 means not sent, 1 means sent and acknowledged).
				int[] checkArray = new int[chunkCount];
				DataInputStream dIS = new DataInputStream(socket.getInputStream());
				DataOutputStream dOS = new DataOutputStream(socket.getOutputStream());
				//sends the total file size (length) to the client, so the client knows how large the file is.
				dOS.writeInt(length);
				Random random = new Random();
				int loop = 0;
				while (loop < chunkCount) {
					int i = random.nextInt(chunkCount);
					if (checkArray[i] == 0) {
						//moves file pointer to the start of the chunk
						rAF.seek(i * 256000);
						//1*256 -> 256. byte 511.999
						byte[] toSend = new byte[256000];
						int read = rAF.read(toSend);
						dOS.writeInt(i); // send chunk no
						dOS.writeInt(read); // send read length
						dOS.write(toSend, 0, read); // send data
						dOS.flush();
						int ACK = dIS.readInt();
						if (i == ACK) {
							checkArray[i] = 1;
							loop++;
						}
					}
				}
				System.out.println(">>> Sent all chunks to " + socket.getInetAddress().getHostAddress() + "...");
				rAF.close();
				dOS.writeInt(-1);
				dOS.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
	       
	    }

    public String calculateFileHash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String searchFiles(String query) {
        if (sharedFolder == null) {
            return "NO_RESULTS";
        }

        File[] files = sharedFolder.listFiles();
        if (files == null || files.length == 0) {
            return "NO_RESULTS";
        }

        StringBuilder results = new StringBuilder();
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().contains(query.toLowerCase())) {
                results.append(file.getName()).append("\n");
            }
        }

        return results.length() > 0 ? results.toString().trim() : "NO_RESULTS";
    }

}

