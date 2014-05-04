package net.hearthstats.ui;

import net.hearthstats.HearthstoneMatch;
import net.hearthstats.Monitor;
import net.hearthstats.util.Rank;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
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
    private JTextField opponentNameField;
    private JComboBox opponentClassComboBox;
    private JComboBox yourClassComboBox;
    private JCheckBox coinCheckBox;
    private JTextArea notesTextArea;
    private JRadioButton resultVictory;
    private JRadioButton resultDefeat;
    private JRadioButton resultDraw;

    private JButton swingButton;


    public MatchEndPopup(HearthstoneMatch match, String infoMessage) {
        this.match = match;
        this.infoMessage = infoMessage;
        this.errorMessages = determineErrors(match);

        initComponents();
    }


    private List<String> determineErrors(HearthstoneMatch match) {
        List<String> result = new ArrayList<>();

        if (match.getRankLevel() == null && "Ranked".equals(match.getMode())) {
            result.add("Your rank not detected");
        }
        if (match.getUserClass() == null) {
            result.add("Your class not detected");
        }
        if (StringUtils.isBlank(match.getOpponentName())) {
            result.add("Opponent name not detected");
        }
        if (match.getOpponentClass() == null) {
            result.add("Opponent class not detected");
        }
        if (match.getResult() == null) {
            result.add("Result not detected");
        }

        return result;
    }


    private void initComponents(){

        // Increase the site of the panel if there are error messages to display
        int preferredHeight = 350 + (30 * errorMessages.size());

        setLayout(layout);
        setMinimumSize(new Dimension(600, 350));
        setPreferredSize(new Dimension(600, preferredHeight));
        setMaximumSize(new Dimension(600, preferredHeight + 200));

        //// Row 1 ////

        JLabel heading = new JLabel("Match Result");
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


        //// Row 4 ////

        add(new JLabel(t("match.label.your_rank")), "right");

        if ("Ranked".equals(match.getMode())) {
            rankComboBox = new JComboBox<>(Rank.values());
            rankComboBox.setSelectedIndex(25 - match.getRankLevel().number);
            rankComboBox.setMinimumSize(new Dimension(150, 27));
            rankComboBox.setPreferredSize(new Dimension(200, 28));
            add(rankComboBox, "");
        } else {
            String rankMessage;
            if ("Arena".equals(match.getMode())) {
                rankMessage = "N/A: Arena Mode";
            } else if ("Casual".equals(match.getMode())) {
                rankMessage = "N/A: Casual Mode";
            } else {
                rankMessage = "N/A";
            }
            JLabel rankNotApplicable = new JLabel(rankMessage);
            rankNotApplicable.setFont(rankNotApplicable.getFont().deriveFont(Font.ITALIC));
            rankNotApplicable.setEnabled(false);
            add(rankNotApplicable, "");

        }

        add(new JLabel(t("match.label.opponent_name")), "right");

        opponentNameField = new JTextField();
        opponentNameField.setMinimumSize(new Dimension(150, 27));
        opponentNameField.setPreferredSize(new Dimension(200, 28));
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
        localizedClassOptions[0] = "- " + t("undetected") + " -";
        for (int i = 1; i < localizedClassOptions.length; i++) {
            localizedClassOptions[i] = t(Monitor.hsClassOptions[i]);
        }

        add(new JLabel(t("match.label.your_class")), "right");

        yourClassComboBox = new JComboBox<>(localizedClassOptions);
        yourClassComboBox.setMinimumSize(new Dimension(150, 27));
        yourClassComboBox.setPreferredSize(new Dimension(200, 28));
        if (match.getUserClass() == null) {
            yourClassComboBox.setSelectedIndex(0);
        } else {
            yourClassComboBox.setSelectedItem(match.getUserClass());
        }
        yourClassComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (yourClassComboBox.getSelectedIndex() > 0) {
                    match.setUserClass(Monitor.hsClassOptions[yourClassComboBox.getSelectedIndex()]);
                }
            }
        });
        add(yourClassComboBox, "");

        add(new JLabel(t("match.label.opponents_class")), "right");

        opponentClassComboBox = new JComboBox<>(localizedClassOptions);
        opponentClassComboBox.setMinimumSize(new Dimension(150, 27));
        opponentClassComboBox.setPreferredSize(new Dimension(200, 28));
        if (match.getOpponentClass() == null) {
            opponentClassComboBox.setSelectedIndex(0);
        } else {
            opponentClassComboBox.setSelectedItem(match.getOpponentClass());
        }
        opponentClassComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (opponentClassComboBox.getSelectedIndex() > 0) {
                    match.setOpponentClass(Monitor.hsClassOptions[opponentClassComboBox.getSelectedIndex()]);
                }
            }
        });
        add(opponentClassComboBox, "wrap");


        //// Row 6 ////

        add(new JLabel(t("match.label.coin")), "right");
        coinCheckBox = new JCheckBox(t("match.coin"));
        coinCheckBox.setSelected(match.hasCoin());
        coinCheckBox.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                match.setCoin(coinCheckBox.isSelected());
            }
        });
        add(coinCheckBox, "wrap");


        //// Row 7 ////

        add(new JLabel("Result:"), "right, gapy 20px 20px");

        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

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



        //// Row 8 ////

        add(new JLabel(t("match.label.notes")), "right");
        notesTextArea = new JTextArea();
        notesTextArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        notesTextArea.setMinimumSize(new Dimension(400, 100));
        notesTextArea.setPreferredSize(new Dimension(540, 150));
        notesTextArea.setMaximumSize(new Dimension(540, 200));
        notesTextArea.setBackground(Color.WHITE);
        notesTextArea.setText(match.getNotes());
        notesTextArea.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                match.setNotes(notesTextArea.getText());
            }
        });
        add(notesTextArea, "span 3, wrap");

    }


    /**
     * Loads text from the main resource bundle, using the local language when available.
     * @param key the key for the desired string
     * @return The requested string
     */
    private String t(String key) {
        return bundle.getString(key);
    }

}
