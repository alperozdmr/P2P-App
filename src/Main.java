import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        
        	 new P2PApplication(9876, "C:\\Users\\Hp\\Desktop\\peer2s");
        	 // share folder dosyası static bir şekilde konulmalıdır her zaman bu dosyadan paylaşım yapılcaktır. 
        });
    }
}
