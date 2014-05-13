package net.hearthstats;

import net.hearthstats.util.Rank;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

public class HearthstoneMatch {

	private String _mode;
	private String _userClass;
	private String _opponentClass;
	private boolean _coin = false;
	private String _result;
	private int _deckSlot;
	private String _opponentName;
	private Rank rankLevel;
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

	public Rank getRankLevel() {
		return rankLevel;
	}
	public String getResult() {
		return _result;
	}

	public void setRankLevel(Rank rankLevel) {
		this.rankLevel = rankLevel;
	}
	
	public void setResult(String result) {
		_result = result;
	}
	
	private String _propertyOrUnknown(String propertyVal) {
		return propertyVal == null ? "[undetected]" : propertyVal;
	}
	public String toString() {
		JSONObject deck = DeckUtils.getDeckFromSlot(getDeckSlot());
		
		return _propertyOrUnknown(getMode()) + 
				("Ranked".equals(getMode()) ? " level " + getRankLevel() : "") +
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
        if ("Ranked".equals(getMode())) {
            if (getRankLevel() == Rank.LEGEND) {
                obj.put("ranklvl", 26);     // 26 currently represents legend on HearthStats
                obj.put("legend", "true");
            } else {
                obj.put("ranklvl", getRankLevel().number);
                obj.put("legend", "false");
            }
        }

        obj.put("slot", getDeckSlot());
		obj.put("class", getUserClass());
		obj.put("oppclass", getOpponentClass());
		obj.put("oppname", getOpponentName());
		obj.put("coin", hasCoin() ? "true" : "false");
        obj.put("result", result);
		obj.put("notes", getNotes());
		obj.put("numturns", getNumTurns());
		obj.put("duration", getDuration());
		
		return obj;
	}

    /**
     * Determines if the data for this match is complete.
     *
     * @return true if there is enough data to submit the match, false if some data is missing
     */
    public boolean isDataComplete() {
        if (getMode() == null) {
            return false;
        } else if (getResult() == null) {
            return false;
        } else if (getUserClass() == null) {
            return false;
        } else if (getOpponentClass() == null) {
            return false;
        } else if (StringUtils.isBlank(getOpponentName())) {
            return false;
        }

        if (getMode().equals("Ranked")) {
            if (getRankLevel() == null) {
                return false;
            } else if (getDeckSlot() < 1 || getDeckSlot() > 9) {
                return false;
            }
        } else if (getMode().equals("Casual")) {
            if (getDeckSlot() < 1 || getDeckSlot() > 9) {
                return false;
            }
        }

        return true;
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
