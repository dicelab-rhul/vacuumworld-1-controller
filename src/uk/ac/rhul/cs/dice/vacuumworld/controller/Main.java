package uk.ac.rhul.cs.dice.vacuumworld.controller;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.ConfigData;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;

public class Main {
	private Main(){}
	
	public static void main(String[] args) {		
		if(args.length < 2) {
			Utils.logWithClass(Main.class.getSimpleName(), "Usage: java -jar <this_file> --config-file <config-json-file-path>");
		}
		else {
			String configFilePath = retrieveConfigFilePath(args);
			ConfigData.initConfigData(configFilePath);
			startController();
			System.exit(0);
		}
	}

	private static String retrieveConfigFilePath(String[] args) {
		if("--config-file".equals(args[0])) {
			return args[1];
		}
		else {
			return null;
		}
	}

	private static void startController() {		
		try {
			ControllerServer.getInstance();
			ControllerServer.startControllerServer();
			Utils.logWithClass(ControllerServer.class.getSimpleName(), "Bye!!!");
		}
		catch(Exception e) {
			Utils.log(e);
		}
	}
}