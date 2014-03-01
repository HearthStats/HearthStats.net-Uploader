package net.hearthstats;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.JOptionPane;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class OCR {
	
	private static String _tessdataPath = "tessdata";
	private static String _lang = "eng";
			
	public static void setTessdataPath(String path)  {
		_tessdataPath = path;
	}

	public static void setLang(String lang)  {
		_lang = lang;
	}
	public static String process(BufferedImage image) {
		// perform ocr
		
		String output = "";
		try {
			Tesseract instance = Tesseract.getInstance(); 
			instance.setDatapath(_tessdataPath);
			instance.setLanguage(_lang);
			output += instance.doOCR(image);
			output = output.trim();
		} catch(Exception e) {
			Main.logException(e);
		}
		return output;
	}

}
