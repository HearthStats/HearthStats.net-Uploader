package net.hearthstats;

import lombok.Data;
import net.hearthstats.util.Rank;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

@Data
public class HearthstoneMatch {

	private String mode;
	private String userClass;
	private String opponentClass;
	private boolean coin;
	private String result;
	private int deckSlot;
	private String opponentName;
	private Rank rankLevel;
	private int numTurns;
	private int duration;
	private String notes;
	private Integer id;
	
	public boolean hasCoin() {
		return isCoin();
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

	
	public String getEditUrl() {
		return "http://hearthstats.net/constructeds/" + getId() + "/edit";		
	}


}
