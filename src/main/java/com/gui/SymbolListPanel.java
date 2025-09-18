// symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel

package com.gui;

import com.etl.SymbolData;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SymbolListPanel extends JPanel {
   private DefaultListModel<SymbolData> symbolModel;
   private JList<SymbolData> symbolList;
   private List<SymbolSelectionListener> listeners;
   private String dataFolderPath;

   // interface that listeners must implement
   public interface SymbolSelectionListener {
       void onSymbolSelected(SymbolData symbol);
   }

    public SymbolListPanel(String dataFolderPath) {
      this.dataFolderPath = dataFolderPath;
      this.listeners = new ArrayList<>();
      
      initializeComponents();
      loadSymbols();
      setupListeners();
   }

   private void initializeComponents() {
      setLayout(new BorderLayout());
      setBackground(GUIComponents.BG_MEDIUM);
      setBorder(GUIComponents.createBorder());
      
      symbolModel = new DefaultListModel<>();
      symbolList = GUIComponents.createList(symbolModel);

      symbolList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      symbolList.setCellRenderer(new SymbolCellRenderer());
      symbolList.setFixedCellHeight(50);
      
      JScrollPane scrollPane = GUIComponents.createScrollPane(symbolList);
      scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      
      add(scrollPane, BorderLayout.CENTER);
   }

   private void loadSymbols() {
      symbolModel.clear();
      File dataFolder = new File(dataFolderPath);
      if (!dataFolder.exists() || !dataFolder.isDirectory()) {
            System.out.println("data directory dne");
         return;
      }
      
      File[] csvFiles = dataFolder.listFiles((dir, name) -> 
         name.toLowerCase().endsWith(".csv"));
      if (csvFiles == null || csvFiles.length == 0) {
            System.out.println("data files dne");
         return;
      }
      
      System.out.println("loading symbols from: " + dataFolderPath);
      System.out.println("found " + (csvFiles != null ? csvFiles.length : 0) + " csv files");
      if (csvFiles != null) {
         for (File f : csvFiles) {
            System.out.println("   " + f.getName());
         }
      }

      // sort and add symbols
      Arrays.sort(csvFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

      // TEMPORARY! please remove and replace with data   
      java.util.Random random = new java.util.Random();
      for (File file : csvFiles) {
         String fileName = file.getName();
         String symbol = fileName.substring(0, fileName.lastIndexOf('.'));
         
         double basePrice = 50 + random.nextDouble() * 200;
         double change = (random.nextDouble() - 0.5) * 10;
         double changePercent = (change / basePrice) * 100;
         
         symbolModel.addElement(new SymbolData(symbol, basePrice, change, changePercent));
      }
   }

   private void setupListeners() {
      symbolList.addListSelectionListener(new ListSelectionListener() {
         @Override
         public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) { // only fire when selection is final
               SymbolData selectedSymbol = symbolList.getSelectedValue();
               if (selectedSymbol != null) {
                  notifyListeners(selectedSymbol);
               }
            }
         }
      });
   }

   // methods for managing listeners
   public void addSymbolSelectionListener(SymbolSelectionListener listener) {
      listeners.add(listener);
   }

   public void removeSymbolSelectionListener(SymbolSelectionListener listener) {
      listeners.remove(listener);
   }

   private void notifyListeners(SymbolData symbol) {
      for (SymbolSelectionListener listener : listeners) {
         listener.onSymbolSelected(symbol);
      }
   }

   // utility methods
   public void refreshSymbols() {
      loadSymbols();
   }

   public String getSelectedSymbol() {
      SymbolData selected = symbolList.getSelectedValue();
      return selected != null ? selected.getSymbol() : null;
   }

   public void clearSelection() {
      symbolList.clearSelection();
   }
}
