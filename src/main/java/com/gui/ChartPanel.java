// includes chart area and orders panel

package com.gui;

import javax.swing.*;
import java.awt.*;

public class ChartPanel extends ContentPanel {
   
   public ChartPanel() {
      initializeComponents();
   }

   private void initializeComponents() {
      setBackground(Color.WHITE);
   }

   public void openChart(String symbol) {
      removeAll();
      JLabel tempLabel = new JLabel(symbol + " chart coming soon <3", JLabel.CENTER);
      tempLabel.setFont(new Font("Arial", Font.BOLD, 16));
      add(tempLabel, BorderLayout.CENTER);
      revalidate();
      repaint();
   }
}
//hello