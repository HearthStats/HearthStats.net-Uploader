package net.hearthstats;

import org.json.simple.JSONObject;

public class ArenaRun {
	
	public String userclass;
	
	public ArenaRun() {
	}
	
	public ArenaRun(JSONObject jsonObj) {
		userclass = (String) jsonObj.get("userclass");
	}
	

}
