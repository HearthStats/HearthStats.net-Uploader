package net.hearthstats;

import java.io.File;

import javax.swing.JOptionPane;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class OCR {
	
	private static String _tessdataPath = "tessdata";
			
	public static void setTessdataPath(String path)  {
		_tessdataPath = path;
	}

	public static String process(String filePath) throws TesseractException {
		// perform ocr
		File imageFile = new File(filePath); 
		
        String output = "";
    	Tesseract instance = Tesseract.getInstance(); 
    	instance.setDatapath(_tessdataPath);
        output += instance.doOCR(imageFile);
        output = output.replaceAll("\\s+","");
        return output;
	}

}
