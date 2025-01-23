
package com.accurx.reliabledownloader;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * An example invocation of {@link MyFileDownloader}, with example values.
 *
 * Returns suitable exit codes to indicate success.
 *
 * Can be interrupted to cancel the download threads.
 */
public class App {

	public static void main(String[] args) {
    	run(false);
    }

    /**
	 * Start downloading...
	 * TODO accept CLI arguments for target url and download file
	 * @param withTests  disables return codes so tests don't exit prematurely
	 */
	public static void run(boolean withTests) {

        // If this url 404's, you can get a live one from https://installer.demo.accurx.com/chain/latest.json.
        var exampleUrl = "https://installer.demo.accurx.com/chain/3.182.57641.0/accuRx.Installer.Local.msi";

        // exampleUrl = "https://radon.parallaxed.net/md5/ubuntu.iso";
        var exampleFilePath = "C:/temp/myfirstdownload.msi";
        // exampleFilePath = "C:/temp/ubuntu.iso";
        var fileDownloader = new MyFileDownloader();
        Signal.handle(new Signal("INT"), new SignalHandler() {
            @Override
			public void handle(Signal signal) {
                System.out.println("Exiting download...");
                fileDownloader.CancelDownloads();
                System.out.println("Download app interrupted");
            	if (!withTests)
				 {
					System.exit(1); // returns non-zero exit status indicating fault
				}
            }
        });
        System.out.println("Download starting, press Ctrl+C to exit...");
        fileDownloader.DownloadFile(exampleUrl, exampleFilePath, (progress) -> System.out.println("Percent progress is " + progress));
        System.out.println("Download app finished");
        if (!withTests) {
			System.exit(0);
		}
	}
}
