package com.etl;

import java.util.*;
import java.util.concurrent.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

// Example DataSources (use the public DataSource interface in DataSource.java)
// in ExtractorDemo.java
class PolygonSource implements DataSource {
    private final String symbol;
    private final String apiKey;
    private final HttpClient client = HttpClient.newHttpClient();

    public PolygonSource(String symbol) {
        this.symbol = symbol;
        this.apiKey = "FOOBYCjBROrpR4gY73p9OqpzQMWNACHL"; // replace with your actual API key
    }

    @Override
    public String extract() {
        String url = String.format(
            "https://api.polygon.io/v2/aggs/ticker/%s/prev?adjusted=true&apiKey=%s",
            symbol, apiKey
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "Error fetching Polygon data: " + e.getMessage();
        }
    }
}

class FinnhubSource implements DataSource {
    private final String symbol;
    private final String apiKey;
    private final HttpClient client = HttpClient.newHttpClient();

    public FinnhubSource(String symbol) {
        this.symbol = symbol;
        this.apiKey = "d2sq3t1r01qkuv3hu930d2sq3t1r01qkuv3hu93g"; // replace with your actual API key
    }

    @Override
    public String extract() {
        String url = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol, apiKey);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return "Error fetching Finnhub data: " + e.getMessage();
        }
    }
}

public class ExtractorDemo {
    private final List<DataSource> sources;
    private final ExecutorService executor;

    public ExtractorDemo(List<DataSource> sources, int threads) {
        this.sources = new ArrayList<>(sources);
        this.executor = Executors.newFixedThreadPool(threads);
    }

    public List<String> runAll() throws InterruptedException, ExecutionException {
        List<Future<String>> futures = new ArrayList<>();
        for (DataSource src : sources) {
            futures.add(executor.submit(src::extract));
        }

        List<String> results = new ArrayList<>();
        for (Future<String> f : futures) {
            results.add(f.get());
        }

        return results;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        // Demo using both sources
        List<DataSource> sources = Arrays.asList(
                new PolygonSource("AAPL"),
                new FinnhubSource("AAPL")
        );

        ExtractorDemo extractor = new ExtractorDemo(sources, 2);
        try {
            List<String> results = extractor.runAll();
            results.forEach(System.out::println);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Run interrupted: " + e.getMessage());
        } catch (ExecutionException e) {
            System.err.println("Execution failed: " + e.getCause());
        } finally {
            extractor.shutdown();
        }
    }
}
