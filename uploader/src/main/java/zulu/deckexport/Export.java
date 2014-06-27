package zulu.deckexport;

import java.awt.image.BufferedImage;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import net.hearthstats.Config;
import net.hearthstats.ProgramHelper;
import net.hearthstats.util.HsRobot;
import zulu.deckexport.card.Deck;
import zulu.deckexport.extracter.ExtracterMain;

public class Export {
	/**
	 * Implementation of Deck Export Feature
	 */
	public static void deckExport()
	{
		ProgramHelper _hsHelper = Config.programHelper();
		_hsHelper.setWindowVisible();							
		BufferedImage img1 = _hsHelper.getScreenCapture();
		HsRobot _hsRobot = new HsRobot(_hsHelper.getHSWindowBounds(),1);
		_hsRobot.export();
		BufferedImage img2 = _hsHelper.getScreenCapture();
		Deck deck  = ExtracterMain.exportDeck(img1, img2);

		if(deck == null) System.out.println("Deck is invalid.");
		else {
			showMessageDialog(deck); // This method is just for the illustration
			
			// deck options should be set here before uploading to server such as deckname, deck class, deck slot, deck owner etc.
			
			// upload deck to server
		}
	}
	/**
	 * This method is available only for illustration.(Can be deleted)
	 * @param deck
	 */
	public static void showMessageDialog(Deck deck) {
		JOptionPane op = new JOptionPane("Deck built and uploaded successfully.", JOptionPane.INFORMATION_MESSAGE);
		JScrollPane scrollPane = new JScrollPane();
		JList list = new JList();
		scrollPane.setViewportView(list);
		list.setListData(deck.toArray());
		op.add(scrollPane);
		JDialog dialog = op.createDialog(null, "Deck Exporter Info");
		dialog.setAlwaysOnTop(true);
		dialog.setModal(true);
		dialog.setFocusableWindowState(true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
	}
}
