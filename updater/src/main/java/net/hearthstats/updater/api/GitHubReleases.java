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
 * <p>Use this class to find out what the latest release of the HearthStats Companion is on GitHub.</p>
 */
public class GitHubReleases {

  private static List<Release> getReleases() {

    List<JsonWrapper> unparsedReleases = GitHubRequest.connect(UpdaterConfiguration.getReleasesApiUrl()).readJsonArray();

    List<Release> releases = new ArrayList<>();
    for (JsonWrapper unparsedRelease : unparsedReleases) {
      releases.add(Release.of(unparsedRelease));
    }

    return releases;
  }


  /**
   * Gets the latest release of the HearthStats Companion published on GitHub.
   * May include draft (unpublished) releases if you're logged in to your GitHub account.
   * @return The latest release, if found
   */
  public static Release getLatestRelease() {
    List<Release> releases = getReleases();
    boolean includeDraftReleases = UpdaterConfiguration.getIncludeDraftReleases();

    // Releases are listed in order from newest to oldest, so the first one is the latest.
    for (Release release : releases) {
      if (!release.isDraft() || includeDraftReleases) {
        return release;
      }
    }

    throw new UpdaterException("GitHub did not return information about the latest release");
  }


  /**
   * Gets the latest release of the HearthStats Companion published on GitHub
   * that contains a Mac OS X asset. Ignore newer releases if they're Windows-only.
   * May include draft (unpublished) releases if you're logged in to your GitHub account.
   * @return The latest release, if found
   */
  public static Release getLatestReleaseForOSX() {
    List<Release> releases = getReleases();
    boolean includeDraftReleases = UpdaterConfiguration.getIncludeDraftReleases();

    // Releases are listed in order from newest to oldest, so the first one is the latest.
    for (Release release : releases) {
      if (release.getOsxAsset() != null && (!release.isDraft() || includeDraftReleases)) {
        return release;
      }
    }

    throw new UpdaterException("GitHub did not return information about the latest release");
  }


  /**
   * Gets the latest release of the HearthStats Companion published on GitHub
   * that contains a Windows asset. Ignore newer releases if they're OS X-only.
   * May include draft (unpublished) releases if you're logged in to your GitHub account.
   * @return The latest release, if found
   */
  public static Release getLatestReleaseForWindows() {
    List<Release> releases = getReleases();
    boolean includeDraftReleases = UpdaterConfiguration.getIncludeDraftReleases();

    // Releases are listed in order from newest to oldest, so the first one is the latest.
    for (Release release : releases) {
      if (release.getWindowsAsset() != null && (!release.isDraft() || includeDraftReleases)) {
        return release;
      }
    }

    throw new UpdaterException("GitHub did not return information about the latest release");
  }


}
