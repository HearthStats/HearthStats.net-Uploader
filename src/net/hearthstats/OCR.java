package net.hearthstats;

import java.io.File;

import javax.swing.JOptionPane;

import net.sourceforge.tess4j.Tesseract;

public class OCR {
	
	private static String _tessdataPath = "tessdata";
			
	public static void setTessdataPath(String path)  {
		_tessdataPath = path;
	}

	public static String process(String filePath) {
		// perform ocr
		File imageFile = new File(filePath); 
		
        String output = "";
        try {
        	Tesseract instance = Tesseract.getInstance(); 
        	instance.setDatapath(_tessdataPath);
	        output += instance.doOCR(imageFile);
	        output = output.replaceAll("\\s+","");
        } catch (Exception e) {
        	JOptionPane.showMessageDialog(null, "Exception in OCR: " + e.toString());
        }
        return output;
	}

}
