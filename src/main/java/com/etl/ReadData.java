//Read data from data directory, selected file
//Access data access singular .csv
package com.etl;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ReadData {
    private final Path dataDir;
    private final Map<String, List<String[]>> cache = new HashMap<>();

    public ReadData(String dataDir) {
        this.dataDir = Paths.get(dataDir);
    }

    // trying out different way of getting the data
    public Stream<String[]> streamData(String fileName) throws IOException {
        Path filePath = dataDir.resolve(fileName);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        BufferedReader br = Files.newBufferedReader(filePath);
        return br.lines()
                 .map(line -> line.split(",")) // simple CSV, for more complex CSV use a parser
                 .onClose(() -> {
                     try { br.close(); } catch (IOException ignored) {}
                 });
    }

    public List<String[]> loadData(String fileName) throws IOException {
        if (cache.containsKey(fileName)) {
            return cache.get(fileName); // return cached data
        }
        
        Path filePath = dataDir.resolve(fileName);
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                rows.add(line.split(",")); // simple CSV split
            }
        }
        
        cache.put(fileName, rows); // store in cache
        return rows;
    }

    public List<String> getFileNames() throws IOException {
        List<String> fileNames = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*.csv")) {
            for (Path path : stream) {
                fileNames.add(path.getFileName().toString());
            }
        }
        return fileNames;
    }
}
