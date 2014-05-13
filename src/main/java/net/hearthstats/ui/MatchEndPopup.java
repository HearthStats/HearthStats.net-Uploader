package net.hearthstats.ui;

import net.hearthstats.DeckUtils;
import net.hearthstats.HearthstoneMatch;
import net.hearthstats.Monitor;
import net.hearthstats.util.Rank;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * A popup to display at the end of the match that allows the match details to be edited.
 *
 * @author gtch
 */
public class MatchEndPopup extends JPanel {

    private final static Logger debugLog = LoggerFactory.getLogger(MatchEndPopup.class);

    private final ResourceBundle bundle = ResourceBundle.getBundle("net.hearthstats.resources.Main");

    private final MigLayout layout = new MigLayout(
            "",
            "[]10[grow]20[]10[grow]",
            ""
    );

    private final HearthstoneMatch match;

    private String infoMessage;
    private List<String> errorMessages;

    private JComboBox rankComboBox;
    private JComboBox gameModeComboBox;
    private JTextField opponentNameField;
    private JComboBox opponentClassComboBox;
    private JComboBox yourClassComboBox;
    private JComboBox yourDeckComboBox;
    private JCheckBox coinCheckBox;
    private JTextArea notesTextArea;
    private JRadioButton resultVictory;
    private JRadioButton resultDefeat;
    private JRadioButton resultDraw;
    private JPanel rankPanel=new JPanel();
    private JPanel deckPanel=new JPanel();
	private JLabel rankNotApplicable;
	private JLabel deckNotApplicable;


    private MatchEndPopup(HearthstoneMatch match, String infoMessage) {
        this.match = match;
        this.infoMessage = infoMessage;
        this.errorMessages = determineErrors(match);

        initComponents();
    }


    public static enum Button {
        SUBMIT, CANCEL
    }



    public static Button showPopup(Component parentComponent, HearthstoneMatch match, String infoMessage, String title) {
        MatchEndPopup popup = new MatchEndPopup(match, infoMessage);

        int value = JOptionPane.showOptionDialog(parentComponent, popup, title,
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.YES_NO_OPTION, null,
                new String[] { "Submit", "Cancel" }, "Submit");

        Button result;
        switch (value) {
            case 0:
                result = Button.SUBMIT;
                break;
            case 1:
                result = Button.CANCEL;
                break;
            default:
                result = Button.CANCEL;
                break;
        }

        return result;
    }



    private List<String> determineErrors(HearthstoneMatch match) {
        List<String> result = new ArrayList<>();

        if (match.getMode() == null) {
            result.add(t("match.popup.error.mode"));
        }
        if (match.getRankLevel() == null && "Ranked".equals(match.getMode())) {
            result.add(t("match.popup.error.rank"));
        }
        if (match.getUserClass() == null) {
            result.add(t("match.popup.error.yourclass"));
        }
        if (StringUtils.isBlank(match.getOpponentName())) {
            result.add(t("match.popup.error.opponentname"));
        }
        if (match.getOpponentClass() == null) {
            result.add(t("match.popup.error.opponentclass"));
        }
        if (match.getResult() == null) {
            result.add(t("match.popup.error.result"));
        }

        return result;
    }


    private void initComponents() {

        // Increase the site of the panel if there are error messages to display
        int preferredHeight = 380 + (30 * errorMessages.size());

        setLayout(layout);
        setMinimumSize(new Dimension(660, 380));
        setPreferredSize(new Dimension(660, preferredHeight));
        setMaximumSize(new Dimension(660, preferredHeight + 200));


        //// Row 1 ////

        JLabel heading = new JLabel(match.getMode() == null ? t("match.popup.heading") : match.getMode() + " " + t("match.popup.heading"));
        Font headingFont = heading.getFont().deriveFont(20f);
        heading.setFont(headingFont);
        add(heading, "span");


        //// Row 2 ////

        if (infoMessage != null) {
            JLabel infoLabel = new JLabel("<html>" + infoMessage + "</html>");
            add(infoLabel, "span, gapy 5px 10px");
        }


        //// Row 3 ////

        if (errorMessages.size() > 0) {

            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            for (String error : errorMessages) {
                if (sb.length() > 6) {
                    sb.append("<br>");
                }
                sb.append("- ");
                sb.append(error);
            }
            sb.append("</html>");

            JLabel errorLabel = new JLabel(sb.toString());
            errorLabel.setForeground(Color.RED.darker());
            add(errorLabel, "span, gapy 5px 10px");
        }

        //// Row 3 bis - game mode ////
        
        //TODO : localize and use constants
        String[] gameModes = new String[] {undetectedLabel(), "Arena" , "Casual", "Practice", "Ranked"};

        add(new JLabel(t("match.label.game_mode")), "right");

        gameModeComboBox = new JComboBox<>(gameModes);
        setDefaultSize(gameModeComboBox);
        if (match.getMode() != null) {
             gameModeComboBox.setSelectedItem(match.getMode());
        } else {
             gameModeComboBox.setSelectedIndex(0);
        }
        gameModeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	match.setMode(gameModeComboBox.getSelectedItem().toString());
            	updateGameMode();
            }
        });
        add(gameModeComboBox, "span");
        
        
        //// Row 4 ////

        add(new JLabel(t("match.label.your_rank")), "right");
        rankComboBox = new JComboBox<>(Rank.values());
        setDefaultSize(rankComboBox);
        rankComboBox.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		match.setRankLevel((Rank) rankComboBox.getSelectedItem());
        	}
        });

        rankNotApplicable = new JLabel("");
        rankNotApplicable.setFont(rankNotApplicable.getFont().deriveFont(Font.ITALIC));
        rankNotApplicable.setEnabled(false);
        setDefaultSize(rankNotApplicable);

        add(rankPanel, "");
        add(new JLabel(t("match.label.opponent_name")), "right");

        opponentNameField = new JTextField();
        setDefaultSize(opponentNameField);
        opponentNameField.setText(match.getOpponentName());
        opponentNameField.addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyReleased(KeyEvent e) {
        		match.setOpponentName(opponentNameField.getText().replaceAll("\\s+", ""));
            }
        });
        add(opponentNameField, "wrap");


        //// Row 5 ////

        String[] localizedClassOptions = new String[Monitor.hsClassOptions.length];
        localizedClassOptions[0] = undetectedLabel();
        for (int i = 1; i < localizedClassOptions.length; i++) {
            localizedClassOptions[i] = t(Monitor.hsClassOptions[i]);
        }

        add(new JLabel(t("match.label.your_class")), "right");

        yourClassComboBox = new JComboBox<>(localizedClassOptions);
        setDefaultSize(yourClassComboBox);
        if (match.getUserClass() == null) {
            yourClassComboBox.setSelectedIndex(0);
        } else {
            yourClassComboBox.setSelectedItem(match.getUserClass());
        }
        yourClassComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (yourClassComboBox.getSelectedIndex() == 0) {
                    match.setUserClass(null);
                } else {
                    match.setUserClass(Monitor.hsClassOptions[yourClassComboBox.getSelectedIndex()]);
                }
            }
        });
        add(yourClassComboBox, "");

        add(new JLabel(t("match.label.opponents_class")), "right");

        opponentClassComboBox = new JComboBox<>(localizedClassOptions);
        setDefaultSize(opponentClassComboBox);
        if (match.getOpponentClass() == null) {
            opponentClassComboBox.setSelectedIndex(0);
        } else {
            opponentClassComboBox.setSelectedItem(match.getOpponentClass());
        }
        opponentClassComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (opponentClassComboBox.getSelectedIndex() == 0) {
                    match.setOpponentClass(null);
                } else {
                    match.setOpponentClass(Monitor.hsClassOptions[opponentClassComboBox.getSelectedIndex()]);
                }
            }
        });
        add(opponentClassComboBox, "wrap");



        //// Row 6 ////

        add(new JLabel(t("match.label.your_deck")), "right");

        String[] deckSlotList = new String[10];
        deckSlotList[0] = undetectedLabel();
        for (int i = 1; i <= 9; i++) {
            JSONObject deck =  DeckUtils.getDeckFromSlot(i);
            StringBuilder sb = new StringBuilder();
            sb.append(t("deck_slot.label_" + i));
            sb.append(" ");
            if (deck == null) {
                sb.append(t("undetected"));
            } else {
                sb.append(deck.get("name"));
            }
            deckSlotList[i] = sb.toString();
        }

        yourDeckComboBox = new JComboBox<>(deckSlotList);
        setDefaultSize(yourDeckComboBox);
        yourDeckComboBox.setSelectedIndex(match.getDeckSlot());
        yourDeckComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                match.setDeckSlot(yourDeckComboBox.getSelectedIndex());
            }
        });
        
      
        deckNotApplicable = new JLabel("");
        deckNotApplicable.setFont(deckNotApplicable.getFont().deriveFont(Font.ITALIC));
        deckNotApplicable.setEnabled(false);
        setDefaultSize(deckNotApplicable);
        
        add(deckPanel, "wrap");

        //// Row 7 ////

        add(new JLabel(t("match.label.coin")), "right");

        coinCheckBox = new JCheckBox(t("match.coin"));
        coinCheckBox.setSelected(match.hasCoin());
        coinCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                match.setCoin(coinCheckBox.isSelected());
            }
        });
        add(coinCheckBox, "wrap");


        //// Row 8 ////

        add(new JLabel("Result:"), "right, gapy 20px 20px");

        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));

        resultVictory = new JRadioButton(t("match.label.result_victory"));
        resultVictory.setMnemonic(KeyEvent.VK_V);
        resultVictory.setMargin(new Insets(0, 0, 0, 10));
        if ("Victory".equals(match.getResult())) {
            resultVictory.setSelected(true);
        }
        resultVictory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (resultVictory.isSelected()) {
                    match.setResult("Victory");
                }
            }
        });
        resultPanel.add(resultVictory);

        resultDefeat = new JRadioButton(t("match.label.result_defeat"));
        resultDefeat.setMnemonic(KeyEvent.VK_D);
        resultDefeat.setMargin(new Insets(0, 0, 0, 10));
        if ("Defeat".equals(match.getResult())) {
            resultDefeat.setSelected(true);
        }
        resultDefeat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (resultDefeat.isSelected()) {
                    match.setResult("Defeat");
                }
            }
        });
        resultPanel.add(resultDefeat);

        resultDraw = new JRadioButton(t("match.label.result_draw"));
        resultDraw.setMnemonic(KeyEvent.VK_R);
        resultDraw.setMargin(new Insets(0, 0, 0, 10));
        if ("Draw".equals(match.getResult())) {
            resultDraw.setSelected(true);
        }
        resultDraw.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (resultDraw.isSelected()) {
                    match.setResult("Draw");
                }
            }
        });
        resultPanel.add(resultDraw);

        ButtonGroup resultGroup = new ButtonGroup();
        resultGroup.add(resultVictory);
        resultGroup.add(resultDefeat);
        resultGroup.add(resultDraw);

        add(resultPanel, "span 3, wrap");


        //// Row 9 ////

        add(new JLabel(t("match.label.notes")), "right");
        notesTextArea = new JTextArea();
        notesTextArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        notesTextArea.setMinimumSize(new Dimension(550, 100));
        notesTextArea.setPreferredSize(new Dimension(550, 150));
        notesTextArea.setMaximumSize(new Dimension(550, 200));
        notesTextArea.setBackground(Color.WHITE);
        notesTextArea.setText(match.getNotes());
        notesTextArea.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                match.setNotes(notesTextArea.getText());
            }
        });
        add(notesTextArea, "span 3, wrap");

        updateGameMode();
    }
    
	private void updateGameMode() {
		boolean isRanked = "Ranked".equals(match.getMode());
		rankPanel.removeAll();
		if (isRanked) {
			if (match.getRankLevel() != null) {
				rankComboBox.setSelectedIndex(25 - match.getRankLevel().number);
			}
			rankPanel.add(rankComboBox);
		} else {
			String rankMessage;
			if ("Arena".equals(match.getMode())) {
				rankMessage = "N/A: Arena Mode";
			} else if ("Casual".equals(match.getMode())) {
				rankMessage = "N/A: Casual Mode";
			} else {
				rankMessage = "N/A";
			}
			rankNotApplicable.setText(rankMessage);
			rankPanel.add(rankNotApplicable);
		}

		deckPanel.removeAll();
		if (isRanked || "Casual".equals(match.getMode())) { // TODO shouldn't we add also Practice here ?
			deckPanel.add(yourDeckComboBox);
		} else {
			String deckMessage;
			if ("Arena".equals(match.getMode())) {
				deckMessage = "N/A: Arena Mode";
			} else {
				deckMessage = "N/A";
			}
			deckNotApplicable.setText(deckMessage);
			deckPanel.add(deckNotApplicable);
		}

		validate();
		repaint();
	}

	private static void setDefaultSize(Component c) {
		c.setMinimumSize(new Dimension(180, 27));
		c.setPreferredSize(new Dimension(200, 28));
	}

    /**
     * Loads text from the main resource bundle, using the local language when available.
     * @param key the key for the desired string
     * @return The requested string
     */
    private String t(String key) {
        return bundle.getString(key);
    }

    private String undetectedLabel() {
    	return "- "+t("undetected")+" -";
    }
}
