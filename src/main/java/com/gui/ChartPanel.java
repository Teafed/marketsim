// includes chart area and orders panel

package com.gui;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import com.etl.ReadData;
import com.market.TradeItem;

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

    public void openChart(TradeItem data) throws IOException {
        removeAll();

        /* if building chart for the first time:
                1. check local database
                    - if no data yet for this TradeItem, fill historical data
                2. sync latest candles
                    - get latest timestamp in db, then fill up to present
                3. start live updates
                4. build initial chart
                5. loop auto refresh to see live updates
         */
        revalidate();
        repaint();
    }
}
