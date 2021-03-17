package at.ac.wu.asana.tryout;

import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import at.ac.wu.asana.csv.ExtractStructuralDataChanges;

public class ExtractStoriesFromProjectsCSV {
	
	static Options opts = new Options();
	static Logger logger = Logger.getLogger("ExtractionFromCSV");	
	
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		FileHandler fh;
		try {
			fh = new FileHandler(new Date()+"-ExtractionFromProjectsCSV.log");
			logger.addHandler(fh);
		    SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    
		
		CommandLineParser lineParser = new DefaultParser();
		CommandLine line = null;
		Options opts = new Options();
		opts.addOption(new Option("csv", true, "the output file produced"))
				.addOption(new Option("pat", true, "the personal access token"))
				.addOption(new Option("ws", true, "the workspace"));
		
		try {
			line = lineParser.parse(opts, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
				
		String pat = line.getOptionValue("pat");
		String ws = line.getOptionValue("ws");
		String csv = line.getOptionValue("csv");
		String p = line.getOptionValue("p");
		String r = line.getOptionValue("r");
		String ots = line.getOptionValue("ots");
		String os = line.getOptionValue("os");
		
		String info = "Extraction started with parameters "+"\npat:"+pat+""
				+ "\nws:" +ws + "," 
				+ "\ncsv:" +csv + ","
				+ "\np:" +p + ","
				+ "\nr:" +r + ","
				+ "\nots:"+ ots
				+ "\nos:"+ os;
		logger.info(info);

		extractFromFile(pat,ws,csv,p,r,(ots!=null),(os!=null));	

		logger.info("All done in "+ ExtractStructuralDataChanges.getElapsedTime(System.currentTimeMillis(), start));
	}

	private static void extractFromFile(String pat, String ws, String csv, String p, String r, boolean b, boolean c) {
		
	}

}
