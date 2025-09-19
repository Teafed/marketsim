import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Extractor connects to Finnhub WebSocket and captures initial quotes and live trades,
 * batching writes to a CSV file.
 */
public class Extractor extends WebSocketListener {

    private static final String FINNHUB_WS_URL = "wss://ws.finnhub.io?token=";
    private static final String CSV_FILE_PATH = "trades.csv";
    private static final int BATCH_SIZE = 1000;
    private static final String CSV_HEADER = "symbol,price,volume,timestamp_ms,source\n";

    private final String apiToken;
    private final List<String> symbols;
    private final List<String> dedupedSymbols;
    private final OkHttpClient client;
    private final Gson gson;
    private final java.util.concurrent.ConcurrentMap<String, Snapshot> latestMap;
    private final ScheduledExecutorService batchWriterExecutor;
    private final java.util.concurrent.ExecutorService fetchExecutor;
    private WebSocket webSocket;

    public Extractor(String apiToken, List<String> symbols) {
        if (apiToken == null || apiToken.trim().isEmpty()) {
            throw new IllegalArgumentException("API token cannot be null or empty.");
        }
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbols list cannot be null or empty.");
        }
        this.apiToken = apiToken;
        this.symbols = symbols;
        // create deduplicated symbol list preserving order
        this.dedupedSymbols = new java.util.ArrayList<>(new java.util.LinkedHashSet<>(symbols));
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.latestMap = new java.util.concurrent.ConcurrentHashMap<>();
        this.batchWriterExecutor = Executors.newSingleThreadScheduledExecutor();
        this.fetchExecutor = Executors.newSingleThreadExecutor();
    }

    public void start() {
        // Remove any leftover files to ensure we start with a clean snapshot file
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(CSV_FILE_PATH));
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(CSV_FILE_PATH + ".tmp"));
        } catch (IOException ignored) {
            // ignore
        }
        fetchCurrentQuotes();
        // Write the initial snapshot immediately to ensure trades.csv is overwritten
        writeBatchToFile();
        Request request = new Request.Builder().url(FINNHUB_WS_URL + apiToken).build();
        webSocket = client.newWebSocket(request, this);
        batchWriterExecutor.scheduleAtFixedRate(this::writeBatchToFile, 1, 1, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        System.out.println("Extractor started. Connecting to WebSocket...");
    }

    /**
     * Fetch current quote data for each subscribed symbol using the Finnhub REST API.
     * Each snapshot is enqueued as a CSV line with volume set to 0.
     */
    private void fetchCurrentQuotes() {
        System.out.println("Fetching current quotes for subscribed symbols...");
        int maxRetries = 3;
        int successCount = 0;
        List<String> failedSymbols = new ArrayList<>();
        for (String symbol : symbols) {
            String url = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol, apiToken);
            long backoffMs = 500;
            boolean success = false;
            for (int attempt = 1; attempt <= maxRetries && !success; attempt++) {
                Request req = new Request.Builder().url(url).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    int code = resp.code();
                    if (!resp.isSuccessful()) {
                        if ((code == 429 || (code >= 500 && code < 600)) && attempt < maxRetries) {
                            long jitter = (long) (Math.random() * 200);
                            try { Thread.sleep(backoffMs + jitter); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                            backoffMs *= 2;
                            continue;
                        }
                        System.err.printf("Failed to fetch quote for %s: HTTP %d\n", symbol, code);
                        break;
                    }
                    String body = resp.body() != null ? resp.body().string() : "";
                    QuoteResponse qr = gson.fromJson(body, QuoteResponse.class);
                    double price = qr != null && qr.c != null ? qr.c : Double.NaN;
                    long rawTs = qr != null && qr.t != null && qr.t > 0 ? qr.t : System.currentTimeMillis();
                    long timestamp = normalizeToMillis(rawTs);
                    if (!Double.isNaN(price)) {
                        latestMap.put(symbol, new Snapshot(price, 0, timestamp, "quote"));
                        success = true;
                        successCount++;
                    } else {
                        System.err.printf("No price available for %s (response: %s)\n", symbol, body);
                    }
                } catch (IOException | JsonSyntaxException e) {
                    if (attempt < maxRetries) {
                        try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                        backoffMs *= 2;
                        continue;
                    }
                    System.err.printf("Error fetching quote for %s: %s\n", symbol, e.getMessage());
                }
            }
            try { Thread.sleep(250); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            if (!success) {
                failedSymbols.add(symbol);
            }
        }

        System.out.printf("Finished fetching initial quotes: %d succeeded, %d failed.%n", successCount, failedSymbols.size());
        if (!failedSymbols.isEmpty()) {
            System.err.println("Failed symbols: " + String.join(",", failedSymbols));
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("WebSocket connection opened.");
        for (String symbol : symbols) {
            String subscribeMsg = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", symbol);
            webSocket.send(subscribeMsg);
            System.out.println("Subscribed to " + symbol);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            TradeResponse tradeResponse = gson.fromJson(text, TradeResponse.class);
            if ("trade".equals(tradeResponse.type) && tradeResponse.data != null) {
                for (Trade trade : tradeResponse.data) {
                    long ts = normalizeToMillis(trade.t);
                    latestMap.put(trade.s, new Snapshot(trade.p, (long)trade.v, ts, "trade"));
                }
            }
        } catch (JsonSyntaxException e) {
            if (!text.contains("ping")) {
                 System.err.println("Error parsing JSON: " + text);
            }
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        System.err.println("WebSocket failure: " + t.getMessage());
        // Print a short stack trace to stderr in a compact form
        System.err.println("Cause: " + t);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        System.out.println("WebSocket closing: " + code + " " + reason);
    }

    private void writeBatchToFile() {
        File file = new File(CSV_FILE_PATH);
        List<String> lines = new ArrayList<>();
        // Build snapshot lines in the order of deduplicated symbols
        for (String symbol : dedupedSymbols) {
            Snapshot s = latestMap.get(symbol);
            if (s != null && !Double.isNaN(s.price)) {
                lines.add(String.format("%s,%.2f,%d,%d,%s", symbol, s.price, s.volume, s.timestamp, s.source));
            } else {
                // If no data for symbol, write a placeholder with zeros and empty source
                lines.add(String.format("%s,0.00,0,0,%s", symbol, ""));
            }
        }

        java.nio.file.Path tmpPath = java.nio.file.Paths.get(CSV_FILE_PATH + ".tmp");
        try (BufferedWriter writer = java.nio.file.Files.newBufferedWriter(tmpPath)) {
            writer.write(CSV_HEADER);
            for (String l : lines) {
                writer.write(l);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing snapshot to temp file: " + e.getMessage());
            return;
        }

        try {
            java.nio.file.Path dest = java.nio.file.Paths.get(CSV_FILE_PATH);
            java.nio.file.Files.move(tmpPath, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("Overwrote %s with %d unique symbols\n", CSV_FILE_PATH, dedupedSymbols.size());
        } catch (IOException e) {
            System.err.println("Error moving temp snapshot to destination: " + e.getMessage());
        }
    }

    public void shutdown() {
        System.out.println("Shutdown hook initiated. Flushing remaining trades...");
        if (webSocket != null) {
            webSocket.close(1000, "Client shutdown");
        }
        client.dispatcher().executorService().shutdown();
        fetchExecutor.shutdown();
        batchWriterExecutor.shutdown();
        try {
            if (!batchWriterExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                batchWriterExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchWriterExecutor.shutdownNow();
        }
        // Final write of snapshot
        writeBatchToFile();
        System.out.println("Shutdown complete.");
    }

    @SuppressWarnings("unused")
    private static class TradeResponse {
        List<Trade> data;
        String type;
    }

    @SuppressWarnings("unused")
    private static class QuoteResponse {
        Double c;
        Double h;
        Double l;
        Double o;
        Double pc;
        Long t;
    }

    @SuppressWarnings("unused")
    private static class Trade {
        String s;
        double p;
        double v;
        long t;
    }

    private static class Snapshot {
        final double price;
        final long volume;
        final long timestamp;
        final String source;

        Snapshot(double price, long volume, long timestamp, String source) {
            this.price = price;
            this.volume = volume;
            this.timestamp = timestamp;
            this.source = source;
        }
    }

    // Normalize timestamps: if value looks like seconds (<= 1e11..1e12), convert to ms
    private static long normalizeToMillis(long t) {
        if (t <= 0) return System.currentTimeMillis();
        // if t looks like seconds (e.g., ~1e9), convert to ms
        if (t < 1_000_000_000_000L) {
            return t * 1000L;
        }
        return t;
    }

    public static void main(String[] args) {
        String apiToken = System.getenv("FINNHUB_API_KEY");
        if (apiToken == null || apiToken.trim().isEmpty()) {
            apiToken = System.getenv("FINNHUB_TOKEN");
        }
        if (apiToken == null || apiToken.trim().isEmpty()) {
            System.err.println("Error: FINNHUB_API_KEY (or FINNHUB_TOKEN) environment variable not set.");
            System.err.println("Please set the API key and run again.");
            System.err.println("Example: export FINNHUB_API_KEY='your_api_key_here'");
            System.err.println("(or export FINNHUB_TOKEN='your_api_key_here' for backward compatibility)");
            return;
        }

        List<String> symbols = Arrays.asList(
            "AAPL", "MSFT", "AMZN", "GOOGL", "TSLA", "NVDA", "JPM", "JNJ", "V", "PG",
            "UNH", "HD", "MA", "BAC", "DIS", "PYPL", "ADBE", "NFLX", "CMCSA", "INTC",
            "PFE", "CSCO", "PEP", "XOM", "T", "ABT", "CRM", "KO", "WMT", "ABBV",
            "MCD", "NKE", "MDT", "COST", "AVGO", "QCOM", "TXN", "HON", "UNP", "LIN",
            "SBUX", "CAT", "IBM", "GS", "LOW", "AMGN", "CVX", "DHR", "LMT", "BLK"
         );

         Extractor extractor = new Extractor(apiToken, symbols);
         extractor.start();

         try {
            Thread.currentThread().join();
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted.");
        }
    }
}
