package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.ConfigData;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.HandshakeCodes;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.HandshakeException;

public class Handshake {
	private static final String ERROR = "Bad handshake.";
	
	private Handshake(){}
	
	public static Boolean attemptHandshakeWithModel(ObjectOutputStream toModel, ObjectInputStream fromModel) throws HandshakeException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> future = executor.submit(() -> doHandshakeWithModel(toModel, fromModel));
		
		try {
			return future.get(ConfigData.getTimeoutInSeconds(), TimeUnit.SECONDS);
		}
		catch (Exception e) {
			throw new HandshakeException(e);
		}
	}
	
	private static Boolean doHandshakeWithModel(ObjectOutputStream toModel, ObjectInputStream fromModel) throws IOException, ClassNotFoundException {
		toModel.writeObject(HandshakeCodes.CHCM.toString());
		toModel.flush();
		
		Utils.logWithClass(Handshake.class.getSimpleName(), "Sent CHCM to model.");
		
		HandshakeCodes response = HandshakeCodes.fromString((String) fromModel.readObject());
		
		Utils.logWithClass(Handshake.class.getSimpleName(), "Received " + (response == null ? null : response.toString()) + " from model."); //MHMC
		
		if(response != null) {
			return finalizeHandshakeWithModel(response);
		}
		else {
			throw new IOException(Handshake.ERROR);
		}
	}

	private static Boolean finalizeHandshakeWithModel(HandshakeCodes response) {
		if(HandshakeCodes.MHMC.equals(response)) {
			return true;
		}
		else {
			throw new IllegalArgumentException(Handshake.ERROR);
		}
	}

	public static Boolean attemptHandshakeWithView(ObjectOutputStream toModel, ObjectInputStream fromModel, ObjectOutputStream toView, HandshakeCodes codeFromView) throws HandshakeException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> future = executor.submit(() -> doHandshakeWithView(toModel, fromModel, toView, codeFromView));
		
		try {
			return future.get(ConfigData.getTimeoutInSeconds(), TimeUnit.SECONDS);
		}
		catch (Exception e) {
			throw new HandshakeException(e);
		}
	}

	private static Boolean doHandshakeWithView(ObjectOutputStream toModel, ObjectInputStream fromModel, ObjectOutputStream toView, HandshakeCodes codeFromView) throws IOException, ClassNotFoundException {
		if(HandshakeCodes.VHVC.equals(codeFromView)) {
			return doHandshakeWithView(toView, toModel, fromModel);
		}
		else {
			throw new IllegalArgumentException(Handshake.ERROR);
		}
	}

	private static Boolean doHandshakeWithView(ObjectOutputStream toView, ObjectOutputStream toModel, ObjectInputStream fromModel) throws IOException, ClassNotFoundException {
		toView.writeObject(HandshakeCodes.CHCV.toString());
		toView.flush();
		
		Utils.logWithClass(Handshake.class.getSimpleName(), "Sent CHCV to view.");
		
		toModel.writeObject(HandshakeCodes.CHVM.toString());
		toModel.flush();
		
		Utils.logWithClass(Handshake.class.getSimpleName(), "Sent CHVM to model.");
		
		return doHandshakeWithView(toView, fromModel);
	}

	private static Boolean doHandshakeWithView(ObjectOutputStream toView, ObjectInputStream fromModel) throws ClassNotFoundException, IOException {
		HandshakeCodes response = HandshakeCodes.fromString((String) fromModel.readObject());
		Utils.logWithClass(Handshake.class.getSimpleName(), "Received " + (response == null ? null : response.toString()) + " from model.");
		
		if(response != null) {
			return finalizeHandshakeWithView(toView, response);
		}
		else {
			throw new IOException(Handshake.ERROR);
		}
	}

	private static Boolean finalizeHandshakeWithView(ObjectOutputStream toView, HandshakeCodes response) throws IOException {
		if(HandshakeCodes.MHMV.equals(response)) {
			toView.writeObject(HandshakeCodes.CHMV.toString());
			toView.flush();
			
			Utils.logWithClass(Handshake.class.getSimpleName(), "Sent CHMV to view.\n");
			
			return true;
		}
		else {
			throw new IllegalArgumentException(Handshake.ERROR);
		}
	}
}