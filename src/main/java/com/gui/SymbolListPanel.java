// symbol list, each entry will display the symbol name, latest fetched price, and percent increase/decrease. clicking one will open a ChartPanel

package com.gui;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SymbolListPanel extends JPanel {
   // debug
   private static final String DATA_DIR_DNE = "no data folder found";
   private static final String DATA_FILES_DNE = "no data files found";
   
   private JList<String> symbolList;
   private DefaultListModel<String> listModel;
   private List<SymbolSelectionListener> listeners;
   private String dataFolderPath;

   // interface that listeners must implement
   public interface SymbolSelectionListener {
      void onSymbolSelected(String symbol);
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
      setBorder(BorderFactory.createTitledBorder("symbols"));

      // create list model and list
      listModel = new DefaultListModel<>();
      symbolList = new JList<>(listModel);
      symbolList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      symbolList.setFont(new Font("Arial", Font.PLAIN, 14));

      // add scrolling
      JScrollPane scrollPane = new JScrollPane(symbolList);
      scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      
      add(scrollPane, BorderLayout.CENTER);
   }

   private void loadSymbols() {
      listModel.clear();
      File dataFolder = new File(dataFolderPath);
      if (!dataFolder.exists() || !dataFolder.isDirectory()) {
         listModel.addElement(DATA_DIR_DNE);
         return;
      }
      
      File[] csvFiles = dataFolder.listFiles((dir, name) -> 
         name.toLowerCase().endsWith(".csv"));
      if (csvFiles == null || csvFiles.length == 0) {
         listModel.addElement(DATA_FILES_DNE);
         return;
      }
      
      System.out.println("loading symbols from: " + dataFolderPath);
      System.out.println("found " + (csvFiles != null ? csvFiles.length : 0) + " csv files");
      if (csvFiles != null) {
         for (File f : csvFiles) {
            System.out.println("   " + f.getName());
         }
      }

      // sort and add symbols (filename without .csv extension)
      Arrays.sort(csvFiles, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
      
      for (File file : csvFiles) {
         String fileName = file.getName();
         String symbol = fileName.substring(0, fileName.lastIndexOf('.'));
         listModel.addElement(symbol);
      }
   }

   private void setupListeners() {
      symbolList.addListSelectionListener(new ListSelectionListener() {
         @Override
         public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) { // only fire when selection is final
               String selectedSymbol = symbolList.getSelectedValue();
               if (selectedSymbol != null && 
                   !selectedSymbol.equals(DATA_DIR_DNE) && 
                   !selectedSymbol.equals(DATA_FILES_DNE)) {
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

   private void notifyListeners(String symbol) {
      for (SymbolSelectionListener listener : listeners) {
         listener.onSymbolSelected(symbol);
      }
   }

   // utility methods
   public void refreshSymbols() {
      loadSymbols();
   }

   public String getSelectedSymbol() {
      return symbolList.getSelectedValue();
   }

   public void clearSelection() {
      symbolList.clearSelection();
   }
}