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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simplified Extractor: connects to Finnhub WebSocket, subscribes to symbols,
 * enqueues trade messages as CSV lines, and writes them in batches to trades.csv.
 */
public class Extractor extends WebSocketListener {

    private static final String FINNHUB_WS_URL = "wss://ws.finnhub.io?token=";
    private static final String CSV_FILE = "trades.csv";
    private static final String CSV_HEADER = "symbol,price,volume,timestamp_ms,source\n";
    private static final int BATCH_SIZE = 1000;

    private final String apiKey;
    private final List<String> symbols;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService writer = Executors.newSingleThreadScheduledExecutor();
    private final java.util.Set<String> failedSymbols = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final java.util.Set<String> noCandle = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final ScheduledExecutorService retryExecutor = Executors.newSingleThreadScheduledExecutor();
    private WebSocket ws;
    private final AtomicBoolean fileInitialized = new AtomicBoolean(false);
    private final ConcurrentHashMap<String,String> latest = new ConcurrentHashMap<>();
    private final boolean forceCandles;

    public Extractor(String apiKey, List<String> symbols) {
        if (apiKey == null || apiKey.trim().isEmpty()) throw new IllegalArgumentException("API key required");
        if (symbols == null || symbols.isEmpty()) throw new IllegalArgumentException("Symbols required");
        this.apiKey = apiKey;
        this.symbols = symbols;
        this.forceCandles = Boolean.parseBoolean(System.getenv("FINNHUB_FORCE_CANDLES"));
    }

    public void start() {
        System.out.println("FINNHUB_FORCE_CANDLES=" + forceCandles);
        initializeCsv();
        // Fetch initial candle snapshots (price + volume) before opening WS
        fetchInitialSnapshots();
        Request req = new Request.Builder().url(FINNHUB_WS_URL + apiKey).build();
        ws = client.newWebSocket(req, this);

        // Batch writer: drain up to BATCH_SIZE every second and append to CSV
        writer.scheduleAtFixedRate(this::writeBatch, 1, 1, TimeUnit.SECONDS);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        // Schedule periodic retry for failed symbols (default 300s)
        long retryInterval = 300L; // seconds
        try {
            String env = System.getenv("RETRY_INTERVAL_SECONDS");
            if (env != null && !env.isEmpty()) retryInterval = Long.parseLong(env);
        } catch (Exception ignored) {}
        retryExecutor.scheduleAtFixedRate(this::fetchFailedSnapshots, retryInterval, retryInterval, TimeUnit.SECONDS);

        System.out.println("Extractor started");
    }

    private void initializeCsv() {
        File f = new File(CSV_FILE);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f, false))) {
            w.write(CSV_HEADER);
            fileInitialized.set(true);
            System.out.println("Initialized CSV file (overwritten): " + CSV_FILE);
        } catch (IOException e) {
            System.err.println("Failed to initialize CSV: " + e.getMessage());
        }
    }

    private boolean fetchQuoteSnapshot(String symbol) {
        String quoteUrl = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol, apiKey);
        Request qreq = new Request.Builder().url(quoteUrl).get().build();
        try (Response qresp = client.newCall(qreq).execute()) {
            if (qresp.isSuccessful()) {
                String qbody = qresp.body() != null ? qresp.body().string() : "";
                QuoteResponse qr = gson.fromJson(qbody, QuoteResponse.class);
                if (qr != null && qr.c != null) {
                    long ts = qr.t != null && qr.t > 0 ? normalizeToMillis(qr.t) : System.currentTimeMillis();
                    String line = String.format("%s,%.2f,%d,%d,quote", symbol, qr.c, 0, ts);
                    queue.offer(line);
                    System.out.printf("Snapshot(quote) %s price=%.2f vol=0 ts=%d%n", symbol, qr.c, ts);
                    return true;
                }
            } else {
                System.err.printf("Quote request failed for %s: HTTP %d%n", symbol, qresp.code());
            }
        } catch (IOException e) {
            System.err.printf("Quote request error for %s: %s%n", symbol, e.getMessage());
        }
        return false;
    }

    // Fetch last-minute candles to get an initial price and volume snapshot per symbol.
    private void fetchInitialSnapshots() {
        System.out.println("Fetching initial candle snapshots...");
        for (String symbol : symbols) {
            if (!forceCandles && noCandle.contains(symbol)) {
                boolean ok = fetchQuoteSnapshot(symbol);
                if (ok) continue;
            }
            int maxRetries = 4;
            long backoff = 500L;
            boolean success = false;
            for (int attempt = 1; attempt <= maxRetries && !success; attempt++) {
                long nowSec = System.currentTimeMillis() / 1000L;
                String candleUrl = String.format("https://finnhub.io/api/v1/stock/candle?symbol=%s&resolution=1&from=%d&to=%d&token=%s",
                        symbol, nowSec - 60, nowSec, apiKey);
                Request creq = new Request.Builder().url(candleUrl).get().build();
                try (Response cresp = client.newCall(creq).execute()) {
                    if (cresp.isSuccessful()) {
                        String body = cresp.body() != null ? cresp.body().string() : "";
                        CandleResponse cr = gson.fromJson(body, CandleResponse.class);
                        if (cr != null && "ok".equalsIgnoreCase(cr.s) && cr.c != null && cr.c.length > 0) {
                            int idx = cr.c.length - 1;
                            double price = cr.c[idx];
                            long vol = (cr.v != null && cr.v.length > idx) ? cr.v[idx] : 0L;
                            long ts = (cr.t != null && cr.t.length > idx) ? normalizeToMillis(cr.t[idx]) : System.currentTimeMillis();
                            String line = String.format("%s,%.2f,%d,%d,candle", symbol, price, vol, ts);
                            queue.offer(line);
                            System.out.printf("Snapshot(candle) %s price=%.2f vol=%d ts=%d%n", symbol, price, vol, ts);
                            success = true;
                            break;
                        } else {
                            // fallback to /quote for price (volume unknown)
                            System.out.printf("No candle for %s (status=%s) — falling back to quote\n", symbol, cr != null ? cr.s : "null");
                            boolean ok = fetchQuoteSnapshot(symbol);
                            success = ok;
                            break;
                        }
                    } else {
                        System.err.printf("Candle request failed for %s: HTTP %d\n", symbol, cresp.code());
                        if (cresp.code() == 429 && attempt < maxRetries) {
                            long jitter = (long)(Math.random() * 200);
                            try { Thread.sleep(backoff + jitter); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                            backoff *= 2;
                        } else if (cresp.code() == 403 || cresp.code() == 404) {
                            System.err.printf("Candle not allowed for %s (HTTP %d). Falling back to quote.\n", symbol, cresp.code());
                            if (!forceCandles) noCandle.add(symbol);
                             // try quote fallback
                             String quoteUrl = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol, apiKey);
                             Request qreq = new Request.Builder().url(quoteUrl).get().build();
                             try (Response qresp = client.newCall(qreq).execute()) {
                                if (qresp.isSuccessful()) {
                                    String qbody = qresp.body() != null ? qresp.body().string() : "";
                                    QuoteResponse qr = gson.fromJson(qbody, QuoteResponse.class);
                                    if (qr != null && qr.c != null) {
                                        long ts = qr.t != null && qr.t > 0 ? normalizeToMillis(qr.t) : System.currentTimeMillis();
                                        String line = String.format("%s,%.2f,%d,%d,quote", symbol, qr.c, 0, ts);
                                        queue.offer(line);
                                        System.out.printf("Snapshot(quote-fallback) %s price=%.2f vol=0 ts=%d%n", symbol, qr.c, ts);
                                        success = true;
                                    }
                                } else {
                                    System.err.printf("Quote fallback failed for %s: HTTP %d\n", symbol, qresp.code());
                                }
                            } catch (IOException ioe) {
                                System.err.printf("Quote fallback error for %s: %s\n", symbol, ioe.getMessage());
                            }
                            break; // don't retry candle for 403/404
                        }
                    }
                } catch (IOException | JsonSyntaxException e) {
                    if (attempt < maxRetries) {
                        try { Thread.sleep(backoff + (long)(Math.random() * 200)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                        backoff *= 2;
                    }
                }
             }
             if (!success) {
                 failedSymbols.add(symbol);
             }
             // larger pause to be kinder to rate limits
             try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
         }
         System.out.println("Finished initial snapshots (queued).\n");
    }

    // Retry fetching snapshots for symbols that previously failed
    private void fetchFailedSnapshots() {
        if (failedSymbols.isEmpty()) return;
        System.out.println("Retrying failed symbols: " + failedSymbols.size());
        List<String> snapshot = new ArrayList<>(failedSymbols);
        for (String symbol : snapshot) {
            boolean ok = fetchSnapshotForSymbol(symbol);
            if (ok) {
                failedSymbols.remove(symbol);
                System.out.println("Retry succeeded for: " + symbol);
            } else {
                System.err.println("Retry still failed for: " + symbol);
            }
            try { Thread.sleep(300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
        }
    }

    // Try to fetch a single symbol's snapshot (candle then quote). Returns true on success.
    private boolean fetchSnapshotForSymbol(String symbol) {
        long nowSec = System.currentTimeMillis() / 1000L;
        String candleUrl = String.format("https://finnhub.io/api/v1/stock/candle?symbol=%s&resolution=1&from=%d&to=%d&token=%s",
                symbol, nowSec - 60, nowSec, apiKey);
        try {
            Request creq = new Request.Builder().url(candleUrl).get().build();
            try (Response cresp = client.newCall(creq).execute()) {
                if (cresp.isSuccessful()) {
                    String body = cresp.body() != null ? cresp.body().string() : "";
                    CandleResponse cr = gson.fromJson(body, CandleResponse.class);
                    if (cr != null && "ok".equalsIgnoreCase(cr.s) && cr.c != null && cr.c.length > 0) {
                        int idx = cr.c.length - 1;
                        double price = cr.c[idx];
                        long vol = (cr.v != null && cr.v.length > idx) ? cr.v[idx] : 0L;
                        long ts = (cr.t != null && cr.t.length > idx) ? normalizeToMillis(cr.t[idx]) : System.currentTimeMillis();
                        String line = String.format("%s,%.2f,%d,%d,candle", symbol, price, vol, ts);
                        queue.offer(line);
                        return true;
                    }
                } else {
                    // Non-successful candle response: handle common non-retryable cases
                    System.err.printf("Candle request failed for %s: HTTP %d\n", symbol, cresp.code());
                    if (cresp.code() == 429) {
                        return false; // let caller retry later
                    } else if (cresp.code() == 403 || cresp.code() == 404) {
                        System.err.printf("Candle not allowed for %s (HTTP %d). Falling back to quote.\n", symbol, cresp.code());
                        noCandle.add(symbol);
                         // try quote fallback
                         String quoteUrl = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s", symbol, apiKey);
                         Request qreq = new Request.Builder().url(quoteUrl).get().build();
                         try (Response qresp = client.newCall(qreq).execute()) {
                            if (qresp.isSuccessful()) {
                                String qbody = qresp.body() != null ? qresp.body().string() : "";
                                QuoteResponse qr = gson.fromJson(qbody, QuoteResponse.class);
                                if (qr != null && qr.c != null) {
                                    long ts = qr.t != null && qr.t > 0 ? normalizeToMillis(qr.t) : System.currentTimeMillis();
                                    String line = String.format("%s,%.2f,%d,%d,quote", symbol, qr.c, 0, ts);
                                    queue.offer(line);
                                    return true;
                                }
                            } else {
                                System.err.printf("Quote fallback failed for %s: HTTP %d\n", symbol, qresp.code());
                            }
                        } catch (IOException ioe) {
                            System.err.printf("Quote fallback error for %s: %s\n", symbol, ioe.getMessage());
                        }
                        return false;
                    }
                }
            } catch (Exception e) {
                // ignore, return false
            }
        } catch (Exception e) {
            // ignore, return false
        }
        return false;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("WebSocket opened, subscribing to symbols...");
        for (String s : symbols) {
            String msg = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", s);
            webSocket.send(msg);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            TradeResponse tr = gson.fromJson(text, TradeResponse.class);
            if (tr != null && "trade".equals(tr.type) && tr.data != null) {
                for (Trade t : tr.data) {
                    long ts = normalizeToMillis(t.t);
                    String line = String.format("%s,%.2f,%d,%d,trade", t.s, t.p, t.v, ts);
                    queue.offer(line);
                }
            }
        } catch (JsonSyntaxException e) {
            // ignore non-trade messages
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        System.err.println("WebSocket failure: " + t.getMessage());
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        System.out.println("WebSocket closing: " + code + " " + reason);
    }

    private void writeBatch() {
        List<String> batch = new ArrayList<>(BATCH_SIZE);
        queue.drainTo(batch, BATCH_SIZE);
        if (batch.isEmpty()) return;

        // Update latest map from drained batch
        for (String l : batch) {
            String sym = l.split(",", 2)[0];
            latest.put(sym, l);
        }

        // Overwrite CSV with current latest rows (use symbols order for stable output)
        File f = new File(CSV_FILE);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(f, false))) {
            w.write(CSV_HEADER);
            int written = 0;
            for (String sym : symbols) {
                String row = latest.get(sym);
                if (row != null) {
                    w.write(row);
                    w.newLine();
                    written++;
                }
            }
            System.out.printf("Wrote latest snapshot of %d symbols to %s%n", written, CSV_FILE);
        } catch (IOException e) {
            System.err.println("Error writing latest CSV: " + e.getMessage());
        }
    }

    private void shutdown() {
        System.out.println("Shutdown: flushing and closing...");
        if (ws != null) ws.close(1000, "shutdown");
        writer.shutdown();
        retryExecutor.shutdown();
        try { if (!writer.awaitTermination(5, TimeUnit.SECONDS)) writer.shutdownNow(); } catch (InterruptedException ignored) {}
        // Final flush
        writeBatch();
        client.dispatcher().executorService().shutdown();
        System.out.println("Shutdown complete");
    }

    // Simple DTOs for Gson
    private static class TradeResponse {
        String type;
        Trade[] data;
    }
    private static class Trade {
        String s; // symbol
        double p; // price
        long v;   // volume
        long t;   // timestamp
    }

    // Responses for REST endpoints
    @SuppressWarnings("unused")
    private static class CandleResponse {
        String s; // status
        double[] c;
        double[] h;
        double[] l;
        double[] o;
        long[] v;
        long[] t;
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

    private static long normalizeToMillis(long ts) {
        return ts < 1_000_000_000_000L ? ts * 1000L : ts;
    }

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("FINNHUB_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("Please set FINNHUB_API_KEY environment variable");
            return;
        }

        List<String> symbols = Arrays.asList(
            "AAPL","MSFT","AMZN","GOOGL","TSLA","NVDA","JPM","JNJ","V","PG",
            "UNH","HD","MA","BAC","DIS","PYPL","ADBE","NFLX","CMCSA","INTC",
            "PFE","CSCO","PEP","XOM","T","ABT","CRM","KO","WMT","ABBV",
            "MCD","NKE","MDT","COST","AVGO","QCOM","TXN","HON","UNP","LIN",
            "SBUX","CAT","IBM","GS","LOW","AMGN","CVX","DHR","LMT","BLK"
        );

        Extractor ex = new Extractor(apiKey, symbols);
        ex.start();

        // Keep main thread alive
        Thread.currentThread().join();
    }
}
