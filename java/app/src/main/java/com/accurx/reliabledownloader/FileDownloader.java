package com.accurx.reliabledownloader;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Stock Interface to be implemented for the exercise
 */
public interface FileDownloader
{
    /**
     * Cancels any in progress downloads
     */
    void CancelDownloads();

    /**
     * Downloads a file, trying to use reliable downloading if possible
     * @param contentFileUrl The url which the file is hosted at
     * @param localFilePath The local file path to save the file to
     * @param onProgressChanged A {@link Consumer<FileProgress>} for accepting callbacks on download progress
     * @return True or false, depending on if download completes and writes to file system okay
     */
    CompletableFuture<Boolean> DownloadFile(String contentFileUrl, String localFilePath, Consumer<FileProgress> onProgressChanged);
}
