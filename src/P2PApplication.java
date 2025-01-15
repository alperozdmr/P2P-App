
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.io.File;
import java.util.List;

public class P2PApplication extends JFrame {

	private JTextField sharedFolderField;
    private JTextField destinationFolderField;
    private JTextArea excludeFoldersArea;
    private JTextArea excludeFilesArea;
    private JTextArea downloadingFilesArea;
    private JTextArea foundFilesArea;
    private JTextField searchField;
    private File downloadFolder;
    private PeerClient Peer;
    private PeerServer peerServer;
    int percentage = 0;

    public P2PApplication(int port, String sharedFolderPath) {
        setTitle("P2P File Sharing Application - Port: " + port);
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        
        peerServer = new PeerServer(port,this);
        Peer = new PeerClient(port,this,peerServer);

        JMenuBar menuBar = new JMenuBar();
        JMenu filesMenu = new JMenu("Files");
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(filesMenu);
        menuBar.add(helpMenu);

        // Files menüsü öğeleri
        JMenuItem connectMenuItem = new JMenuItem("Connect");
        JMenuItem disconnectMenuItem = new JMenuItem("Disconnect");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        JMenuItem aboutMenuItem = new JMenuItem("About");

        aboutMenuItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Alper Özdemir 20210702058","About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutMenuItem);

        connectMenuItem.addActionListener(e -> connect());
        disconnectMenuItem.addActionListener(e -> disconnect());
        exitMenuItem.addActionListener(e -> System.exit(0));

        filesMenu.add(connectMenuItem);
        filesMenu.add(disconnectMenuItem);
        filesMenu.addSeparator();
        filesMenu.add(exitMenuItem);

        setJMenuBar(menuBar);

        // Ana Panel
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Üst Panel
        JPanel folderPanel = new JPanel();
        folderPanel.setLayout(new GridLayout(2, 3, 10, 10));

        folderPanel.add(new JLabel("Root of the P2P shared folder:"));
        sharedFolderField = new JTextField(sharedFolderPath);
        sharedFolderField.setEditable(false);
        folderPanel.add(sharedFolderField);
        JButton setSharedFolderButton = new JButton("Set");
        setSharedFolderButton.addActionListener(e -> setSharedFolder());
        folderPanel.add(setSharedFolderButton);

        folderPanel.add(new JLabel("Destination folder:"));
        destinationFolderField = new JTextField();
        folderPanel.add(destinationFolderField);
        JButton setDestinationFolderButton = new JButton("Set");
        setDestinationFolderButton.addActionListener(e -> setDestinationFolder());
        folderPanel.add(setDestinationFolderButton);

        mainPanel.add(folderPanel, BorderLayout.NORTH);

        // Orta Panel: Ayarlar ve İndirmeler
        JPanel settingsAndDownloadsPanel = new JPanel(new GridLayout(2, 2, 10, 10));

        // Sol Üst Panel: Settings
        JPanel exclusionPanel = new JPanel(new BorderLayout());
        exclusionPanel.setBorder(BorderFactory.createTitledBorder("Settings"));

        excludeFoldersArea = new JTextArea();
        excludeFoldersArea.setEditable(false);
        JScrollPane excludeFoldersScrollPane = new JScrollPane(excludeFoldersArea);
        excludeFoldersScrollPane.setPreferredSize(new Dimension(200, 100)); 
        exclusionPanel.add(excludeFoldersScrollPane, BorderLayout.CENTER);

        JPanel folderButtonPanel = new JPanel();
        folderButtonPanel.setLayout(new GridLayout(1, 2, 5, 5));
        JButton addFolderButton = new JButton("Add");
        JButton deleteFolderButton = new JButton("Delete");
        addFolderButton.addActionListener(e -> addFile(1));
        deleteFolderButton.addActionListener(e -> deleteFile());
        folderButtonPanel.add(addFolderButton);
        folderButtonPanel.add(deleteFolderButton);

        exclusionPanel.add(folderButtonPanel, BorderLayout.SOUTH);
        settingsAndDownloadsPanel.add(exclusionPanel);

        // Sağ Üst Panel: Exclude Files
        JPanel excludeFilesPanel = new JPanel(new BorderLayout());
        excludeFilesPanel.setBorder(BorderFactory.createTitledBorder("Exclude files matching these masks"));

        excludeFilesArea = new JTextArea();
        JScrollPane excludeFilesScrollPane = new JScrollPane(excludeFilesArea);
        excludeFilesScrollPane.setPreferredSize(new Dimension(200, 100)); 
        excludeFilesPanel.add(excludeFilesScrollPane, BorderLayout.CENTER);

        JPanel fileButtonPanel = new JPanel();
        fileButtonPanel.setLayout(new GridLayout(1, 2, 5, 5));
        JButton addFileButton = new JButton("Add");
        JButton deleteFileButton = new JButton("Delete");
        addFileButton.addActionListener(e -> addFile(2));
        deleteFileButton.addActionListener(e -> deleteFile());
        fileButtonPanel.add(addFileButton);
        fileButtonPanel.add(deleteFileButton);

        excludeFilesPanel.add(fileButtonPanel, BorderLayout.SOUTH);
        settingsAndDownloadsPanel.add(excludeFilesPanel);

        // Sol Alt Panel: Downloading Files
        JPanel downloadingPanel = new JPanel(new BorderLayout());
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));
        downloadingFilesArea = new JTextArea();
        downloadingFilesArea.setEditable(false);
        downloadingFilesArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (!downloadingFilesArea.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Downloading files clicked:\n" + downloadingFilesArea.getText());
                }
            }
        });
        JScrollPane downloadingScrollPane = new JScrollPane(downloadingFilesArea);
        downloadingScrollPane.setPreferredSize(new Dimension(300, 100)); 
        downloadingPanel.add(downloadingScrollPane, BorderLayout.CENTER);
        settingsAndDownloadsPanel.add(downloadingPanel);

        // Sağ Alt Panel: Found Files
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder("Found files"));

        foundFilesArea = new JTextArea();
        foundFilesArea.setEditable(false);
        foundFilesArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (!foundFilesArea.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Found files clicked:\n" + foundFilesArea.getText());
                }
            }
        });
        JScrollPane foundFilesScrollPane = new JScrollPane(foundFilesArea);
        foundFilesScrollPane.setPreferredSize(new Dimension(300, 100)); 
        searchPanel.add(foundFilesScrollPane, BorderLayout.CENTER);

        JPanel searchBoxPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchFiles());
        searchBoxPanel.add(searchField, BorderLayout.CENTER);
        searchBoxPanel.add(searchButton, BorderLayout.EAST);
        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(e -> downloadSelectedFile());  
        searchBoxPanel.add(downloadButton, BorderLayout.SOUTH);

        searchPanel.add(searchBoxPanel, BorderLayout.SOUTH);
        settingsAndDownloadsPanel.add(searchPanel);

        mainPanel.add(settingsAndDownloadsPanel, BorderLayout.CENTER);



        add(mainPanel);
        setVisible(true);
    }

    private void connect() {
    	 Peer.broadcastPresence();
        Peer.listenForPeer();
        JOptionPane.showMessageDialog(this, "Connected to the network.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void disconnect() {
        Peer.disconnect();
        JOptionPane.showMessageDialog(this, "Disconnected from the network.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setSharedFolder() {
        File folder = new File(sharedFolderField.getText());
        if (folder.exists() && folder.isDirectory()) {
        	peerServer.setSharedFolder(folder);
            JOptionPane.showMessageDialog(this, "Shared folder set to: " + folder.getAbsolutePath(), "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid shared folder.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setDestinationFolder() {
    	JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
        	downloadFolder = chooser.getSelectedFile();
        	destinationFolderField.setText(downloadFolder.getAbsolutePath());
        	Peer.setDownloadFolder(downloadFolder);
           
        }
    }

    private void addFile(int i) {
    	 if (Peer != null && peerServer.getSharedFolder() != null && i == 1) {
    	        
    	        List<String> sharedFiles = peerServer.getSharedFiles();
    	        
    	        if (sharedFiles.isEmpty()) {
    	            JOptionPane.showMessageDialog(null, "No files found in the shared folder.", "Info", JOptionPane.INFORMATION_MESSAGE);
    	            return;
    	        }

    	        
    	        StringBuilder filesToDisplay = new StringBuilder();
    	        for (String file : sharedFiles) {
    	            if (!excludeFoldersArea.getText().contains(file)) {
    	                filesToDisplay.append(file).append("\n");
    	            }
    	        }

    	        if (filesToDisplay.length() > 0) {
    	            excludeFoldersArea.append(filesToDisplay.toString());
    	           
    	        } else {
    	            JOptionPane.showMessageDialog(null, "All shared files are already in the list.", "Warning", JOptionPane.WARNING_MESSAGE);
    	        }
    	    }
    	 else if (Peer != null && Peer.getDownloadedFolder() != null && i == 2) {
 	        
 	        List<String> downloadedFiles = Peer.getDownloadedFiles();
 	        
 	        if (downloadedFiles.isEmpty()) {
 	            JOptionPane.showMessageDialog(null, "No files found in the shared folder.", "Info", JOptionPane.INFORMATION_MESSAGE);
 	            return;
 	        }

 	        
 	        StringBuilder filesToDisplay = new StringBuilder();
 	        for (String file : downloadedFiles) {
 	            if (!excludeFilesArea.getText().contains(file)) {
 	                filesToDisplay.append(file).append("\n");
 	            }
 	        }

 	        if (filesToDisplay.length() > 0) {
 	        	excludeFilesArea.append(filesToDisplay.toString());
 	           
 	        } else {
 	            JOptionPane.showMessageDialog(null, "All shared files are already in the list.", "Warning", JOptionPane.WARNING_MESSAGE);
 	        }
 	    }
    	 	else {
    	        JOptionPane.showMessageDialog(null, "Shared folder is not set.", "Error", JOptionPane.ERROR_MESSAGE);
    	    }
    	 
    }
    private void deleteFile() {
    	String selectedText = excludeFoldersArea.getSelectedText();

        if (selectedText != null && !selectedText.isEmpty()) {
            
            String currentContent = excludeFoldersArea.getText();
            excludeFoldersArea.setText(currentContent.replace(selectedText + "\n", ""));
        } else {
            JOptionPane.showMessageDialog(null, "Please select a file to delete.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    public void addFounded(String message) {
    	foundFilesArea.append(message+"\n");
    }
    public void updateDownloadProgress(String fileName, int totalChunks, int downloadedChunks) {
        percentage = (downloadedChunks * 100) / totalChunks;
        SwingUtilities.invokeLater(() -> {
            downloadingFilesArea.append(fileName + " - " + percentage + "% downloaded\n");
        });
    }


    private void searchFiles() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Search query cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<DiscoveredPeer> peers = Peer.getPeers();
        if (peers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No peers available for search.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder results = new StringBuilder();
        for (DiscoveredPeer peer : peers) {
            try {
                String response = Peer.searchFileOnPeer(query, peer); 
                if (!response.equals("NO_RESULTS")) {
                    results.append("Peer: ").append(peer._ıp +"/").append(peer._dicoveredPort).append("\n").append(response).append("\n\n");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error while searching on peer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        if (results.length() > 0) {
            foundFilesArea.setText(results.toString());
            JOptionPane.showMessageDialog(this, "Search completed.", "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            foundFilesArea.setText("No files found.");
            JOptionPane.showMessageDialog(this, "No matching files found.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void downloadSelectedFile() {
        String selectedText = foundFilesArea.getSelectedText();  
        if (selectedText == null || selectedText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a file to download.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<DiscoveredPeer> peers = Peer.getPeers();
        if (peers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No peers available for download.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int index = 0;

        for (int i = 0; i< peers.size();i++) {
            try {
                String response = Peer.searchFileOnPeer(selectedText, peers.get(i)); 
                if (!response.equals("NO_RESULTS")) {
                    index = i;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error while searching on peer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        try {
            
            DiscoveredPeer peer = peers.get(index); 
            Peer.requestFile(selectedText, peer);
            JOptionPane.showMessageDialog(this, "Download request sent for: " + selectedText, "Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error during file download request: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}


