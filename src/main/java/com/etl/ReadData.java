//Read data from data directory, selected file
//Access data access singular .csv
package com.etl;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReadData {
    private Map<String, List<String[]>> data = new HashMap<>();

    public ReadData(String dataDir) throws IOException {
        loadData(dataDir);
    }

    private void loadData(String dataDir) throws IOException {
        Files.list(Paths.get(dataDir))
                .filter(path -> path.toString().endsWith(".csv"))
                .forEach(path -> {
                    try {
                        List<String[]> rows = new ArrayList<>();
                        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                rows.add(line.split(",")); // simple CSV split
                            }
                        }
                        data.put(path.getFileName().toString(), rows);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    // Public accessor for other classes
    public List<String[]> getFileData(String fileName) {
        return data.get(fileName);
    }

    public Set<String> getFileNames() {
        return data.keySet();
    }
}
