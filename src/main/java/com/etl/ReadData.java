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
}
