package com.accurx.reliabledownloader;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/**
 * <p>A reliable implementation of {@link FileDownloader} that is tolerant to network
 * disruptions.
 *
 * <p>Once {@link #DownloadFile(String, String, Consumer)} is invoked, this class will
 * dispatch asynchronous HTTP requests and block until interrupted or {@link #CancelDownloads()}
 * is called.
 *
 * <p>TODO lint and format code and docs properly
 *
 * <p>TODO requires proper logging, using Log4j or similar instead of System.out statements,
 * including an error log traceable back to a client if telemetry is provided.
 *
 * <p>TODO Requires fault reporting, wherever mentioned. Could be a post back to a URI
 * or persistent fault logs communicated with telemetry whenever a network connection
 * becomes available. Information could include error origin, runtime information
 * and other local data that helps an operations team debug the issue for a client.
 *
 * <p>TODO This class is too big and needs to split out concerns to other classes
 *
 * <p>TODO progress reporting only works when ranges are supported by the download URI,
 * this could be remedied by implementing a custom wrap with {@link BodyHandlers#ofInputStream()}
 *
 * <p>TODO exponential backoff is implemented up to {@link #maxDelay}, there are alternative strategies
 *
 * @author ciaran@parallaxed.net
 */
public class MyFileDownloader implements FileDownloader
{
	protected String contentFileUrl = null;

	protected Long contentLength = 0l;

	// Determines whether the download loops continue to run
	// TODO better cancellation logic on SIGINT or other signals
	private boolean downloading = false;

	// TODO better initialization of class properties
	protected File downloadTarget = null;
	private String downloadTargetPath;
	protected String localFilePath = null;
	private long maxDelay = 10_000;

	protected String md5Hash = null;
	protected boolean rangesSupported = false;

	private WebSystemCalls wsc = new DefaultWebSystemCall();

	/**
	 * Empty constructor
	 */
	public MyFileDownloader() {

	}

	/**
     * Return a full file download attempt
     *
     * TODO refactor into a single function to handle full and partial downloads
     *
     * @param url 		The URL to download
     * @param delayMillis Initially delay to wait before retrying
     */
    private CompletableFuture<Void> attemptDownload(String url, long delayMillis) {
        System.out.println("Attempting to download from URL: " + url);
        WebSystemCalls wsc = new DefaultWebSystemCall();
        // Call DownloadContent asynchronously

        return wsc.DownloadContent(url).thenCompose(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            // Write the response content to a file
                            writeResponseToFile(response);
                            System.out.println("Download completed successfully.");
                            return CompletableFuture.completedFuture(null); // Success
                        } catch (IOException e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    } else {
                    	CancelDownloads();
                        return CompletableFuture.failedFuture(
                                new IOException("Failed with HTTP status code: " + response.statusCode()));
                    }
                })
		        .exceptionally(error -> {
		            System.out.println("Download failed: " + error.getMessage());
		            return error; // Propagate the error to the next stage
		        })
		        .thenCompose(error -> {
		        	//if (error.getClass() == RuntimeException.class) {
		        	//	return CompletableFuture.completedFuture(null);
		        	//}
		        	if (downloading && error instanceof Throwable) {
		            	 System.out.println("Retrying in " + delayMillis + " ms...");

		                long nextDelay = Math.min(delayMillis * 2, maxDelay);

		                // Schedule retry using a delayed executor
		                return CompletableFuture.runAsync(() -> {
		                    // No operation, just wait for the delay
		                }, CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS))
		                        .thenCompose(ignored -> attemptDownload(url, nextDelay));
		            }
		        	return CompletableFuture.completedFuture(null);
		        });
    }
	/**
     * Downloads a chunk of a resource, retrying if an exception is not fatal. Called by
     *  {@link #DownloadFile(String, String, Consumer)}
     * @param url			the resource URL to fetch
     * @param delayMillis	initial delay before retry
     * @param start			start of byte range
     * @param end			end of byte range
     * @return {@link CompletableFuture}<Void>
     */
    protected CompletableFuture<Void> attemptPartialDownload(String url, long delayMillis, long start, long end) {
        // Call DownloadPartialContent asynchronously
        return wsc.DownloadPartialContent(url, start, end)
                .thenCompose(response -> {
                    if (response.statusCode() == 206) { // HTTP 206: Partial Content
                        try {
                            // Append the partial content to the file
                        	writeResponseToFile(response);
                            // System.out.println("Downloaded byte range " + start + "-" + end + " successfully.");
                            return CompletableFuture.completedFuture(null); // Success
                        } catch (IOException e) {
                            return CompletableFuture.failedFuture(e);
                        }
                    } else {
                    	downloading = false;
                        return CompletableFuture.failedFuture(
                                new IOException("Failed with HTTP status code: " + response.statusCode()));
                    }
                })
                .exceptionally(error -> {
                    System.out.println("Download of byte range " + start + "-" + end + " failed: " + error.getMessage());
                    return error; // Propagate
                })
                .thenCompose(error -> {
                    if (downloading && error instanceof Throwable) {
                        System.out.println("Retrying byte range " + start + "-" + end + " in " + delayMillis + " ms...");

                        long nextDelay = Math.min(delayMillis * 2, maxDelay);

                        // Schedule retry using a delayed executor
                        return CompletableFuture.runAsync(() -> {
                            System.out.println(nextDelay+" seconds until retry...");
                        }, CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS))
                                .thenCompose(ignored -> attemptPartialDownload(url, nextDelay, start, end));
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }
	/**
     * Will set loop conditions to false.
     *
     * <p>TODO a more graceful exit
     */
    @Override
	public void CancelDownloads() {
        this.downloading = false;
    }

	/**
     * Set {@link #downloading} to false and remove any temp files
     */
    private void cleanUp() {
    	CancelDownloads();
    	System.out.println("Deleting temporary file");
    	downloadTarget.delete();
    }

	/**
	 * Attempts a full or partial download of the {@link #contentFileUrl}, checks
	 * download integrity and renames the file to {@link #localFilePath}.
	 *
	 * <p>TODO throw more sensible exceptions, refactor into overloaded methods (range/no-range) and call appropriately
	 * @param contentFileUrl	the resource to download
	 * @param localFilePath		the final downloaded file path
	 * @param onProgressChanged a consumer to receive {@link FileProgress} updates
	 */
    @Override
	public CompletableFuture<Boolean> DownloadFile(String contentFileUrl, String localFilePath,
    		Consumer<FileProgress> onProgressChanged) {
    	// TODO tidy this up
    	setContentFileUrl(contentFileUrl);
    	setLocalFilePath(localFilePath);
        try {
            while (true) {
            	// Will not proceed if headers are missing
            	ProcessHeaders(this.contentFileUrl);

				if (FileUtils.checkFileMd5(localFilePath, this.md5Hash)) {
					System.out.println("File "+localFilePath+" already exists with MD5 "+this.md5Hash);
					return CompletableFuture.completedFuture(true);
				}


				setTempFileWithHash(this.localFilePath, this.md5Hash);
        		this.downloading = true;

        		// TODO r
            	if (!this.rangesSupported) {
            		System.out.println("No range support, re-creating file: "+downloadTargetPath);
            		downloadTarget.delete(); downloadTarget.createNewFile();
            		System.out.println("Initiating full download");
            		waitForDownload(contentFileUrl, maxDelay);
            	}
            	else {
            		System.out.println("Initiating partial download");
            		// TODO get start byte from end of open partial file
            	    long chunkSize = 1024 * 1024; // 1 MB
            	    long totalSize = this.contentLength;
            	    long initialDelay = 500; // Start retry delay in milliseconds
            	    long targetLength = downloadTarget.length();
            	    FileProgress progress = new FileProgress(contentLength, 0);

            	    if (targetLength > 0) {
						System.out.println("Resuming existing download from byte "+ targetLength);
					}
            	    // Iterate downloading chunks until complete
            	    for (long start = targetLength;  start < totalSize; start += chunkSize) {
            	    	long end = Math.min(start + chunkSize - 1, totalSize - 1);
            	        waitForPartialDownload(contentFileUrl, downloadTargetPath, start, end, initialDelay);
            	        progress.updateProgress(end-start);
            	        onProgressChanged.accept(progress);
            	    }
            	}
            	// TODO be neater
            	if (!FileUtils.checkFileMd5(downloadTargetPath, this.md5Hash)) {
					throw new RuntimeException("Downloaded content failed hash check, terminating");
				}

            	System.out.println("Successful hash check, renaming to "+localFilePath);
            	downloadTarget.renameTo(new File(localFilePath));
            	return CompletableFuture.completedFuture(true);
            }
        } catch (Exception e) {
        	// TODO we do not resume or retry on these failure conditions, usually
        	// missing or incorrect headers
        	System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally {

		}
        // Download failed, return false
       	return CompletableFuture.completedFuture(false);
    }

	/**
     * Wait for the response headers synchronously, return null if the header is not present, or
     * a {@link String} value present.
     * @param responseFuture	a {@link CompletableFuture<HttpResponse<Void>>}
     * @param headerName		the name of the header
     * @return the {@link String} value or null
     * @throws NoSuchElementException
     */
    private String extractHeader (
    		CompletableFuture<HttpResponse<Void>> responseFuture, String headerName) {
    	CompletableFuture<Optional<String>> val = responseFuture.thenApply(response -> {
        	System.out.print(headerName+": ");
            Optional<String> v = response.headers().firstValue(headerName);
            System.out.println(v.orElse("not found"));
            return v;
    	});

    	Optional<String> opt = val.join();
    	if (opt.isEmpty()) {
			return null;
		}
    	return opt.get();

    }

	/**
	 * @return the contentFileUrl
	 */
	protected String getContentFileUrl() {
		return contentFileUrl;
	}
	/**
	 * @return the contentLength
	 */
	public Long getContentLength() {
		return contentLength;
	}

	// Getters and setters for subclasses
	/**
	 * @return the downloadTarget
	 */
	protected File getDownloadTarget() {
		return downloadTarget;
	}

	/**
	 * @return the localFilePath
	 */
	protected String getLocalFilePath() {
		return localFilePath;
	}

    /**
	 * Calls {@link WebSystemCalls#GetHeaders(String)}, then populates the class properties
	 * with the values returned. If essential headers are missing an exception is thrown.
	 *
	 * @param contentFileUrl 	the resource to query
	 * @throws Exception
	 */
	protected void ProcessHeaders(String contentFileUrl) throws Exception {
    	CompletableFuture<HttpResponse<Void>> res = wsc.GetHeaders(contentFileUrl);
    	setContentLength(extractHeader(res,"Content-Length"));
    	setMD5HashFromHeader(extractHeader(res,"Content-MD5"));
    	setAcceptRanges(extractHeader(res,"accept-ranges"));
    	// TODO will throw a fatal exception if Content-MD5 header is not available
    	System.out.println("MD5 digest: "+this.md5Hash);
	}

    /**
     * Configures the downloader with range support {@link #rangesSupported rangesSupported}
     */
    protected void setAcceptRanges(String ranges) {
    	if (ranges != null) {
    		if ("bytes".equalsIgnoreCase(ranges)) {
    			this.rangesSupported = true;
    			return;
    		}
    	}
    	this.rangesSupported = false;
    }

    /**
	 * @param contentFileUrl the contentFileUrl to set
	 */
	protected void setContentFileUrl(String contentFileUrl) {
		this.contentFileUrl = contentFileUrl;
	}

    /**
     * Sets the expected length of the content to be downloaded
     * @param length
     */
    private void setContentLength(String length) {
    	if (length == null) {
			return;
		}
    	this.contentLength = Long.decode(length);
    }

    /**
	 * @param downloadTarget the downloadTarget to set
	 */
	protected void setDownloadTarget(File downloadTarget) {
		this.downloadTarget = downloadTarget;
	}

    /**
	 * Set the destination path for the download
	 * @param localFilePath the localFilePath to set
	 */
	protected void setLocalFilePath(String localFilePath) {
		this.localFilePath = localFilePath;
	}

    /**
     * Sets {@link #md5Hash} from it's encoded value.
     * @param contentMD5	The Base64 value of the Content-MD5 header
     *
     */
    protected void setMD5HashFromHeader(String contentMD5) {
        StringBuilder sb = new StringBuilder();
    	try {
            byte[] md5Bytes = Base64.getDecoder().decode(contentMD5);
            for (byte b : md5Bytes) {
                sb.append(String.format("%02x", b));
            }
            this.md5Hash = sb.toString();
            return;
    	}
    	catch (Exception e) {
    		// TODO Add fault reporting
	   		e.printStackTrace();
    	}
    	throw new RuntimeException("No MD5 hash decoded");
    }
    /**
	 * Opens a new file based on the given {@link #localFilePath} and {@link #md5Hash},
	 * should be called with the MD5 ASCII digest as extracted from a pre-flight HEAD
	 * request
	 * @param localFilePath		final intended filename used as a prefix for the temp file
	 * @param md5Hash			expected md5hash of file when fully downloaded
	 * @throws IOException
	 */
	protected void setTempFileWithHash(String localFilePath, String md5Hash) throws IOException {
    	this.downloadTargetPath = localFilePath+"."+md5Hash+".part";
    	File target = FileUtils.openOrCreateFile(downloadTargetPath);
    	setDownloadTarget(target);
	}

    /**
     * Run a while loop while {@link #downloading} is true, continue retrying.
     *
     * TODO refactor into a single function to wait partially or wholly
     *
     * @param url 		The URL to download
     * @param initialDelay Initially delay to wait before retrying
     */
    private void waitForDownload(String url, long initialDelay) {
        while (downloading) {
            try {
                attemptDownload(url, initialDelay).join(); // Wait for the partial download to complete
                System.out.println("Successfully downloaded.");
                break; // Exit the loop once the download is successful
            } catch (Exception e) {
                System.out.println("Retrying...");
            }
        }
    }
    /**
     * Run a while loop while {@link #downloading} is true, continue retrying.
     *
     * TODO refactor into a single function to wait partially or wholly, use non-blocking
     *
     * @param url 		The URL to download
     * @param fileName 	The file name to write to
     * @param start		The start point (usually the .length() of the partial file)
     * @param end		The total size (usually as given by the Content-Length header)
     * @param initialDelay Initially delay to wait before retrying
     */
    private void waitForPartialDownload(String url, String fileName, long start, long end, long initialDelay) {
        while (downloading) {
            try {
                attemptPartialDownload(url, initialDelay, start, end).join(); // Wait for the partial download to complete
                System.out.println("Successfully downloaded byte range " + start + "-" + end + ".");
                break; // Exit the loop once the download is successful
            } catch (Exception e) {
                System.out.println("Download of byte range " + start + "-" + end + " failed: " + e.getMessage());
                System.out.println("Retrying...");
            }
        }
    }

    /**
     * Writes the response bytes to the partial file path of {@link #downloadTarget}
     *
     * @param response		The 206 or 200 response body
     * @throws IOException
     */
    private void writeResponseToFile(HttpResponse<byte[]> response) throws IOException {
        // Define the directory and file path
    	Path filePath = Path.of(downloadTargetPath);
        // Write the response body to the file
        Files.write(filePath, response.body(), StandardOpenOption.APPEND);
        // System.out.println("Bytes written successfully to: " + filePath);
    }
}
