package net.hearthstats.updater.api.model;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>Represents the key information about a Release on GitHub.</p>
 *
 * <p>See <a href="https://developer.github.com/v3/repos/releases/">https://developer.github.com/v3/repos/releases/</a>
 * for details.</p>
 */
public class Release {

  private final long id;
  private final String version;
  private final String name;
  private final String htmlUrl;
  private final String body;
  private final boolean draft;
  private final List<ReleaseAsset> assets;


  private Release(long id, String version, String name, String htmlUrl, String body, boolean draft, List<ReleaseAsset> assets) {
    this.id = id;
    this.version = version;
    this.name = name;
    this.htmlUrl = htmlUrl;
    this.body = body;
    this.draft = draft;
    this.assets = assets;
  }


  public String getVersion() {
    return version;
  }

  public String getName() {
    return name;
  }

  public long getId() {
    return id;
  }

  public String getHtmlUrl() {
    return htmlUrl;
  }

  public String getBody() {
    return body;
  }

  public boolean isDraft() {
    return draft;
  }


  public List<ReleaseAsset> getAssets() {
    return assets;
  }

  public ReleaseAsset getOsxAsset() {
    for (ReleaseAsset asset : assets) {
      if ("Mac OS X (zip)".equals(asset.getLabel())) {
        return asset;
      }
    }
    return null;
  }

  public ReleaseAsset getWindowsAsset() {
    for (ReleaseAsset asset : assets) {
      if ("Windows (zip)".equals(asset.getLabel())) {
        return asset;
      }
    }
    return null;
  }


  @Override
  public String toString() {
    return "Release{" +
      "id=" + id +
      ", version='" + version + '\'' +
      ", name='" + name + '\'' +
      ", htmlUrl='" + htmlUrl + '\'' +
      ", body='" + body + '\'' +
      ", draft=" + draft +
      ", assets=" + assets +
      '}';
  }


  public static Release of(JsonWrapper json) {
    List<ReleaseAsset> releaseAssets = new ArrayList<>();
    for (JsonWrapper jsonRelease : json.getObjectArray("assets")) {
      releaseAssets.add(ReleaseAsset.of(jsonRelease));
    }

    return new Release(
      json.getNumber("id").longValue(),
      json.getString("tag_name"),
      json.getString("name"),
      json.getString("html_url"),
      json.getString("body"),
      json.getBoolean("draft").booleanValue(),
      releaseAssets
    );
  }

}
