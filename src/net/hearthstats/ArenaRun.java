package net.hearthstats;

import org.json.simple.JSONObject;

public class ArenaRun {
	
	private String _userclass;
	
	public ArenaRun() {
	}
	
	public ArenaRun(JSONObject jsonObj) {
		_userclass = (String) jsonObj.get("userclass");
	}
	
	public String getUserClass() {
		return _userclass;
	}
	public void setUserClass(String userclass) {
		_userclass = userclass;
	}
	
	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		obj.put("userclass", _userclass);
		return obj;
	}

}
