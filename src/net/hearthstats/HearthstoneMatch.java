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
	private String _rankLeve;
	private int _numTurns = 0;
	private int _duration;
	private String _notes;
	private Integer _id;
	
	public HearthstoneMatch() {
		
	}
	public int getDeckSlot() {
		return _deckSlot;
	}
	
	public int getDuration() {
		return _duration;
	}
	
	public String getMode() {
		return _mode;
	}
	
	public int getNumTurns() {
		return _numTurns;
	}

	public void setMode(String mode) {
		_mode = mode;
	}
	
	public void setNumTurns(int numTurns) {
		_numTurns = numTurns;
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
	
	public void setDuration(int duration) {
		_duration = duration;
	}

	public String getRankLevel() {
		return _rankLeve;
	}
	public String getResult() {
		return _result;
	}

	public void setRankLevel(String rankLevel) {
		_rankLeve = rankLevel;
	}
	
	public void setResult(String result) {
		_result = result;
	}
	
	private String _propertyOrUnknown(String propertyVal) {
		return propertyVal == null ? "[undetected]" : propertyVal;
	}
	public String toString() {
		JSONObject deck = DeckSlotUtils.getDeckFromSlot(getDeckSlot());
		
		return _propertyOrUnknown(getMode()) + 
				(getMode() == "Ranked" ? " level " + getRankLevel() : "") +
				" " +
				(hasCoin() ? "" : "no ") + "coin " + 
				_propertyOrUnknown(getUserClass()) + " vs. " +
				_propertyOrUnknown(getOpponentClass()) + " " +
				"(" + _propertyOrUnknown(getOpponentName()) + ") " +
				getResult() + " " +
				(deck == null ? "" : " deck: " + deck.get("name")) +
				" (" + getNumTurns() + " turns)";
				
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJsonObject() {
		String result = null;
		switch(getResult()) {
			case "Victory": case "Win":
				result = "Win";
				break;
			case "Defeat": case "Loss":
				result = "Loss";
				break;
			case "Draw":
				result = "Draw";
				break;
		}
		JSONObject obj = new JSONObject();
		obj.put("mode", getMode());
		obj.put("class", getUserClass());
		obj.put("oppclass", getOpponentClass());
		obj.put("oppname", getOpponentName());
		obj.put("result", result);
		obj.put("coin", hasCoin() ? "true" : "false");
		obj.put("slot", getDeckSlot());
		obj.put("notes", getNotes());
		obj.put("ranklvl", getRankLevel());
		obj.put("numturns", getNumTurns());
		obj.put("duration", getDuration());
		
		return obj;
	}

	public String getNotes() {
		return _notes;
	}
	public void setNotes(String text) {
		_notes = text;
	}
	
	public String getEditUrl() {
		return "http://hearthstats.net/constructeds/" + getId() + "/edit";		
	}

	public Integer getId() {
		return _id;
	}
	public void setId(int id) {
		_id  = id;
	}

}
