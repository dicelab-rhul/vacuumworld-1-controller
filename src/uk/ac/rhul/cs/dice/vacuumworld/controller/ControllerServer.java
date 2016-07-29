package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.managers.ModelIncomingManagerRunnable;
import uk.ac.rhul.cs.dice.vacuumworld.controller.managers.ModelOutgoingManagerRunnable;
import uk.ac.rhul.cs.dice.vacuumworld.controller.managers.ViewIncomingManagerRunnable;
import uk.ac.rhul.cs.dice.vacuumworld.controller.managers.ViewOutoingManagerRunnable;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.HandshakeCodes;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.HandshakeException;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.DeadThreadException;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;

public class ControllerServer {
	private static ControllerServer instance;
	private static String modelIp;
	private static int modelPort;
	private static ConcurrentLinkedQueue<ViewRequest> viewRequests;
	private static ConcurrentLinkedQueue<ModelUpdate> modelUpdates;
	private static Socket socketWithView;
	private static Socket socketWithModel;
	private static ServerSocket server;
	private static boolean started;
	
	private static ViewIncomingManagerRunnable fromView;
	private static ViewOutoingManagerRunnable toView;
	private static ModelIncomingManagerRunnable fromModel;
	private static ModelOutgoingManagerRunnable toModel;
	
	private static Thread fromViewThread;
	private static Thread toViewThread;
	private static Thread fromModelThread;
	private static Thread toModelThread;
	
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
	
	public static void startControllerServer() throws IOException {
		server = new ServerSocket(Utils.CONTROLLER_PORT);
		started = true;
		
		while(started) {
			connectWithModelIfNecessary();
			waitForViewConnectionIfNecessary();
			startManagers();
			monitorManagers();
			stopThreads();
		}
	}

	private static void stopThreads() {
		fromViewThread.interrupt();
		toViewThread.interrupt();
		fromModelThread.interrupt();
		toModelThread.interrupt();
	}

	private static void monitorManagers() {
		while(true) {
			try {
				boolean alive = fromViewThread.isAlive();
				checkDeadThread(alive);
				alive = toViewThread.isAlive();
				checkDeadThread(alive);
				alive = fromModelThread.isAlive();
				checkDeadThread(alive);
				alive = toModelThread.isAlive();
				checkDeadThread(alive);
			}
			catch(DeadThreadException e) {
				return;
			}
		}
	}
	
	private static void checkDeadThread(boolean alive) throws DeadThreadException {
		if(!alive) {
			throw new DeadThreadException();
		}
	}

	private static void startManagers() throws IOException {
		fromView = new ViewIncomingManagerRunnable(socketWithView, viewRequests);
		toView = new ViewOutoingManagerRunnable(socketWithView, modelUpdates);
		fromModel = new ModelIncomingManagerRunnable(socketWithModel, modelUpdates);
		toModel = new ModelOutgoingManagerRunnable(socketWithModel, viewRequests);
		
		createAndStartThreads();
	}

	private static void createAndStartThreads() {
		fromViewThread = new Thread(fromView);
		toViewThread = new Thread(toView);
		fromModelThread = new Thread(fromModel);
		toModelThread = new Thread(toModel);
		
		startThreads();
	}

	private static void startThreads() {
		toViewThread.start();
		toModelThread.start();
		fromModelThread.start();
		fromViewThread.start();
	}

	private static void waitForViewConnectionIfNecessary() {
		try {
			if(socketWithView == null) {
				waitForViewConnection();
				
			}
			else if(socketWithModel.isClosed()) {
				waitForViewConnection();
			}
		}
		catch(Exception e) {
			return;
		}
	}

	private static void waitForViewConnection() throws IOException, ClassNotFoundException, HandshakeException {
		socketWithView = server.accept();
		Object code = new ObjectInputStream(socketWithView.getInputStream()).readObject();
		
		if(!(code instanceof HandshakeCodes)) {
			throw new IllegalArgumentException("Bad handshake.");
		}
		
		socketWithView = Handshake.attemptHandshakeWithView(socketWithView, socketWithModel, (HandshakeCodes) code, modelIp, modelPort);
	}

	private static void connectWithModelIfNecessary() {
		try {
			if(socketWithModel == null) {
				socketWithModel = Handshake.attemptHandshakeWithModel(modelIp, modelPort);
			}
			else if(socketWithModel.isClosed()) {
				socketWithModel = Handshake.attemptHandshakeWithModel(modelIp, modelPort);
			}
		}
		catch(Exception e) {
			return;
		}
	}
}