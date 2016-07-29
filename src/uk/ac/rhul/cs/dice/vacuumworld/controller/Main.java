package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;

public class Main {
	private Main(){}
	
	public static void main(String[] args) {
		if(args.length < 2) {
			Utils.log("Expected 2 arguments, " + args.length + " given.");
			Utils.log("Usage: java -jar <this_file> <model_address> <model_port>");
			System.exit(-1);
		}
		
		String modelIp = args[0];
		int modelPort = Integer.parseInt(args[1]);
		
		try {
			startControllerServer(modelIp, modelPort);
		}
		catch(Exception e) {
			Utils.log(e);
			System.exit(-1);
		}
	}

	private static void startControllerServer(String modelIp, int modelPort) throws IOException {
		ControllerServer.getInstance(modelIp, modelPort);
		ControllerServer.startControllerServer();
	}
}