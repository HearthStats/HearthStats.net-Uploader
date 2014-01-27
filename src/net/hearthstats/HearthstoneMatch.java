package net.hearthstats;

import org.json.simple.JSONObject;

public class HearthstoneMatch {

	private String _mode;
	private String _userClass;
	private String _opponentClass;
	private boolean _coin = false;
	private String _result;
	private int _deckSlot;
	
	public HearthstoneMatch() {
		
	}

	public int getDeckSlot() {
		return _deckSlot;
	}
	
	public String getMode() {
		return _mode;
	}

	public void setMode(String _mode) {
		this._mode = _mode;
	}

	public String getUserClass() {
		return _userClass;
	}

	public void setUserClass(String _userClass) {
		this._userClass = _userClass;
	}

	public String getOpponentClass() {
		return _opponentClass;
	}

	public void setOpponentClass(String _opponentClass) {
		this._opponentClass = _opponentClass;
	}

	public boolean hasCoin() {
		return _coin;
	}

	public void setCoin(boolean _coin) {
		this._coin = _coin;
	}
	
	public void setDeckSlot(int deckSlot) {
		_deckSlot = deckSlot;
	}

	public String getResult() {
		return _result;
	}

	public void setResult(String _result) {
		this._result = _result;
	}
	
	public String toString() {
		return (getMode() == null ? "[undetected]" : getMode()) + " " +
				(hasCoin() ? "" : "no ") + "coin " + 
				(getUserClass() == null ? "[undetected]" : getUserClass()) + " vs. " +
				(getOpponentClass() == null ? "[undetected]" : getOpponentClass()) + " " +
				getResult();
	}

	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		obj.put("mode", getUserClass());
		obj.put("userclass", getUserClass());
		obj.put("oppclass", getOpponentClass());
		obj.put("win", getResult() == "Victory" ? "true" : "false");
		obj.put("gofirst", hasCoin() ? "false" : "true");
		obj.put("slot", getDeckSlot());
		obj.put("rank", getMode());
		
		return obj;
	}

}
