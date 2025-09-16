// includes chart area and orders panel

package com.gui;

import javax.swing.*;
import java.awt.*;
import com.etl.ReadData;
import com.etl.SymbolData;

public class ChartPanel extends ContentPanel {
   
   public ChartPanel() {
      initializeComponents();
   }

   private void initializeComponents() {
      setBackground(Color.WHITE);
   }

   public void openChart(SymbolData data) {
      removeAll();
      JLabel tempLabel = new JLabel(data.getSymbol(), JLabel.CENTER);
      tempLabel.setFont(new Font("Arial", Font.BOLD, 16));
      add(tempLabel, BorderLayout.CENTER);
      revalidate();
      repaint();
   }
}
