package net.hearthstats.updater.api;

import net.hearthstats.updater.UpdaterConfiguration;
import net.hearthstats.updater.exception.UpdaterException;
import net.hearthstats.updater.api.model.JsonWrapper;
import net.hearthstats.updater.api.model.Release;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Provides access to the GitHub Releases API.</p>
 *
 * <p>Use this class to find out what the latest release of the HearthStats Uploader is on GitHub.</p>
 */
public class GitHubReleases {


//  public static void main(String[] args) {
//    System.out.println("Starting...");
//
//    System.out.println("URL: " + UpdaterConfiguration.getReleasesApiUrl());
//
////    Release latestRelease = getLatestRelease(true);
////    System.out.println("Latest release is: " + latestRelease.getName() + " (ID " + latestRelease.getId() + ")");
//
//
////    downloadSomething();
////
//    List<Release> releases = getReleases();
//
//    for (Release release : releases) {
//      System.out.println("* Release: " + release);
//    }
//
//  }
//


  private static List<Release> getReleases() {

    List<JsonWrapper> unparsedReleases = GitHubRequest.connect(UpdaterConfiguration.getReleasesApiUrl()).readJsonArray();

    List<Release> releases = new ArrayList<>();
    for (JsonWrapper unparsedRelease : unparsedReleases) {
      releases.add(Release.of(unparsedRelease));
    }

    return releases;
  }


  public static Release getLatestRelease(boolean includeDraftReleases) {
    List<Release> releases = getReleases();

    // Releases are listed in order from newest to oldest, so the first one is the latest.
    for (Release release : releases) {
      if (!release.isDraft() || includeDraftReleases) {
        return release;
      }
    }

    throw new UpdaterException("GitHub did not return information about the latest release");
  }



//  private static void downloadSomething() {
//    try {
//
////      URL url = new URL(UpdaterConfiguration.getReleasesApiUrl());
////      try (InputStream in = url.openStream()) {
////        Files.copy(in, Paths.get("output.txt"));
////      }
//
//      URL url = new URL(UpdaterConfiguration.getReleasesApiUrl());
//      URLConnection connection = url.openConnection();
//      connection.setAllowUserInteraction(true);
//
//      connection.addRequestProperty("User-Agent", UpdaterConfiguration.getClientUserAgent());
//      connection.addRequestProperty("Accept", UpdaterConfiguration.getClientAccept());
//
//      Map<String, List<String>> requestProperties = connection.getRequestProperties();
//      for (Map.Entry<String, List<String>> entry : requestProperties.entrySet()) {
//        System.out.println("* REQP " + entry.getKey() + " = " + entry.getValue());
//      }
//
//
//      connection.connect();
//      Map<String, List<String>> headerFields = connection.getHeaderFields();
//      for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
//        System.out.println("* HEAD " + entry.getKey() + " = " + entry.getValue());
//      }
//
//      try (Reader inputReader = new InputStreamReader(connection.getInputStream(), "UTF-8")) {
//        JSONParser parser = new JSONParser();
//        Object output = parser.parse(inputReader);
//
//        System.out.println("* Response = " + output.toString());
//
//
//      } catch (ParseException e) {
//        System.out.println("ParseException");
//        e.printStackTrace();
//      }
//
//
//    } catch (MalformedURLException e) {
//      e.printStackTrace();
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//
//
//  }

}
