package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.HandshakeCodes;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.HandshakeException;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;

public class ViewControllerRunnable implements Runnable {
	private Queue<ViewRequest> viewRequests;
	private Queue<ModelUpdate> modelUpdates;
	private ServerSocket serverSocket;
	private Socket socketWithView;
	private ObjectInputStream fromView;
	private ObjectOutputStream toView;
	private Socket socketWithModel;
	private boolean allRight;
	
	public ViewControllerRunnable(Socket socketWithModel, Queue<ViewRequest> viewRequests, Queue<ModelUpdate> modelUpdates) {
		this.socketWithModel = socketWithModel;
		this.viewRequests = viewRequests;
		this.modelUpdates = modelUpdates;
	}

	@Override
	public void run() {
		try {
			connect();
			doJob();
		}
		catch (Exception e) {
			manageException(e);
		}
	}
	
	private void doJob() throws IOException, ClassNotFoundException {
		while(this.allRight) {
			this.viewRequests.add((ViewRequest) this.fromView.readObject());
			
			if(!this.modelUpdates.isEmpty()) {
				ModelUpdate update = this.modelUpdates.poll();
				this.toView.writeObject(update);
				this.toView.flush();
			}
		}
	}
	
	private void manageException(Exception e) {
		Utils.log(e);
		this.allRight = false;
		
		try {
			this.socketWithView.close();
		}
		catch(Exception ex) {
			Utils.log("Socket already closed.", ex);
		}
	}

	private void connect() throws IOException, HandshakeException, ClassNotFoundException {
		if(!checkConnected()) {
			doHandshake();
			this.allRight = true;
		}	
	}

	private void doHandshake() throws IOException, HandshakeException, ClassNotFoundException {
		this.socketWithView = this.serverSocket.accept();
		HandshakeCodes codeFromView = getCodeFromView();
		
		this.socketWithView = Handshake.attemptHandshakeWithView(this.socketWithView, this.socketWithModel, codeFromView, null, 0);
		this.fromView = new ObjectInputStream(this.socketWithView.getInputStream());
		this.toView = new ObjectOutputStream(this.socketWithView.getOutputStream());
	}

	private HandshakeCodes getCodeFromView() throws IOException, HandshakeException, ClassNotFoundException {
		ObjectInputStream temp = new ObjectInputStream(this.socketWithView.getInputStream());
		Object code = temp.readObject();
		
		if(!(code instanceof HandshakeCodes)) {
			throw new HandshakeException("Bad handshake.");
		}
		else {
			return (HandshakeCodes) code;
		}
	}

	private boolean checkConnected() {
		if(this.socketWithView == null) {
			return false;
		}
		else if(this.socketWithView.isConnected()) {
			return true;
		}
		
		return false;
	}
}