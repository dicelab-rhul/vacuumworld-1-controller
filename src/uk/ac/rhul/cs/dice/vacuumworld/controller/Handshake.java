package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.HandshakeCodes;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.HandshakeException;

public class Handshake {
	private static final String ERROR = "Bad handshake.";
	private static final int TIME_TO_WAIT = 10000;
	
	private Handshake(){}
	
	public static Socket attemptHandshakeWithModel(String modelIp, int modelPort) throws HandshakeException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Socket> future = executor.submit(() -> doHandshakeWithModel(modelIp, modelPort));
		
		try {
			return future.get(TIME_TO_WAIT, TimeUnit.MILLISECONDS);
		}
		catch (Exception e) {
			throw new HandshakeException(e);
		}
	}
	
	private static Socket doHandshakeWithModel(String modelIp, int modelPort) throws IOException, ClassNotFoundException {
		Socket modelSocket = new Socket(modelIp, modelPort);
		ObjectOutputStream toModel = new ObjectOutputStream(modelSocket.getOutputStream());
		ObjectInputStream fromModel = new ObjectInputStream(modelSocket.getInputStream());
		
		return doHandshakeWithModel(modelSocket, toModel, fromModel);
	}
	
	private static Socket doHandshakeWithModel(Socket modelSocket, ObjectOutputStream toModel, ObjectInputStream fromModel) throws IOException, ClassNotFoundException {
		toModel.writeObject(HandshakeCodes.CHCM);
		toModel.flush();
		
		Object response = fromModel.readObject();
		
		if(response instanceof HandshakeCodes) {
			return finalizeHandshakeWithModel(modelSocket, (HandshakeCodes) response);
		}
		else {
			throw new IOException(ERROR);
		}
	}

	private static Socket finalizeHandshakeWithModel(Socket modelSocket, HandshakeCodes response) {
		if(HandshakeCodes.MHMC.equals(response)) {
			return modelSocket;
		}
		else {
			throw new IllegalArgumentException(ERROR);
		}
	}

	public static Socket attemptHandshakeWithView(Socket viewSocket, Socket mSocket, HandshakeCodes codeFromView, String modelIp, int modelPort) throws HandshakeException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Socket> future = executor.submit(() -> doHandshakeWithView(viewSocket, mSocket, codeFromView, modelIp, modelPort));
		
		try {
			return future.get(TIME_TO_WAIT, TimeUnit.MILLISECONDS);
		}
		catch (Exception e) {
			throw new HandshakeException(e);
		}
	}
	
	private static Socket doHandshakeWithView(Socket viewSocket, Socket mSocket, HandshakeCodes codeFromView, String modelIp, int modelPort) throws IOException, ClassNotFoundException {
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
	}

	private static Socket doHandshakeWithView(Socket viewSocket, HandshakeCodes codeFromView, ObjectOutputStream toView, ObjectOutputStream toModel, ObjectInputStream fromModel) throws IOException, ClassNotFoundException {
		if(HandshakeCodes.VHVC.equals(codeFromView)) {
			return doHandshakeWithView(viewSocket, toView, toModel, fromModel);
		}
		else {
			throw new IllegalArgumentException(ERROR);
		}
	}

	private static Socket doHandshakeWithView(Socket viewSocket, ObjectOutputStream toView, ObjectOutputStream toModel, ObjectInputStream fromModel) throws IOException, ClassNotFoundException {
		toView.writeObject(HandshakeCodes.CHCV);
		toView.flush();
		toModel.writeObject(HandshakeCodes.CHVM);
		toModel.flush();
		
		return doHandshakeWithView(viewSocket, toView, fromModel);
	}

	private static Socket doHandshakeWithView(Socket viewSocket, ObjectOutputStream toView, ObjectInputStream fromModel) throws ClassNotFoundException, IOException {
		Object response = fromModel.readObject();
		
		if(response instanceof HandshakeCodes) {
			return finalizeHandshakeWithView(viewSocket, toView, (HandshakeCodes) response);
		}
		else {
			throw new IOException(ERROR);
		}
	}

	private static Socket finalizeHandshakeWithView(Socket viewSocket, ObjectOutputStream toView, HandshakeCodes response) throws IOException {
		if(HandshakeCodes.MHMV.equals(response)) {
			toView.writeObject(HandshakeCodes.CHMV);
			toView.flush();
			
			return viewSocket;
		}
		else {
			throw new IllegalArgumentException(ERROR);
		}
	}
}