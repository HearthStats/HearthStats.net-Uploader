package net.hearthstats;

import java.io.File;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class OCR {

	public static String process(String filePath) {
		// perform ocr
		File imageFile = new File(filePath);
        Tesseract instance = Tesseract.getInstance(); //

        String output = "";
        try {
	        output += instance.doOCR(imageFile).replaceAll("\\s+","");
        } catch (TesseractException e) {
        	System.err.println(e.getMessage());
        }
        return output;
	}

}
