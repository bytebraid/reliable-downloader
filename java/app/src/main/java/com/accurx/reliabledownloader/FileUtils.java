package com.accurx.reliabledownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Static helpers for handling files, checking hashes.
 * <p>TODO improve or integrate this better
 * @author ciaran@parallaxed.net
 */
public class FileUtils {

    /**
	 *
	 * @param filePath		Path to the file for hashing
	 * @param expectedMd5	Expected MD5 has of the file at filePath
	 * @return				Boolean true or false
	 * @throws IOException	If the file cannot be found
	 * @throws NoSuchAlgorithmException If the algorithm is not found
	 */
	public static boolean checkFileMd5(String filePath, String expectedMd5) throws IOException, NoSuchAlgorithmException {
	    File file = new File(filePath);
	    if (!file.exists()) {
//	        throw new IOException("File does not exist: " + filePath);
	    	return false;
	    }

	    // Compute the MD5 hash of the file
	    MessageDigest md = MessageDigest.getInstance("MD5");
	    try (FileInputStream fis = new FileInputStream(file)) {
	        byte[] buffer = new byte[1024];
	        int bytesRead;
	        while ((bytesRead = fis.read(buffer)) != -1) {
	            md.update(buffer, 0, bytesRead);
	        }
	    }

	    // Convert the computed MD5 hash to a hexadecimal string
	    byte[] md5Bytes = md.digest();
	    StringBuilder sb = new StringBuilder();
	    for (byte b : md5Bytes) {
	        sb.append(String.format("%02x", b));
	    }
	    String computedMd5 = sb.toString();
	    System.out.println("MD5 check "+filePath+" - expected "+expectedMd5+" | actual "+computedMd5);
	    // Compare the computed MD5 with the expected value
	    return computedMd5.equalsIgnoreCase(expectedMd5);

	}

	/**
     * Gets the file at the provided path if it exists. If the file does not exist,
     * creates a new file with the specified name in the same directory.
     *
     * TODO implement context management to destruct / remove the file as needed
     *
     * @param filePath    The path of the file to check.
     * @return The existing file or the newly created file.
     * @throws IOException If an error occurs during file creation.
     */
    public static File openOrCreateFile(String filePath) throws IOException {
        // Create a File object for the specified path
        File file = new File(filePath);

        if (file.exists()) {
            // File exists, return its absolute path
            System.out.println("File exists: " + file.getAbsolutePath());
            return file;
        } else {
            // File doesn't exist, ensure parent directories exist
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("Failed to create parent directories for: " + filePath);
                }
            }
            // Create the file
            if (!file.createNewFile()) {
            	throw new IOException("Failed to create the file: " + filePath);
            }
        }
        System.out.println("New file created: " + file.getAbsolutePath());
        // Return the full absolute path of the file
        return file;
    }
}