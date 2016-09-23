package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import uk.ac.rhul.cs.dice.vacuumworld.view.ModelUpdate;
import uk.ac.rhul.cs.dice.vacuumworld.view.ViewRequest;

public class ControllerServer {
	private static ControllerServer instance;
	private static String modelIp;
	private static int modelPort;
	private static ConcurrentLinkedQueue<ViewRequest> viewRequests;
	private static ConcurrentLinkedQueue<ModelUpdate> modelUpdates;
	private static Socket socketWithView;
	private static ObjectOutputStream toViewStream;
	private static ObjectInputStream fromViewStream;
	private static Socket socketWithModel;
	private static ObjectOutputStream toModelStream;
	private static ObjectInputStream fromModelStream;
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
		fromView = new ViewIncomingManagerRunnable(socketWithView, fromViewStream, viewRequests);
		toView = new ViewOutoingManagerRunnable(socketWithView, toViewStream, modelUpdates);
		fromModel = new ModelIncomingManagerRunnable(socketWithModel, fromModelStream, modelUpdates);
		toModel = new ModelOutgoingManagerRunnable(socketWithModel, toModelStream, viewRequests);
		
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
			else if(socketWithView.isClosed()) {
				waitForViewConnection();
			}
		}
		catch(Exception e) {
			return;
		}
	}

	private static void waitForViewConnection() throws IOException, ClassNotFoundException, HandshakeException {
		System.out.println("Waiting for view...");
		Socket socket = server.accept();
		ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
		
		HandshakeCodes code = HandshakeCodes.fromString((String) input.readObject());
		System.out.println("received " + (code == null ? null : code.toString()) + " from view");
		
		if(code == null) {
			throw new IllegalArgumentException("Bad handshake.");
		}
		
		if(Handshake.attemptHandshakeWithView(toModelStream, fromModelStream, output, code)) {
			socketWithView = socket;
			toViewStream = output;
			fromViewStream = input;
		}
	}

	private static void connectWithModelIfNecessary() {
		try {
			if(socketWithModel == null) {
				tryHandshake();
			}
			else if(socketWithModel.isClosed()) {
				tryHandshake();
			}
		}
		catch(Exception e) {
			return;
		}
	}

	private static void tryHandshake() throws IOException, HandshakeException {
		Socket socket = new Socket(modelIp, modelPort);
		ObjectOutputStream o = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream i = new ObjectInputStream(socket.getInputStream());
		
		if(Handshake.attemptHandshakeWithModel(o, i)) {
			socketWithModel = socket;
			toModelStream = o;
			fromModelStream = i;
		}
	}
}