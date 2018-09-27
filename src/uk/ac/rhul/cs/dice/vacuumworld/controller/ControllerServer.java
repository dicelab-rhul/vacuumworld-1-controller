package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import uk.ac.rhul.cs.dice.vacuumworld.controller.managers.ModelIncomingManagerRunnable;
import uk.ac.rhul.cs.dice.vacuumworld.controller.managers.ModelOutgoingManagerRunnable;
import uk.ac.rhul.cs.dice.vacuumworld.controller.managers.ViewIncomingManagerRunnable;
import uk.ac.rhul.cs.dice.vacuumworld.controller.managers.ViewOutoingManagerRunnable;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.ConfigData;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.HandshakeCodes;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.HandshakeException;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ModelUpdate;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.StopSignal;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ViewRequest;

public class ControllerServer {
	private static ControllerServer instance;
	private static ServerSocket server;
	
	private static ConcurrentLinkedQueue<ViewRequest> viewRequests;
	private static ConcurrentLinkedQueue<ModelUpdate> modelUpdates;
	
	private static Socket socketWithView;
	private static ObjectOutputStream toViewStream;
	private static ObjectInputStream fromViewStream;
	
	private static Socket socketWithModel;
	private static ObjectOutputStream toModelStream;
	private static ObjectInputStream fromModelStream;
	
	private static volatile StopSignal sharedStopSignal;
	private static ExecutorService executor;
	
	private static ViewIncomingManagerRunnable fromView;
	private static ViewOutoingManagerRunnable toView;
	private static ModelIncomingManagerRunnable fromModel;
	private static ModelOutgoingManagerRunnable toModel;
	
	private ControllerServer() {
		ControllerServer.viewRequests = new ConcurrentLinkedQueue<>();
		ControllerServer.modelUpdates = new ConcurrentLinkedQueue<>();
		ControllerServer.sharedStopSignal = new StopSignal();
	}
	
	public static ControllerServer getInstance() {
		if(ControllerServer.instance == null) {
			ControllerServer.instance = new ControllerServer();
		}
		
		return ControllerServer.instance;
	}
	
	public static void startControllerServer() throws IOException {
		Utils.logWithClass(ControllerServer.class.getSimpleName(), "Starting controller server...");
		
		ControllerServer.server = new ServerSocket(ConfigData.getControllerPort());
		Utils.logWithClass(ControllerServer.class.getSimpleName(), "Controller server started.");
		
		connectWithModelIfNecessary();
		waitForViewConnectionIfNecessary();
		
		Utils.logWithClass(ControllerServer.class.getSimpleName(), "Handshake with Controller and View succesfully completed.");
		
		startManagers();
		startMonitoringLoop();
		
		ControllerServer.server.close();
	}

	private static void startMonitoringLoop() throws IOException {
		while(((ThreadPoolExecutor) ControllerServer.executor).getActiveCount() == 4) {
			continue;
		}
		
		if(ControllerServer.sharedStopSignal.mustStop()) {
			waitAndAct();
		}
		else {
			killLeftovers("One or more threads died. Killing leftovers...");
		}
	}

	private static void waitAndAct() {
		while(((ThreadPoolExecutor) ControllerServer.executor).getActiveCount() != 1) {
			continue;
		}
		
		killLeftovers(ViewIncomingManagerRunnable.class.getSimpleName() + " left to kill. Proceeding...");
	}

	private static void killLeftovers(String message) {
		try {
			Utils.logWithClass(ControllerServer.class.getSimpleName(), message);
			
			Utils.closeInputStreamIfNecessary(ControllerServer.fromModelStream);
			Utils.closeOutputStreamIfNecessary(ControllerServer.toModelStream);
			Utils.closeSocketIfNecessary(ControllerServer.socketWithModel);
			Utils.closeInputStreamIfNecessary(ControllerServer.fromViewStream);
			Utils.closeOutputStreamIfNecessary(ControllerServer.toViewStream);
			Utils.closeSocketIfNecessary(ControllerServer.socketWithView);
			
			ControllerServer.executor.shutdownNow();
			ControllerServer.executor.awaitTermination(5, TimeUnit.SECONDS);
			Utils.logWithClass(ControllerServer.class.getSimpleName(), "Done.");
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void startManagers() throws IOException {
		ControllerServer.fromView = new ViewIncomingManagerRunnable(ControllerServer.sharedStopSignal, ControllerServer.socketWithView, ControllerServer.fromViewStream, ControllerServer.viewRequests);
		ControllerServer.toView = new ViewOutoingManagerRunnable(ControllerServer.sharedStopSignal, ControllerServer.socketWithView, ControllerServer.toViewStream, ControllerServer.modelUpdates);
		ControllerServer.fromModel = new ModelIncomingManagerRunnable(ControllerServer.sharedStopSignal, ControllerServer.socketWithModel, ControllerServer.fromModelStream, ControllerServer.modelUpdates);
		ControllerServer.toModel = new ModelOutgoingManagerRunnable(ControllerServer.sharedStopSignal, ControllerServer.socketWithModel, ControllerServer.toModelStream, ControllerServer.viewRequests);
		
		startThreads();
	}

	private static void startThreads() {
		ControllerServer.executor = Executors.newFixedThreadPool(4);
		
		ControllerServer.executor.execute(ControllerServer.fromModel);
		ControllerServer.executor.execute(ControllerServer.toModel);
		ControllerServer.executor.execute(ControllerServer.fromView);
		ControllerServer.executor.execute(ControllerServer.toView);
	}

	private static void waitForViewConnectionIfNecessary() {
		try {
			if(ControllerServer.socketWithView == null) {
				waitForViewConnection();
				
				return;
			}
			
			if(ControllerServer.socketWithView.isClosed()) {
				waitForViewConnection();
				
				return;
			}
		}
		catch(Exception e) {
			Utils.log(e);
		}
	}

	private static void waitForViewConnection() throws IOException, ClassNotFoundException, HandshakeException {
		Utils.logWithClass(ControllerServer.class.getSimpleName(), "Waiting for view...");

		Socket socket = ControllerServer.server.accept();
		ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
		
		Utils.logWithClass(ControllerServer.class.getSimpleName(), "Controller server connected with (presumably the View server) " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ".");
		
		HandshakeCodes code = HandshakeCodes.fromString((String) input.readObject());
		
		Utils.logWithClass(ControllerServer.class.getSimpleName(), "Received " + (code == null ? null : code.toString()) + " from view.");
		
		attemptHandshakeWithView(code, socket, input, output);
	}

	private static void attemptHandshakeWithView(HandshakeCodes code, Socket socket, ObjectInputStream input, ObjectOutputStream output) throws HandshakeException {
		if(code == null) {
			throw new IllegalArgumentException("Bad handshake.");
		}
		
		if(Handshake.attemptHandshakeWithView(ControllerServer.toModelStream, ControllerServer.fromModelStream, output, code)) {
			ControllerServer.socketWithView = socket;
			ControllerServer.toViewStream = output;
			ControllerServer.fromViewStream = input;
		}
	}

	private static void connectWithModelIfNecessary() {
		try {
			if(ControllerServer.socketWithModel == null) {
				tryHandshakeWithModel();
				
				return;
			}
			
			if(ControllerServer.socketWithModel.isClosed()) {
				tryHandshakeWithModel();
				
				return;
			}
		}
		catch(Exception e) {
			Utils.log(e);
		}
	}

	private static void tryHandshakeWithModel() throws IOException, HandshakeException {
		Socket socket = new Socket(ConfigData.getModelIp(), ConfigData.getModelPort());
		ObjectOutputStream o = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream i = new ObjectInputStream(socket.getInputStream());
		
		Utils.logWithClass(ControllerServer.class.getSimpleName(), "Controller server connected with (presumably the Model server) " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ".");
		
		checkHandshakeWithModelResult(socket, i, o);
	}

	private static void checkHandshakeWithModelResult(Socket socket, ObjectInputStream i, ObjectOutputStream o) throws HandshakeException, IOException {
		if(Handshake.attemptHandshakeWithModel(o, i)) {
			ControllerServer.socketWithModel = socket;
			ControllerServer.toModelStream = o;
			ControllerServer.fromModelStream = i;			
		}
		else {
			socket.close();
		}
	}
}