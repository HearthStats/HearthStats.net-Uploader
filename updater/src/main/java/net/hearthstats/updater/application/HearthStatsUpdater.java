package net.hearthstats.updater.application;

import net.hearthstats.updater.UpdaterConfiguration;
import net.hearthstats.updater.exception.UpdaterException;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


class HearthStatsUpdater {

  private static final Set<String> FILES_TO_SKIP = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
    "instructions-osx.txt"
  )));


  private final ProgressWindow window;

  private final String version;
  private final long assetId;
  private final String hearthstatsLocation;
  private final String downloadFile;

  private HearthStatsUpdater(String version, long assetId, String hearthstatsLocation, String downloadFile) {
    window = new ProgressWindow();
    this.version = version;
    this.assetId = assetId;
    this.hearthstatsLocation = hearthstatsLocation;
    this.downloadFile = downloadFile;

    window.open();
  }


  private void download() {
    window.log("Downloading version " + version + ". Please be patient ...");
    String assetUrlString = UpdaterConfiguration.getAssetApiUrl(assetId);

    try {
      URL assetUrl = new URL(assetUrlString);

      HttpURLConnection connection = (HttpURLConnection) assetUrl.openConnection();
      connection.addRequestProperty("User-Agent", UpdaterConfiguration.getClientUserAgent());
      connection.addRequestProperty("Accept", UpdaterConfiguration.getClientAccept());
      connection.addRequestProperty("Media-Type", "application/octet-stream");

      // During development you can set an OAuth token to test with draft releases.
      if (!UpdaterConfiguration.getGitHubOAuthToken().isEmpty()) {
        connection.addRequestProperty("Authorization", "token " + UpdaterConfiguration.getGitHubOAuthToken());
      }

      connection.setInstanceFollowRedirects(true);

      connection.connect();

      try (ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
           FileOutputStream fos = new FileOutputStream(downloadFile)) {
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
      }

    } catch (IOException e) {
      String error = "Unable to open connection to URL " + assetUrlString + " due to exception " + e.getMessage();
      window.log(error);
      throw new UpdaterException(error, e);
    }

  }

  private void extractZip() {
    window.log("v" + version + " downloaded, now extracting...");

    File updateZip = new File(downloadFile);
    if (updateZip.isFile()) {
      unZipIt(updateZip.getPath(), hearthstatsLocation);
      window.log("Done");
    } else {
      window.log("Updater Error: unable to locate " + updateZip.getPath());
    }

  }

  private void unZipIt(String zipFile, String outputFolder) {

    byte[] buffer = new byte[1024];

    try {

      // create output directory is not exists
      File folder = new File(outputFolder);
      if (!folder.exists()) {
        folder.mkdir();
      }

      // get the zip file content
      ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
      // get the zipped file list entry
      ZipEntry ze = zis.getNextEntry();

      while (ze != null) {

        String fileName = ze.getName();
        if (!FILES_TO_SKIP.contains(fileName)) {
          File newFile = new File(outputFolder + File.separator + fileName);

          System.out.println("file unzip : " + newFile.getAbsoluteFile());
          window.log("file unzip : " + newFile.getAbsoluteFile());

          // create all non exists folders
          // else you will hit FileNotFoundException for compressed folder
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
      String error = "Unable to uncompres file " + zipFile + " due to exception " + ex.getMessage();
      window.log(error);
      throw new UpdaterException(error, ex);
    }
  }


  public void runMain() {
    window.log("Update complete. Attempting to restart...");
    try {
      switch(getOperatingSystem()) {
        case "WINDOWS":
          Runtime.getRuntime().exec("HearthStats.exe");
          break;
        case "OSX":
          Desktop.getDesktop().open(new File(hearthstatsLocation + "/HearthStats.app"));
          break;
      }
      window.close();
    } catch (Exception e) {
      window.log("Error: " + e.getMessage());
      e.printStackTrace();
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

    updater.extractZip();

    updater.runMain();

    System.exit(0);
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


}
