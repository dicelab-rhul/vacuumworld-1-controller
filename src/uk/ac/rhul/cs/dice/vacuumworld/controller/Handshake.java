package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.HandshakeCodes;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.HandshakeException;

public class Handshake {
	private static final String ERROR = "Bad handshake.";
	private static final int TIME_TO_WAIT = 100000;
	
	private Handshake(){}
	
	public static Boolean attemptHandshakeWithModel(ObjectOutputStream toModel, ObjectInputStream fromModel) throws HandshakeException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> future = executor.submit(() -> doHandshakeWithModel(toModel, fromModel));
		
		try {
			return future.get(TIME_TO_WAIT, TimeUnit.MILLISECONDS);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new HandshakeException(e);
		}
	}
	
	/*private static Boolean doHandshakeWithModel(ObjectOutputStream toModel, ObjectInputStream fromModel) throws IOException, ClassNotFoundException {
		ObjectOutputStream toModel = new ObjectOutputStream(modelSocket.getOutputStream());
		ObjectInputStream fromModel = new ObjectInputStream(modelSocket.getInputStream());
		
		return doHandshakeWithModel(modelSocket, toModel, fromModel);
	}*/
	
	private static Boolean doHandshakeWithModel(ObjectOutputStream toModel, ObjectInputStream fromModel) throws IOException, ClassNotFoundException {
		toModel.writeObject(HandshakeCodes.CHCM.toString());
		toModel.flush();
		System.out.println("sent CHCM to model");
		
		HandshakeCodes response = HandshakeCodes.fromString((String) fromModel.readObject());
		System.out.println("received " + (response == null ? null : response.toString()) + " from model"); //MHMC
		
		if(response != null) {
			return finalizeHandshakeWithModel(response);
		}
		else {
			throw new IOException(ERROR);
		}
	}

	private static Boolean finalizeHandshakeWithModel(HandshakeCodes response) {
		if(HandshakeCodes.MHMC.equals(response)) {
			return true;
		}
		else {
			throw new IllegalArgumentException(ERROR);
		}
	}

	public static Boolean attemptHandshakeWithView(ObjectOutputStream toModel, ObjectInputStream fromModel, ObjectOutputStream toView, HandshakeCodes codeFromView) throws HandshakeException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> future = executor.submit(() -> doHandshakeWithView(toModel, fromModel, toView, codeFromView));
		
		try {
			return future.get(TIME_TO_WAIT, TimeUnit.MILLISECONDS);
		}
		catch (Exception e) {
			throw new HandshakeException(e);
		}
	}
	
	/*private static Socket doHandshakeWithView(Socket viewSocket, Socket mSocket, HandshakeCodes codeFromView, String modelIp, int modelPort) throws IOException, ClassNotFoundException {
		Socket modelSocket = getModelSocket(mSocket, modelIp, modelPort);
		
		ObjectOutputStream toView = new ObjectOutputStream(viewSocket.getOutputStream());
		ObjectOutputStream toModel = new ObjectOutputStream(modelSocket.getOutputStream());
		ObjectInputStream fromModel = new ObjectInputStream(modelSocket.getInputStream());
		
		return doHandshakeWithView(viewSocket, codeFromView, toView, toModel, fromModel);
	}

	private static Socket getModelSocket(Socket mSocket, String modelIp, int modelPort) throws IOException, ClassNotFoundException {
		if(mSocket == null) {
			return doHandshakeWithModel(modelIp, modelPort);
		}
		else if(mSocket.isClosed() || !mSocket.isConnected()) {
			return doHandshakeWithModel(modelIp, modelPort);
		}
		else {
			return mSocket;
		}
	}*/

	private static Boolean doHandshakeWithView(ObjectOutputStream toModel, ObjectInputStream fromModel, ObjectOutputStream toView, HandshakeCodes codeFromView) throws IOException, ClassNotFoundException {
		if(HandshakeCodes.VHVC.equals(codeFromView)) {
			return doHandshakeWithView(toView, toModel, fromModel);
		}
		else {
			throw new IllegalArgumentException(ERROR);
		}
	}

	private static Boolean doHandshakeWithView(ObjectOutputStream toView, ObjectOutputStream toModel, ObjectInputStream fromModel) throws IOException, ClassNotFoundException {
		toView.writeObject(HandshakeCodes.CHCV.toString());
		toView.flush();
		System.out.println("sent CHCV to view");
		
		toModel.writeObject(HandshakeCodes.CHVM.toString());
		toModel.flush();
		System.out.println("sent CHVM to model");
		
		return doHandshakeWithView(toView, fromModel);
	}

	private static Boolean doHandshakeWithView(ObjectOutputStream toView, ObjectInputStream fromModel) throws ClassNotFoundException, IOException {
		HandshakeCodes response = HandshakeCodes.fromString((String) fromModel.readObject());
		System.out.println("received " + (response == null ? null : response.toString()) + " from model");
		
		if(response != null) {
			return finalizeHandshakeWithView(toView, response);
		}
		else {
			throw new IOException(ERROR);
		}
	}

	private static Boolean finalizeHandshakeWithView(ObjectOutputStream toView, HandshakeCodes response) throws IOException {
		if(HandshakeCodes.MHMV.equals(response)) {
			toView.writeObject(HandshakeCodes.CHMV.toString());
			toView.flush();
			System.out.println("sent CHMV to view");
			
			return true;
		}
		else {
			throw new IllegalArgumentException(ERROR);
		}
	}
}