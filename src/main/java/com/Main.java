package com.aeno.marketsim;

import com.aeno.marketsim.gui.MainWindow;
import javax.swing.SwingUtilities;

public class Main {
   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         new MainWindow().setVisible(true);
      });
   }
}
