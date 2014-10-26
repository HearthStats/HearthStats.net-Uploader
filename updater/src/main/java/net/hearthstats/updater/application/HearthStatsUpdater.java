package net.hearthstats.updater.application;

import net.hearthstats.updater.UpdaterConfiguration;
import net.hearthstats.updater.exception.UpdaterException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


class HearthStatsUpdater implements ActionListener {

  private static final Set<String> FILES_TO_SKIP = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
    "instructions-osx.txt"
  )));

  private static final int BUFFER_SIZE = 8096;  // 8kb

  private final ProgressWindow window;

  private final String version;
  private final long assetId;
  private final String hearthstatsLocation;
  private final String downloadFile;

  private SwingWorker<Object, Object> downloadWorker;

  private HearthStatsUpdater(String version, long assetId, String hearthstatsLocation, String downloadFile) {
    window = new ProgressWindow(this);
    this.version = version;
    this.assetId = assetId;
    this.hearthstatsLocation = hearthstatsLocation;
    this.downloadFile = downloadFile;

    window.open();
  }


  private void download() {
    downloadWorker = new SwingWorker<Object, Object>() {
      private boolean errorOccurred = false;

      @Override
      protected Object doInBackground() throws Exception {

        window.setProgress("Starting download...");
        window.log("Downloading version " + version + " of the HearthStats Companion.");
        window.enableCancelButton();

        String assetUrlString = UpdaterConfiguration.getNewAssetApiUrl(assetId);
        boolean downloadedFromNewUrl = downloadAsset(assetUrlString);
        if (!downloadedFromNewUrl) {
          assetUrlString = UpdaterConfiguration.getOldAssetApiUrl(assetId);
          downloadAsset(assetUrlString);
        }

        return null;
      }


      private boolean downloadAsset(String assetUrlString) {
        String currentUrlString = assetUrlString;

        try {
          URL assetUrl = new URL(assetUrlString);

          HttpURLConnection connection = (HttpURLConnection) assetUrl.openConnection();
          connection.addRequestProperty("User-Agent", UpdaterConfiguration.getClientUserAgent());
          connection.addRequestProperty("Accept", "application/octet-stream");

          // During development you can set an OAuth token to test with draft releases.
          if (!UpdaterConfiguration.getGitHubOAuthToken().isEmpty()) {
            connection.addRequestProperty("Authorization", "token " + UpdaterConfiguration.getGitHubOAuthToken());
          }

          // Do not redirect automatically because we should not pass GitHub authorisation tokens to a redirected URL,
          // such as to Amazon S3 where most GitHub binaries are stored.
          connection.setInstanceFollowRedirects(false);

          logRequestHeaders(connection);
          connection.connect();
          logResponseHeaders(connection);

          int responseCode = connection.getResponseCode();
          if (responseCode == 404) {
            // Asset was not found
            return false;
          }
          if (responseCode == 302 || responseCode == 307) {
            // We have been redirected to the download, which is the normal behaviour
            String redirectUrlString = connection.getHeaderField("location");
            currentUrlString = redirectUrlString;
            connection.disconnect();

            URL redirectUrl = new URL(redirectUrlString);
            System.out.println("Downloading from " + urlWithoutQuery(redirectUrl));
            connection = (HttpURLConnection) redirectUrl.openConnection();
            connection.addRequestProperty("User-Agent", UpdaterConfiguration.getClientUserAgent());

            logRequestHeaders(connection);
            connection.connect();
            logResponseHeaders(connection);
          }

          // Download the file
          byte[] buffer = new byte[BUFFER_SIZE];
          int bytesRead = -1;
          long totalBytesRead = 0;
          long fileSize = connection.getContentLengthLong();

          window.setProgress("Downloaded", 0, (int) fileSize);
          window.log(String.format("File size is %1$.1fMB.", fileSize / 1048576f));
          try (InputStream in = connection.getInputStream();
               FileOutputStream fos = new FileOutputStream(downloadFile)) {
            while ((bytesRead = in.read(buffer)) != -1 && !isCancelled()) {
              fos.write(buffer, 0, bytesRead);
              totalBytesRead += bytesRead;
              int percentCompleted = (int) (totalBytesRead * 100 / fileSize);
              setProgress(percentCompleted > 100 ? 100 : percentCompleted);
              window.setProgress("Downloaded", (int) totalBytesRead, (int) fileSize);
            }
          }

          return true;

        } catch (IOException e) {
          String error = "Unable to open connection to URL " + currentUrlString + " due to exception " + e.getMessage();
          window.log(error);
          errorOccurred = true;
          throw new UpdaterException(error, e);
        }
      }


      @Override
      protected void done() {
        window.disableCancelButton();

        if (!isCancelled() && !errorOccurred) {
          window.setProgress("Download Complete");
          window.log("Download complete.");
          extractZip();
        } else {
          window.setProgress("Download Cancelled");
          window.log("Download cancelled. Please restart the updater if you want to try again.");
        }
      }
    };

    downloadWorker.execute();

  }


  private void extractZip() {
    SwingWorker<Object, Object> extractWorker = new SwingWorker<Object, Object>() {
      private boolean errorOccurred = false;

      @Override
      protected Object doInBackground() throws Exception {
        window.log("Extracting " + version + " to " + hearthstatsLocation + "...");

        File updateZip = new File(downloadFile);
        if (updateZip.isFile()) {
          byte[] buffer = new byte[1024];

          try {
            // create output directory if it does not exist
            File folder = new File(hearthstatsLocation);
            if (!folder.exists()) {
              folder.mkdir();
            }

            // get the zip file content
            ZipInputStream zis = new ZipInputStream(new FileInputStream(updateZip.getPath()));
            // get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
              String fileName = ze.getName();
              if (!FILES_TO_SKIP.contains(fileName)) {
                File newFile = new File(hearthstatsLocation + File.separator + fileName);
                System.out.println("Unzipping file: " + newFile.getAbsoluteFile());

                // Create parent folders for files in the archive because FileOutputStream expects them to exist
                new File(newFile.getParent()).mkdirs();

                if (!ze.isDirectory()) {
                  FileOutputStream fos = new FileOutputStream(newFile);
                  try {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                      fos.write(buffer, 0, len);
                    }
                  } finally {
                    fos.close();
                  }
                }
              }
              ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

            System.out.println("Done");

          } catch (IOException ex) {
            String error = "Unable to uncompress file " + updateZip.getPath() + " due to exception " + ex.getMessage();
            window.log(error);
            errorOccurred = true;
            throw new UpdaterException(error, ex);
          }

          window.log("HearthStats Companion is now updated.");
        } else {
          window.log("Updater Error: unable to locate " + updateZip.getPath());
        }

        return null;
      }

      @Override
      protected void done() {
        if (!isCancelled() && !errorOccurred) {
          runMain();
        }
      }

    };

    extractWorker.execute();
  }


  public void runMain() {
    window.log("Update complete. Attempting to restart...");
    try {
      // Attempt to open HearthStats
      switch(getOperatingSystem()) {
        case "WINDOWS":
          Runtime.getRuntime().exec("HearthStats.exe");
          break;
        case "OSX":
          Desktop.getDesktop().open(new File(hearthstatsLocation + "/HearthStats.app"));
          break;
      }
      window.close();

      // If no exception occurred then quit
      System.exit(0);

    } catch (Exception e) {
      window.log("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getID() == ProgressWindow.EVENT_CANCEL) {
      if (downloadWorker != null && !downloadWorker.isDone()) {
        System.out.println("Cancel download has been requested");
        downloadWorker.cancel(false);
      }
    }
  }


  /**
   * @param args
   */
  public static void main(String[] args) {
    Map<String, String> arguments = getCommandArguments(args);

    HearthStatsUpdater updater = new HearthStatsUpdater(
      arguments.get("version"),
      Long.parseLong(arguments.get("assetId")),
      arguments.get("hearthstatsLocation"),
      arguments.get("downloadFile")
    );

    updater.download();

  }


  private static Map<String, String> getCommandArguments(String[] args) {
    Map<String, String> result = new HashMap<>();

    for (String arg : args) {
      String[] argSplit = arg.split("=");
      if (argSplit.length == 2) {
        result.put(argSplit[0], argSplit[1]);
      }
    }

    return result;
  }


  private static String getOperatingSystem() {
    String osString = null;
    try {
      osString = System.getProperty("os.name");
    } catch ( SecurityException ex ) {
      // Some system properties may not be available if the user has their security settings locked down
      System.err.println("Caught a SecurityException reading the system property 'os.name', defaulting to blank string.");
    }
    if (osString == null) {
      return null;
    } else if ( osString.startsWith( "Windows" ) ) {
      return "WINDOWS";
    } else if ( osString.startsWith( "Mac OS X" ) ) {
      return "OSX";
    } else {
      return null;
    }
  }


  private static String urlWithoutQuery(URL url) {
    if (url == null) {
      return "";
    } else {
      return url.getProtocol()
        + "://"
        + url.getHost()
        + url.getPath();
    }
  }


  private static void logRequestHeaders(HttpURLConnection connection) {
    System.out.println("-----------------------------------------------------------------");
    System.out.println("Request headers for " + connection.getURL().toExternalForm());
    for (Map.Entry<String, List<String>> entry : connection.getRequestProperties().entrySet()) {
      System.out.println("  " + entry.getKey() + "=" + entry.getValue());
    }
  }


  private static void logResponseHeaders(HttpURLConnection connection) {
    System.out.println("-----------------------------------------------------------------");
    System.out.println("Response headers for " + connection.getURL().toExternalForm());
    for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
      System.out.println(" " + entry.getKey() + "=" + entry.getValue());
    }
    System.out.println("-----------------------------------------------------------------");
  }


}
