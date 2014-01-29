package net.hearthstats;

import java.io.File;

import javax.swing.JOptionPane;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class OCR {

	public static String process(String filePath) {
		// perform ocr
		File imageFile = new File(filePath); 
		
        String output = "";
        try {
        	Tesseract instance = Tesseract.getInstance(); //
	        output += instance.doOCR(imageFile);
	        output = output.replaceAll("\\s+","");
        } catch (Exception e) {
        	JOptionPane.showMessageDialog(null, e.getMessage());
        }
        return output;
	}

}
