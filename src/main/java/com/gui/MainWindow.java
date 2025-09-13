// splits window into left and right panels. left pane is the list of symbols, right will have chart panel and profile panel

package com.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

public class MainWindow extends JFrame implements SymbolListPanel.SymbolSelectionListener {
   private JSplitPane splitPane;
   private SymbolListPanel dataPanel;
   private JPanel rightPanel;

   private static final String DATA_FOLDER = "data";
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

   private void setupSplitPane() {
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dataPanel, rightPanel);
      splitPane.setDividerLocation(LEFT_PANEL_WIDTH);
      splitPane.setOneTouchExpandable(false);
      splitPane.setResizeWeight(0.0); // right panel gets all extra space
      splitPane.setContinuousLayout(true); // smooth resizing
      splitPane.setOneTouchExpandable(true); // adds collapse/expand buttons

      // customize dividers
      splitPane.setDividerSize(1);
      splitPane.setUI(new BasicSplitPaneUI() {
         @Override
         public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
               @Override
               public void paint(Graphics g) {
               }
            };
         }
      });
      
      splitPane.setUI(new BasicSplitPaneUI() {
         @Override
         public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(this) {
               @Override
               public void paint(Graphics g) {
                  // basic
                  g.setColor(new Color(200, 200, 200));
                  g.fillRect(0, 0, getWidth(), getHeight());

                  /* testing
                  // gradient background
                  Graphics2D g2 = (Graphics2D) g;
                  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                      RenderingHints.VALUE_ANTIALIAS_ON);
                  
                  GradientPaint gradient = new GradientPaint(
                     0, 0, new Color(200, 200, 200),
                     getWidth(), 0, new Color(150, 150, 150)
                  );
                  g2.setPaint(gradient);
                  g2.fillRect(0, 0, getWidth(), getHeight());
                  
                  // optional: add subtle grip lines
                  g2.setColor(new Color(100, 100, 100));
                  int centerY = getHeight() / 2;
                  for (int i = -6; i <= 6; i += 3) {
                     g2.drawLine(2, centerY + i, getWidth() - 2, centerY + i);
                  }
                  */
               }
            };
         }
      });
      
      add(splitPane, BorderLayout.CENTER);
   }

   private void createPanels() {
      // data panel - contains list of symbols from csv data folder
      dataPanel = new SymbolListPanel(DATA_FOLDER);
      dataPanel.setBackground(Color.LIGHT_GRAY);
      dataPanel.setBorder(BorderFactory.createTitledBorder("Symbols"));
      dataPanel.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 0));
      dataPanel.setMinimumSize(new Dimension(MIN_LEFT_WIDTH, 0));
      dataPanel.addSymbolSelectionListener(this);

      // right panel - will show selected csv content
      rightPanel = new JPanel(new BorderLayout());
      rightPanel.setBackground(Color.WHITE);
      rightPanel.setBorder(BorderFactory.createTitledBorder("Content"));
      rightPanel.setMinimumSize(new Dimension(MIN_RIGHT_WIDTH, 0));
   }

   // ensure left panel has a minimum width
   private void setupResizeListener() {
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
   
   // implement the SymbolSelectionListener interface
   @Override
   public void onSymbolSelected(String symbol) {
      // for now, just update the placeholder text
      rightPanel.removeAll();
      JLabel selectedLabel = new JLabel(symbol + " chart coming soon <3", JLabel.CENTER);
      selectedLabel.setFont(new Font("Arial", Font.BOLD, 16));
      rightPanel.add(selectedLabel, BorderLayout.CENTER);
      rightPanel.revalidate();
      rightPanel.repaint();
      
      // TODO: create/show the appropriate chart panel
   }
}
