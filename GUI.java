import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class GUI {
   private static void createAndShowGUI() {
      // Create the main window
      JFrame frame = new JFrame("MarketSim");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      // Add a simple component (e.g., a JLabel)
      JLabel label = new JLabel("this is a test!");
      frame.getContentPane().add(label);

      // Set window size and make it visible
      frame.pack(); // Adjusts frame size to fit its contents
      frame.setVisible(true);
   }

   public static void main(String[] args) {
      // Schedule GUI creation on the Event Dispatch Thread (EDT)
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            createAndShowGUI();
         }
      });
   }
}

