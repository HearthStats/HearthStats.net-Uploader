package net.hearthstats.updater.api.model;

import net.hearthstats.updater.exception.JsonException;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by charlie on 6/07/2014.
 */
public class JsonWrapper implements JSONAware {

  private final String context;
  private final JSONObject obj;

  private JsonWrapper(String context, JSONObject obj) {
    this.context = context;
    this.obj = obj;
  }


  public JsonWrapper getObject(String key) {
    Object value = obj.get(key);
    if (value == null) {
      return null;
    } else if (value instanceof JSONObject) {
      return new JsonWrapper(context + "." + key, (JSONObject) value);
    } else {
      throw new JsonException("Expected " + context + "." + key + " to be JSONObject but it is " + value.getClass().getSimpleName());
    }
  }

  public List<JsonWrapper> getObjectArray(String key) {
    List<JSONObject> inputList = getArray(key);
    List<JsonWrapper> resultList = new ArrayList<JsonWrapper>();

    if (inputList != null) {
      for (int i = 0; i < inputList.size(); i++) {
        try {
          resultList.add(new JsonWrapper(context + "." + key + "[" + i + "]", inputList.get(i)));
        } catch (ClassCastException e) {
          throw new JsonException("Expected " + context + "." + key + "[" + i + "] to be JSONObject but it is not");
        }
      }
    }

    return resultList;
  }


  public String getString(String key) {
    Object value = obj.get(key);
    if (value == null) {
      return null;
    } else if (value instanceof String) {
      return (String) value;
    } else {
      throw new JsonException("Expected " + context + "." + key + " to be String but it is " + value.getClass().getSimpleName());
    }
  }


  public Number getNumber(String key) {
    Object value = obj.get(key);
    if (value == null) {
      return null;
    } else if (value instanceof Number) {
      return (Number) value;
    } else {
      throw new JsonException("Expected " + context + "." + key + " to be Number but it is " + value.getClass().getSimpleName());
    }
  }


  public Boolean getBoolean(String key) {
    Object value = obj.get(key);
    if (value == null) {
      return null;
    } else if (value instanceof Boolean) {
      return (Boolean) value;
    } else {
      throw new JsonException("Expected " + context + "." + key + " to be Boolean but it is " + value.getClass().getSimpleName());
    }
  }



  @Override
  public String toJSONString() {
    return obj.toJSONString();
  }


  @SuppressWarnings("unchecked")
  private <T> List<T> getArray(String key) throws ClassCastException {
    Object value = obj.get(key);
    if (value == null) {
      return null;
    } else if (value instanceof JSONArray) {
      List<T> resultList = new ArrayList<T>();
      for (Object item : (List<?>) value) {
        resultList.add((T) item);
      }
      return resultList;
    } else {
      throw new JsonException("Expected " + context + "." + key + " to be JSONArray but it is " + value.getClass().getSimpleName());
    }
  }



  public static JsonWrapper of(JSONObject jsonObject) {
    return new JsonWrapper("", jsonObject);
  }


  public static List<JsonWrapper> of(JSONArray jsonArray) {
    List<JsonWrapper> result = new ArrayList<>();
    for (int i = 0; i < jsonArray.size(); i++) {
      result.add(new JsonWrapper("[" + i + "]", (JSONObject) jsonArray.get(i)));
    }
    return result;
  }

}
