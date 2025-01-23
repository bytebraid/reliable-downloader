package com.accurx.reliabledownloader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.ClassOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Runs different configurations of {@link MyFileDownloader} to simulate typical successes
 * and failures. Arbitrary fudging implemented in {@link MyFileDownloadTestSubclass}, very
 * messy but suffices to demonstrate coverage etc.
 * 
 * <p>TODO separate out hardcoded values
 * <p>TODO implement network wrappers / disconnects
 * <p>TODO make the whole thing neater, use proper Mocking where applicable
 * <p>TODO remove ordering dependency, do better initializations and cleanUp()
 * @author ciaran@parallaxed.net
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class MyFileDownloaderTest {
	// A valid file with hash matching testMD5
	public static String testURL = "https://installer.demo.accurx.com/chain/3.182.57641.0/accuRx.Installer.Local.msi";
	// A valid file with a hash that doesn't match testMD5
	public static String testURL2 = "https://installer.demo.accurx.com/chain/4.227.10959.0/accuRx.Installer.Local.msi";
	// A quirky URL with a broken response
	public static String testURL3 = "https://radon.parallaxed.net/md5/index.html";
	// Expected hash value for testURL
	public static String testMD5 = "58d2c8553756fdb2e3d60246336e6a77";
	public String testPath = "inst.msi";
	// Expected Content-Length for testURL
	public Long testLen = 14565376l;
	MyFileDownloadTestSubclass dl = null;
	ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
	PrintStream originalOut = System.out;

	public MyFileDownloaderTest() {	
		this.dl = new MyFileDownloadTestSubclass(testURL, testPath);
		// Create a ByteArrayOutputStream to capture the output
		cleanUp();
		OutputStream teeStream = new TeeOutputStream(System.out, capturedOutput);
		PrintStream teePrintStream = new PrintStream(teeStream);
		System.setOut(teePrintStream);
	}

	public void cleanUp(String path) {
		File testFile = new File(path);
		if (testFile.exists())
			testFile.delete();
	}

	public void cleanUp() {
		cleanUp(testPath);
		if (dl.getDownloadTarget() != null)
			cleanUp(dl.getDownloadTarget().getPath());
	}

	@Test
	@Order(1)
	public void setUp() throws Exception {
		dl.setupTest();
	}

	@Test
	@Order(2)
	public void testHeaders() throws Exception {
		// Check Known Content-MD5 header of a test resource
		assertEquals(dl.getMD5(), testMD5);
		assertEquals(dl.getContentLength(), testLen);
	}

	@Test
	@Order(3)
	public void testPartialDownload() {
		assertTrue(dl.testPartial());
		String capturedString = capturedOutput.toString();
		assertTrue(capturedString.contains("Initiating partial download"));
	}

	@Test
	@Order(4)
	public void testAlreadyExists() {
		assertTrue(dl.testPartial());
		String capturedString = capturedOutput.toString();
		assertTrue(capturedString.contains("already exists with MD5"));
		cleanUp();
	}

	@Test
	@Order(5)
	public void testNoRanges() throws Exception {
		// Check Known Content-MD5 header of a test resource
		assertTrue(dl.testFull());
		String capturedString = capturedOutput.toString();
		assertTrue(capturedString.contains("Initiating full download"));
		cleanUp();
	}

	
	@Test	  
	@Order(6) 
	public void testMD5Mismatch() throws Exception {
		dl.setContentUrl(testURL2);
		assertFalse(dl.testFull()); 
		String capturedString = capturedOutput.toString();
		assertTrue(capturedString.contains("failed hash check")); 
		cleanUp();
	}
	
	@Test	  
	@Order(7) 
	public void testBadURI() throws Exception {
		dl.setContentUrl(testURL3);
		cleanUp();
		assertTrue(dl.testBadAttempt());
		// dl.setupTest();
		cleanUp();
		assertFalse(dl.testFull());
		cleanUp();

	}	
	
	@Test	  
	@Order(98) 
	public void testApp() throws Exception {
		App.run(true);
		String capturedString = capturedOutput.toString();
		assertTrue(capturedString.contains("Download app finished"));
	}		
	
	@Order(99)
	@Test
	public void finishUp() {
		System.setOut(originalOut);
		cleanUp();
	}
}

/**
 * Overridden MyFileDownloader to mess with the internals and trigger 
 * exceptional behaviours and approach full coverage.
 */
class MyFileDownloadTestSubclass extends MyFileDownloader {
	String url;
	String path;

	public MyFileDownloadTestSubclass(String url, String path) {
		this.url = url;
		this.path = path;
	}

	@Override
	protected void ProcessHeaders(String url) {
		// no-op, so DownloadFile doesn't re-process the headers
	}

	public void setupTest() throws Exception {
		setLocalFilePath(path);
		super.ProcessHeaders(this.url);
		// this.lockTempFile(path, this.md5Hash);

	}

	public boolean testPartial() {
		return runDownload();
	}

	public boolean testFull() {
		this.setAcceptRanges(null);
		return runDownload();
	}
	
	public boolean testBadAttempt() {
		try {
			this.attemptPartialDownload(url,100,0,256);
			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean runDownload() {
		CompletableFuture<Boolean> done = DownloadFile(url, path,
				(progress) -> System.out.println("Percent progress is " + progress));
		return done.join();
	}

	public String getMD5() {
		return this.md5Hash;
	}

	public void setContentUrl(String url) {
		this.url = url;
	}

}

/**
 * Capture stdout, duplicate stream to original System.out
 */
class TeeOutputStream extends OutputStream {
	private final OutputStream stream1;
	private final OutputStream stream2;

	public TeeOutputStream(OutputStream stream1, OutputStream stream2) {
		this.stream1 = stream1;
		this.stream2 = stream2;
	}

	@Override
	public void write(int b) throws IOException {
		stream1.write(b);
		stream2.write(b);
	}

	@Override
	public void flush() throws IOException {
		stream1.flush();
		stream2.flush();
	}

	@Override
	public void close() throws IOException {
		stream1.close();
		stream2.close();
	}
}