package com.gui;

import com.etl.SymbolData;
import javax.swing.*;
import java.awt.*;

// Custom cell renderer for rich symbol display
public class SymbolCellRenderer extends JPanel implements ListCellRenderer<SymbolData> {
   private JLabel symbolLabel;
   private JLabel priceLabel;
   private JLabel changeLabel;
   
   public SymbolCellRenderer() {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
      
      symbolLabel = new JLabel();
      symbolLabel.setFont(new Font("Arial", Font.BOLD, 14));
      
      priceLabel = new JLabel();
      priceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
      priceLabel.setHorizontalAlignment(JLabel.RIGHT);
      
      changeLabel = new JLabel();
      changeLabel.setFont(new Font("Arial", Font.PLAIN, 11));
      changeLabel.setHorizontalAlignment(JLabel.RIGHT);
      
      // layout
      JPanel leftPanel = new JPanel(new BorderLayout());
      leftPanel.add(symbolLabel, BorderLayout.CENTER);
      leftPanel.setOpaque(false);
      
      JPanel rightPanel = new JPanel(new BorderLayout());
      rightPanel.add(priceLabel, BorderLayout.NORTH);
      rightPanel.add(changeLabel, BorderLayout.CENTER);
      rightPanel.setOpaque(false);
      
      add(leftPanel, BorderLayout.WEST);
      add(rightPanel, BorderLayout.EAST);
   }
   
   @Override
   public Component getListCellRendererComponent(
         JList<? extends SymbolData> list,
         SymbolData value,
         int index,
         boolean isSelected,
         boolean cellHasFocus) {
      
      if (value != null) {
         symbolLabel.setText(value.getSymbol());
         priceLabel.setText(String.format("$%.2f", value.getPrice()));
         
         double changePercent = value.getChangePercent();
         String changeText = String.format("%+.2f%%", changePercent);
         changeLabel.setText(changeText);
         
         // color coding for change
         if (changePercent > 0) {
            changeLabel.setForeground(new Color(34, 139, 34)); // forest green
         } else if (changePercent < 0) {
            changeLabel.setForeground(new Color(220, 20, 60)); // crimson
         } else {
            changeLabel.setForeground(Color.GRAY);
         }
      }
      
      // selection styling
      if (isSelected) {
         setBackground(new Color(70, 130, 180)); // steel blue
         symbolLabel.setForeground(Color.WHITE);
         priceLabel.setForeground(Color.WHITE);
      } else {
         // alternating row colors
         if (index % 2 == 0) {
            setBackground(new Color(248, 248, 248)); // very light gray
         } else {
            setBackground(Color.WHITE);
         }
         symbolLabel.setForeground(Color.BLACK);
         priceLabel.setForeground(Color.BLACK);
      }
      
      setOpaque(true);
      return this;
   }
}