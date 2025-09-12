package com.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MainWindow extends JFrame {
   private JSplitPane splitPane;
   private JPanel leftPanel;
   private JPanel rightPanel;
   private static final int LEFT_PANEL_WIDTH = 250;
   private static final int MIN_LEFT_WIDTH = 150;
   private static final int MIN_RIGHT_WIDTH = 300;

   public MainWindow() {
      initializeWindow();
      createPanels();
      setupSplitPane();
      setupResizeListener();
      
      setVisible(true);
   }

   private void initializeWindow() {
      setTitle("Marketsim");
      setSize(1200, 800);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setLocationRelativeTo(null); // center on screen
   }

   private void createPanels() {
      // left panel - will contain the csv file list
      leftPanel = new JPanel();
      leftPanel.setBackground(Color.LIGHT_GRAY);
      leftPanel.setBorder(BorderFactory.createTitledBorder("CSV Files"));
      leftPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 0));
      leftPanel.setMinimumSize(new Dimension(MIN_LEFT_WIDTH, 0));

      // right panel - will show selected csv content
      rightPanel = new JPanel();
      rightPanel.setBackground(Color.WHITE);
      rightPanel.setBorder(BorderFactory.createTitledBorder("Content"));
      rightPanel.setMinimumSize(new Dimension(MIN_RIGHT_WIDTH, 0));
   }

   private void setupSplitPane() {
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
      splitPane.setDividerLocation(LEFT_PANEL_WIDTH);
      splitPane.setResizeWeight(0.0); // right panel gets all extra space
      splitPane.setContinuousLayout(true); // smooth resizing
      splitPane.setOneTouchExpandable(true); // adds collapse/expand buttons
      
      add(splitPane, BorderLayout.CENTER);
   }

   private void setupResizeListener() {
      // ensure left panel stays constant width when window is resized
      addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            // maintain the left panel at constant width
            int currentDividerLocation = splitPane.getDividerLocation();
            if (currentDividerLocation != LEFT_PANEL_WIDTH) {
               // only reset if user hasn't manually moved the divider
               // might need adjusting idk
               SwingUtilities.invokeLater(() -> {
                  if (splitPane.getDividerLocation() < MIN_LEFT_WIDTH) {
                     splitPane.setDividerLocation(MIN_LEFT_WIDTH);
                  } else if (getWidth() - splitPane.getDividerLocation() < MIN_RIGHT_WIDTH) {
                     splitPane.setDividerLocation(getWidth() - MIN_RIGHT_WIDTH);
                  }
               });
            }
         }
      });
   }
}
