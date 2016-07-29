package uk.ac.rhul.cs.dice.vacuumworld.controller.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {
	public static final String MODEL_IP = "127.0.0.1";
	public static final int MODEL_PORT = 10890;
	public static final String CONTROLLER_IP = "127.0.0.1";
	public static final int CONTROLLER_PORT = 1890;
	private static final Logger LOGGER = Logger.getGlobal();
	
	private Utils(){}
	
	public static void log(String message) {
		log(Level.INFO, message);
	}
	
	public static void log(Exception e) {
		log(e.getMessage(), e);
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

	public static void doWait(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		}
		catch(Exception e) {
			Utils.log(e);
		}
	}
}