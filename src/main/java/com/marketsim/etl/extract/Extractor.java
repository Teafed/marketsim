package src.main.java.com.marketsim.etl.extract;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Extractor class for fetching financial data from Finnhub and Polygon APIs.
 * <p>
 * This class is part of the ETL (Extract, Transform, Load) pipeline for market simulation.
 * It provides methods to fetch data from financial APIs and handles authentication,
 * request formatting, and response processing.
 * </p>
 * 
 * @author Oscar Lopez Group 11
 * @version 1.0
 */
public class Extractor {
    /** Default timeout in seconds for HTTP requests */
    private static final int TIMEOUT_SECONDS = 10;
    
    /** HTTP client instance used for making API requests */
    private final HttpClient httpClient;
    
    /** API key for accessing Finnhub financial data services */
    private final String finnhubApiKey;
    
    /** API key for accessing Polygon financial data services */
    private final String polygonApiKey;
    

    /**
     * Constructor that initializes the HttpClient and loads API keys from environment variables.
     * <p>
     * Sets up an HttpClient with appropriate timeout settings and loads the required
     * API keys from environment variables. Validates that both API keys are present
     * and non-empty.
     * </p>
     * 
     * @throws IllegalStateException if either FINNHUB_API_KEY or POLYGON_API_KEY environment
     *                               variables are not set or are empty
     */
    public Extractor() {

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        

        this.finnhubApiKey = System.getenv("FINNHUB_API_KEY");
        this.polygonApiKey = System.getenv("POLYGON_API_KEY");
        

        if (finnhubApiKey == null || finnhubApiKey.isEmpty()) {
            throw new IllegalStateException("FINNHUB_API_KEY environment variable is not set");
        }
        
        if (polygonApiKey == null || polygonApiKey.isEmpty()) {
            throw new IllegalStateException("POLYGON_API_KEY environment variable is not set");
        }
    }
    

    /**
     * Fetches data from the Finnhub API.
     * <p>
     * Constructs a properly formatted URL with the provided endpoint and the Finnhub API key,
     * sends a GET request to the Finnhub API, and returns the raw JSON response.
     * </p>
     * 
     * @param endpoint the API endpoint to fetch data from (e.g., "/quote?symbol=AAPL")
     * @return raw JSON response as a String
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted
     */
    public String fetchFromFinnhub(String endpoint) throws IOException, InterruptedException {
        String url = "https://finnhub.io/api/v1" + endpoint;
        if (!url.contains("?")) {
            url += "?";
        } else if (!url.endsWith("&")) {
            url += "&";
        }
        url += "token=" + finnhubApiKey;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    

    /**
     * Fetches data from the Polygon API.
     * <p>
     * Constructs a properly formatted URL with the provided endpoint and the Polygon API key,
     * sends a GET request to the Polygon API, and returns the raw JSON response.
     * </p>
     * 
     * @param endpoint the API endpoint to fetch data from (e.g., "/v2/aggs/ticker/AAPL/range/1/day/2023-01-01/2023-01-10")
     * @return raw JSON response as a String
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted
     */
    public String fetchFromPolygon(String endpoint) throws IOException, InterruptedException {
        String url = "https://api.polygon.io" + endpoint;
        if (!url.contains("?")) {
            url += "?";
        } else if (!url.endsWith("&")) {
            url += "&";
        }
        url += "apiKey=" + polygonApiKey;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    

    /**
     * Fetches data asynchronously from a given URL.
     * <p>
     * Sends an asynchronous GET request to the specified URL and returns a CompletableFuture
     * that will be completed with the response body when the request completes.
     * </p>
     * 
     * @param url the complete URL to fetch data from, including any required API keys
     * @return CompletableFuture containing the response as a String
     */
    public CompletableFuture<String> fetchAsync(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }
    

    /**
     * Main method demonstrating the usage of the Extractor class.
     * <p>
     * Provides examples of:
     * <ul>
     *   <li>Fetching a Finnhub stock quote for AAPL</li>
     *   <li>Fetching Polygon aggregate data for AAPL</li>
     *   <li>Using fetchAsync to request Finnhub data for TSLA</li>
     * </ul>
     * </p>
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            Extractor extractor = new Extractor();
            

            System.out.println("Fetching Finnhub stock quote for AAPL...");
            String finnhubResponse = extractor.fetchFromFinnhub("/quote?symbol=AAPL");
            System.out.println("Finnhub Response: " + finnhubResponse);
            System.out.println();
            

            System.out.println("Fetching Polygon aggregate data for AAPL...");
            String polygonResponse = extractor.fetchFromPolygon("/v2/aggs/ticker/AAPL/range/1/day/2023-01-01/2023-01-10");
            System.out.println("Polygon Response: " + polygonResponse);
            System.out.println();
            

            System.out.println("Fetching Finnhub data for TSLA asynchronously...");
            String asyncUrl = "https://finnhub.io/api/v1/quote?symbol=TSLA&token=" + System.getenv("FINNHUB_API_KEY");
            CompletableFuture<String> future = extractor.fetchAsync(asyncUrl);
            

            future.thenAccept(result -> {
                System.out.println("Async Finnhub Response: " + result);
            }).join();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}