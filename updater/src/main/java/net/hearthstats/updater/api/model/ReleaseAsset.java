package net.hearthstats.updater.api.model;

/**
 * <p>Represents the key information about an assets of a Release on GitHub.</p>
 *
 * <p>See <a href="https://developer.github.com/v3/repos/releases/">https://developer.github.com/v3/repos/releases/</a>
 * for details.</p>
 */
public class ReleaseAsset {
  private final long id;
  private final String name;
  private final String label;
  private final String url;
  private final long size;


  public ReleaseAsset(long id, String name, String label, String url, long size) {
    this.id = id;
    this.name = name;
    this.label = label;
    this.url = url;
    this.size = size;
  }


  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getLabel() {
    return label;
  }

  public String getUrl() {
    return url;
  }

  public long getSize() {
    return size;
  }


  @Override
  public String toString() {
    return "ReleaseAsset {" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", label='" + label + '\'' +
      ", url='" + url + '\'' +
      ", size=" + size +
      '}';
  }


  public static ReleaseAsset of(JsonWrapper json) {
    return new ReleaseAsset(
      json.getNumber("id").longValue(),
      json.getString("name"),
      json.getString("label"),
      json.getString("url"),
      json.getNumber("size").longValue()
    );
  }

}
