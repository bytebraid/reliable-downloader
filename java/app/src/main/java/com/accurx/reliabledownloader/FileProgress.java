package com.accurx.reliabledownloader;

import java.time.Duration;
import java.time.Instant;

/**
 * Provided to a consumer for returning a {@link String} representation of the
 * ongoing download progress.
 */
public class FileProgress
{
    private Duration estimatedRemaining;
    private double progressPercent;
    private double remainingTimeSeconds;
    private Instant startTime;
    private long totalBytesDownloaded;
    private long totalFileSize;

    /**
     * Initialize the object, sets startTime when constructed. Rate calculations
     * are based on the deltas between calls to {@link #updateProgress(long)}
     *
     * @param totalFileSize			File size
     * @param totalBytesDownloaded	Bytes downloaded so far
     */
    public FileProgress(long totalFileSize, long totalBytesDownloaded) {
        this.totalFileSize = totalFileSize;
        this.totalBytesDownloaded = totalBytesDownloaded;
        this.estimatedRemaining = Duration.ofSeconds(100);
        this.progressPercent = 0d;
        this.startTime = Instant.now();
    }
    /**
     * Stock constructor.
     *
     * @param totalFileSize
     * @param totalBytesDownloaded
     * @param progressPercent
     * @param estimatedRemaining
     */
    public FileProgress(long totalFileSize, long totalBytesDownloaded, double progressPercent, Duration estimatedRemaining) {
        this.totalFileSize = totalFileSize;
        this.totalBytesDownloaded = totalBytesDownloaded;
        this.progressPercent = progressPercent;
        this.estimatedRemaining = estimatedRemaining;
        this.startTime = Instant.now();
    }

    public Duration getEstimatedRemaining() {
        return estimatedRemaining;
    }

    public long getPercent() {
        return Math.round(progressPercent);
    }

    public double getProgressPercent() {
        return progressPercent;
    }

    public long getTotalBytesDownloaded() {
        return totalBytesDownloaded;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    @Override
    public String toString() {
    	String remaining = String.format("%.2f", remainingTimeSeconds);
		/*
		 * return "FileProgress{" + "totalFileSize=" + totalFileSize +
		 * ", totalBytesDownloaded=" + totalBytesDownloaded + ", progressPercent=" +
		 * progressPercent + ", estimatedRemaining=" + remaining + '}';
		 */
    	return (getPercent()+"% ("+remaining+"s remaining)");
    }

    /**
     * Takes the chunkSize, calculates the rate of download, updates attributes
     * @param chunkBytes
     */
    public void updateProgress(long chunkBytes) {
        this.totalBytesDownloaded += chunkBytes;

        long elapsedMillis = Duration.between(startTime, Instant.now()).toMillis();

        // Calculate download rate in bytes/second
        double downloadRate = totalBytesDownloaded / (elapsedMillis / 1000.0);

        // Estimate remaining time in seconds
        double remainingBytes = totalFileSize - totalBytesDownloaded;
        remainingTimeSeconds = (downloadRate > 0) ? (remainingBytes / downloadRate) : Double.POSITIVE_INFINITY;

        this.estimatedRemaining = Duration.ofSeconds(Double.doubleToLongBits(remainingTimeSeconds));
        this.progressPercent = (totalBytesDownloaded / (double) totalFileSize) * 100;
    }
}