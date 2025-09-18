// includes chart area and orders panel

package com.gui;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import com.etl.ReadData;
import com.etl.SymbolData;

public class ChartPanel extends ContentPanel {
    private ReadData reader;

    public ChartPanel() {
        try {
            // Load CSV files from the "data" directory
            reader = new ReadData("data");
        } catch (Exception e) {
            e.printStackTrace();
        }

        initializeComponents();
    }

    private void initializeComponents() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
    }

    public void openChart(SymbolData data) {
        removeAll();

        // Example: Use the symbol name as a CSV filename
        List<String[]> rows = reader.loadData(data.getSymbol() + ".csv");
        
        if (rows != null) {
           JLabel infoLabel = new JLabel(
                "Loaded " + rows.size() + " rows for " + data.getSymbol(),
                JLabel.CENTER
           );
           infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
           add(infoLabel, BorderLayout.CENTER);
        } else {
           JLabel errorLabel = new JLabel(
                "No data found for " + data.getSymbol(),
                JLabel.CENTER
           );
           errorLabel.setFont(new Font("Arial", Font.BOLD, 16));
           add(errorLabel, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }
}
