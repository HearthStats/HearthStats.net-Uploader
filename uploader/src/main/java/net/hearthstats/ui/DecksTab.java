package net.hearthstats.ui;

import static net.hearthstats.util.Translations.t;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.hearthstats.API;
import net.hearthstats.Constants;
import net.hearthstats.DeckUtils;
import net.hearthstats.Main;
import net.miginfocom.swing.MigLayout;

import org.json.simple.JSONObject;

public class DecksTab extends JPanel {
	private JComboBox<String> _deckSlot1Field;
	private JComboBox<String> _deckSlot2Field;
	private JComboBox<String> _deckSlot3Field;
	private JComboBox<String> _deckSlot4Field;
	private JComboBox<String> _deckSlot5Field;
	private JComboBox<String> _deckSlot6Field;
	private JComboBox<String> _deckSlot7Field;
	private JComboBox<String> _deckSlot8Field;
	private JComboBox<String> _deckSlot9Field;
	protected API _api = new API();
	private static final String DECKS_URL = "http://hearthstats.net/decks";

	public DecksTab() {

		MigLayout layout = new MigLayout();
		setLayout(layout);

		add(new JLabel(" "), "wrap");
		add(new JLabel(t("set_your_deck_slots")), "skip,span,wrap");
		add(new JLabel(" "), "wrap");

		add(new JLabel(t("deck_slot.label_1")), "skip");
		add(new JLabel(t("deck_slot.label_2")), "");
		add(new JLabel(t("deck_slot.label_3")), "wrap");

		_deckSlot1Field = new JComboBox<>();
		add(_deckSlot1Field, "skip");
		_deckSlot2Field = new JComboBox<>();
		add(_deckSlot2Field, "");
		_deckSlot3Field = new JComboBox<>();
		add(_deckSlot3Field, "wrap");

		add(new JLabel(" "), "wrap");

		add(new JLabel(t("deck_slot.label_4")), "skip");
		add(new JLabel(t("deck_slot.label_5")), "");
		add(new JLabel(t("deck_slot.label_6")), "wrap");

		_deckSlot4Field = new JComboBox<>();
		add(_deckSlot4Field, "skip");
		_deckSlot5Field = new JComboBox<>();
		add(_deckSlot5Field, "");
		_deckSlot6Field = new JComboBox<>();
		add(_deckSlot6Field, "wrap");

		add(new JLabel(" "), "wrap");

		add(new JLabel(t("deck_slot.label_7")), "skip");
		add(new JLabel(t("deck_slot.label_8")), "");
		add(new JLabel(t("deck_slot.label_9")), "wrap");

		_deckSlot7Field = new JComboBox<>();
		add(_deckSlot7Field, "skip");
		_deckSlot8Field = new JComboBox<>();
		add(_deckSlot8Field, "");
		_deckSlot9Field = new JComboBox<>();
		add(_deckSlot9Field, "wrap");

		add(new JLabel(" "), "wrap");
		add(new JLabel(" "), "wrap");

		JButton saveButton = new JButton(t("button.save_deck_slots"));
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_saveDeckSlots();
			}
		});
		add(saveButton, "skip");

		JButton refreshButton = new JButton(t("button.refresh"));
		refreshButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					updateDecks();
				} catch (IOException e1) {
					Main.showErrorDialog("Error updating decks", e1);
				}
			}
		});
		add(refreshButton, "wrap,span");

		add(new JLabel(" "), "wrap");
		add(new JLabel(" "), "wrap");

		JButton myDecksButton = new JButton(t("manage_decks_on_hsnet"));
		myDecksButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(DECKS_URL));
				} catch (Throwable e1) {
					Main.showErrorDialog("Error launching browser with URL"
							+ DECKS_URL, e1);
				}
			}
		});
		add(myDecksButton, "skip,span");

	}

	public void updateDecks() throws IOException {
		DeckUtils.updateDecks();
		_applyDecksToSelector(_deckSlot1Field, 1);
		_applyDecksToSelector(_deckSlot2Field, 2);
		_applyDecksToSelector(_deckSlot3Field, 3);
		_applyDecksToSelector(_deckSlot4Field, 4);
		_applyDecksToSelector(_deckSlot5Field, 5);
		_applyDecksToSelector(_deckSlot6Field, 6);
		_applyDecksToSelector(_deckSlot7Field, 7);
		_applyDecksToSelector(_deckSlot8Field, 8);
		_applyDecksToSelector(_deckSlot9Field, 9);
	}

	private void _applyDecksToSelector(JComboBox<String> selector,
			Integer slotNum) {

		selector.setMaximumSize(new Dimension(200, selector.getSize().height));
		selector.removeAllItems();

		selector.addItem("- Select a deck -");

		List<JSONObject> decks = DeckUtils.getDecks();

		Collections.sort(decks, new Comparator<JSONObject>() {
			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				return name(o1).compareTo(name(o2));
			}

		});

		for (int i = 0; i < decks.size(); i++) {
			selector.addItem(name(decks.get(i))
					+ "                                       #"
					+ decks.get(i).get("id"));
			if (decks.get(i).get("slot") != null
					&& decks.get(i).get("slot").toString()
							.equals(slotNum.toString()))
				selector.setSelectedIndex(i + 1);
		}
	}

	private String name(JSONObject o) {
		return Constants.hsClassOptions[Integer.parseInt(o.get("klass_id")
				.toString())]
				+ " - " + o.get("name").toString().toLowerCase();
	}

	private void _saveDeckSlots() {

		try {
			_api.setDeckSlots(_getDeckSlotDeckId(_deckSlot1Field),
					_getDeckSlotDeckId(_deckSlot2Field),
					_getDeckSlotDeckId(_deckSlot3Field),
					_getDeckSlotDeckId(_deckSlot4Field),
					_getDeckSlotDeckId(_deckSlot5Field),
					_getDeckSlotDeckId(_deckSlot6Field),
					_getDeckSlotDeckId(_deckSlot7Field),
					_getDeckSlotDeckId(_deckSlot8Field),
					_getDeckSlotDeckId(_deckSlot9Field));
			Main.showMessageDialog(this, _api.getMessage());
			updateDecks();
		} catch (Throwable e) {
			Main.showErrorDialog("Error saving deck slots", e);
		}
	}

	private Integer _getDeckSlotDeckId(JComboBox selector) {
		Integer deckId = null;
		String deckStr = (String) selector.getItemAt(selector
				.getSelectedIndex());
		Pattern pattern = Pattern.compile("[^0-9]+([0-9]+)$");
		Matcher matcher = pattern.matcher(deckStr);
		if (matcher.find()) {
			deckId = Integer.parseInt(matcher.group(1));
		}
		return deckId;
	}
}
