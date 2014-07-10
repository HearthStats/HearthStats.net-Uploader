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

}
