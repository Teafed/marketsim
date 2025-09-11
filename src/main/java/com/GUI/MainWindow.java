package com.GUI;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

   public MainWindow() {
      setTitle("Marketsim");
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setSize(1200, 800);
      setLocationRelativeTo(null);

      initComponents();
   }

   private void initComponents() {
      setLayout(new BorderLayout());

      // left panel - ticker and profile
      JPanel leftPanel = new JPanel();
      leftPanel.setPreferredSize(new Dimension(200, 0));
      leftPanel.setBackground(Color.LIGHT_GRAY);
      leftPanel.add(new JLabel("ticker list goes here"));

      // right panel - chart and orders
      JPanel chartPanel = new JPanel();
      chartPanel.setBackground(Color.WHITE);
      chartPanel.add(new JLabel("chart goes here"));

      JPanel orderPanel = new JPanel();
      orderPanel.setPreferredSize(new Dimension(0, 150));
      orderPanel.setBackground(Color.GRAY);
      orderPanel.add(new JLabel("orders go here"));

      add(leftPanel, BorderLayout.WEST);
      add(chartPanel, BorderLayout.CENTER);
      add(orderPanel, BorderLayout.SOUTH);
   }
}
