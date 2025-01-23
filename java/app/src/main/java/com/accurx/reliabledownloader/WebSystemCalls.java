package com.accurx.reliabledownloader;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
/**
 * Stock Interface to be implemented for the exercise
 */
public interface WebSystemCalls {

    /**
     * Does a simple HTTP GET to download content from a URL
     */
    CompletableFuture<HttpResponse<byte[]>> DownloadContent(String url);

    /**
     * Does an HTTP GET but with a range specified to download partial content (if supported)
     * @param from From value, in bytes
     * @param to To value, in bytes
     */
    CompletableFuture<HttpResponse<byte[]>> DownloadPartialContent(String url, long from, long to);

    /**
     * Does an HTTP HEAD REST call to just get the headers for a URL
     */
    CompletableFuture<HttpResponse<Void>> GetHeaders(String url);
}
