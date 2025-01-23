package com.accurx.reliabledownloader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility for returning async {@link HttpResponse} objects.
 */
public class DefaultWebSystemCall implements WebSystemCalls {

    private final HttpClient client = HttpClient.newBuilder()
            .build();
    /**
     * TODO Could be reimplemented with {@link HttpResponse}<InputStream> and extended
     * to indicate stream read progress from the response body.
     */
    @Override
    public CompletableFuture<HttpResponse<byte[]>> DownloadContent(String url) {
        var request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();

        CompletableFuture<HttpResponse<byte[]>> res = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .orTimeout(2, TimeUnit.MINUTES);
        return res;
    }

    @Override
    /**
     * Returns partial content for the resource
     */
    public CompletableFuture<HttpResponse<byte[]>> DownloadPartialContent(String url, long from, long to) {
        var request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("Range", String.format("bytes=%s-%s", from, to))
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
            .orTimeout(2, TimeUnit.MINUTES);
    }


    /**
     * Makes a HEAD request to retrieve headers
     * @param url	the resource to fetch
     * @return a {@link CompletableFuture<HttpResponse<Void>>} a future response
     */
    @Override
    public CompletableFuture<HttpResponse<Void>> GetHeaders(String url) {
        var request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .HEAD()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

}
