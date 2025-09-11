package com;

import com.GUI.MainWindow;
import javax.swing.SwingUtilities;

public class Main {
   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         new MainWindow().setVisible(true);
      });
   }
}
