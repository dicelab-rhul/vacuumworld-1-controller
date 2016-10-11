package uk.ac.rhul.cs.dice.vacuumworld.controller.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.VacuumWorldLogFormatter;

public class Utils {
	private static final Logger LOGGER = initLogger();
	
	private Utils(){}
	
	private static Logger initLogger() {
		Logger logger = Logger.getAnonymousLogger();
		logger.setUseParentHandlers(false);
		VacuumWorldLogFormatter formatter = new VacuumWorldLogFormatter();
		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(formatter);
		logger.addHandler(handler);
		
		return logger;
	}
	
	public static void log(String message) {
		log(Level.INFO, message);
	}
	
	public static void log(Exception e) {
		log(e.getMessage(), e);
	}
	
	public static void fakeLog(Exception e) {
		//this exception does not need to be logged
	}

	public static void log(String message, Exception e) {
		log(Level.SEVERE, message, e);
	}

	public static void log(Level level, String message) {
		LOGGER.log(level, message);
	}

	public static void log(Level level, String message, Exception e) {
		LOGGER.log(level, message, e);
	}
	
	public static void logWithClass(String source, String message) {
		log(source + ": " + message);
	}
	
	public static void logOnFileAndOverwrite(String filename, String... toLog) {
		log(filename, false, toLog);
	}
	
	public static void logOnFileAndAppend(String filename, String... toLog) {
		log(filename, true, toLog);
	}
	
	public static void log(String filename, boolean append, String... toLog) {
		try(FileOutputStream fo = new FileOutputStream(filename, append)) {
			log(fo, toLog);
		}
		catch(Exception e) {
			log(e);
		}
		
	}

	private static void log(FileOutputStream fo, String... toLog) throws IOException {
		for(String line : toLog) {
			fo.write(line.getBytes());
			fo.write("\n".getBytes());
		}
		
		fo.flush();
	}

	public static void doWait(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		}
		catch(Exception e) {
			Utils.log(e);
		}
	}
}