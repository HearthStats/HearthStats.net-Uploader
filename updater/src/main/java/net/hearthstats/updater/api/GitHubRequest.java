package net.hearthstats.updater.api;

import net.hearthstats.updater.UpdaterConfiguration;
import net.hearthstats.updater.exception.UpdaterException;
import net.hearthstats.updater.api.model.JsonWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;


/**
 * Makes requests to the GitHub API v3.
 */
class GitHubRequest {

  private final URL apiUrl;
  private final URLConnection connection;

  private GitHubRequest(URL apiUrl, URLConnection connection) {
    this.apiUrl = apiUrl;
    this.connection = connection;
  }


  static GitHubRequest connect(String url) {
    try {
      URL apiUrl = new URL(url);
      URLConnection connection = apiUrl.openConnection();
      connection.addRequestProperty("User-Agent", UpdaterConfiguration.getClientUserAgent());
      connection.addRequestProperty("Accept", UpdaterConfiguration.getClientAccept());

      // During development you can set an OAuth token to test with draft releases.
      if (!UpdaterConfiguration.getGitHubOAuthToken().isEmpty()) {
        connection.addRequestProperty("Authorization", "token " + UpdaterConfiguration.getGitHubOAuthToken());
      }

      connection.connect();

      return new GitHubRequest(apiUrl, connection);
    } catch (IOException e) {
      throw new UpdaterException("Unable to open connection to URL " + url + " due to exception " + e.getMessage(), e);
    }
  }



  Object read() {
    try (Reader inputReader = new InputStreamReader(connection.getInputStream(), "UTF-8")) {
      JSONParser parser = new JSONParser();
      return parser.parse(inputReader);
    } catch (ParseException e) {
      throw new UpdaterException("Unable to parse JSON due to exception " + e.getMessage(), e);
    } catch (UnsupportedEncodingException e) {
      throw new UpdaterException("Unable to parse JSON because UTF-8 encoding is unsupported, this should not be possible!", e);
    } catch (IOException e) {
      throw new UpdaterException("Unable to read data from URL " + apiUrl.toString() + " due to exception " + e.getMessage(), e);
    }
  }


  List<JsonWrapper> readJsonArray() {
    Object output = read();
    if (output instanceof JSONArray) {
      return JsonWrapper.of((JSONArray) output);
    } else {
      throw new UpdaterException("Unable to read JSON array because the response is " + output.getClass().getSimpleName());
    }
  }


  JsonWrapper readJsonObject() {
    Object output = read();
    if (output instanceof JSONObject) {
      return JsonWrapper.of((JSONObject) output);
    } else {
      throw new UpdaterException("Unable to read JSON object because the response is " + output.getClass().getSimpleName());
    }
  }


}
