package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ControllerServer {
	private static ControllerServer instance;
	private static String modelIp;
	private static int modelPort;
	private static ConcurrentLinkedQueue<ViewRequest> viewRequests;
	private static ConcurrentLinkedQueue<ModelUpdate> modelUpdates;
	
	private ControllerServer(String modelIp, int modelPort) {
		ControllerServer.modelIp = modelIp;
		ControllerServer.modelPort = modelPort;
		ControllerServer.viewRequests = new ConcurrentLinkedQueue<>();
		ControllerServer.modelUpdates = new ConcurrentLinkedQueue<>();
	}
	
	public static ControllerServer getInstance(String modelIp, int modelPort) {
		if(instance == null) {
			instance = new ControllerServer(modelIp, modelPort);
		}
		
		return instance;
	}
	
	public static void startControllerServer() {
		ViewControllerRunnable viewControllerRunnable = new ViewControllerRunnable(viewRequests, modelUpdates);
		ControllerModelRunnable controllerModelRunnable = new ControllerModelRunnable(modelIp, modelPort, viewRequests, modelUpdates);
		
		Thread viewControllerThread = new Thread(viewControllerRunnable);
		Thread controllerModelThread = new Thread(controllerModelRunnable);
		
		viewControllerThread.start();
		controllerModelThread.start();
	}
}