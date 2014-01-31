package net.hearthstats;

import org.json.simple.JSONObject;

public class HearthstoneMatch {

	private String _mode;
	private String _userClass;
	private String _opponentClass;
	private boolean _coin = false;
	private String _result;
	private int _deckSlot;
	private String _opponentName;
	
	public HearthstoneMatch() {
		
	}

	public int getDeckSlot() {
		return _deckSlot;
	}
	
	public String getMode() {
		return _mode;
	}

	public void setMode(String mode) {
		_mode = mode;
	}

	public String getUserClass() {
		return _userClass;
	}

	public void setUserClass(String userClass) {
		_userClass = userClass;
	}

	public String getOpponentClass() {
		return _opponentClass;
	}

	public void setOpponentClass(String opponentClass) {
		_opponentClass = opponentClass;
	}
	
	public void setOpponentName(String opponentName) {
		_opponentName = opponentName;
	}
	
	public String getOpponentName() {
		return _opponentName;
	}

	public boolean hasCoin() {
		return _coin;
	}

	public void setCoin(boolean coin) {
		_coin = coin;
	}
	
	public void setDeckSlot(int deckSlot) {
		_deckSlot = deckSlot;
	}

	public String getResult() {
		return _result;
	}

	public void setResult(String result) {
		_result = result;
	}
	
	private String _propertyOrUnknown(String propertyVal) {
		return propertyVal == null ? "[undetected]" : propertyVal;
	}
	public String toString() {
		return _propertyOrUnknown(getMode()) + " " +
				(hasCoin() ? "" : "no ") + "coin " + 
				_propertyOrUnknown(getUserClass()) + " vs. " +
				_propertyOrUnknown(getOpponentClass()) + " " +
				"(" + _propertyOrUnknown(getOpponentName()) + ") " +
				getResult() + " " +
				" deck slot " + getDeckSlot();
	}

	public JSONObject toJsonObject() {
		JSONObject obj = new JSONObject();
		obj.put("mode", getUserClass());
		obj.put("userclass", getUserClass());
		obj.put("oppclass", getOpponentClass());
		obj.put("oppname", getOpponentName());
		obj.put("win", getResult() == "Victory" ? "true" : "false");
		obj.put("gofirst", hasCoin() ? "false" : "true");
		obj.put("slot", getDeckSlot());
		obj.put("rank", getMode());
		
		return obj;
	}

}
