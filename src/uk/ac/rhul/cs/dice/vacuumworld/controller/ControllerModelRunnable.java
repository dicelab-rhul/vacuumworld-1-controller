package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;

public class ControllerModelRunnable implements Runnable {
	private String modelIp;
	private int modelPort;
	private Queue<ViewRequest> viewRequests;
	private Queue<ModelUpdate> modelUpdates;
	private Socket socketWithModel;
	private ObjectInputStream fromModel;
	private ObjectOutputStream toModel;
	private boolean allRight;
	
	public ControllerModelRunnable(String modelIp, int modelPort, Queue<ViewRequest> viewRequests, Queue<ModelUpdate> modelUpdates) throws IOException {
		this.modelIp = modelIp;
		this.modelPort = modelPort;
		this.viewRequests = viewRequests;
		this.modelUpdates = modelUpdates;
		this.allRight = true;
	}
	
	public Socket getSocketWithModel() {
		if(this.socketWithModel == null) {
			return null;
		}
		else if(!this.socketWithModel.isConnected()) {
			return null;
		}
		else {
			return this.socketWithModel;
		}
	}

	@Override
	public void run() {
		try {
			this.socketWithModel = Handshake.attemptHandshakeWithModel(this.modelIp, this.modelPort);
			this.fromModel = new ObjectInputStream(this.socketWithModel.getInputStream());
			this.toModel = new ObjectOutputStream(this.socketWithModel.getOutputStream());
			
			doJob();
		}
		catch (Exception e) {
			manageException(e);
		}
	}

	private void doJob() throws IOException, ClassNotFoundException {
		while(this.allRight) {
			if(!this.viewRequests.isEmpty()) {
				ViewRequest request = this.viewRequests.poll();
				this.toModel.writeObject(request);
				this.toModel.flush();
			}
			
			this.modelUpdates.add((ModelUpdate) this.fromModel.readObject());
		}
	}

	private void manageException(Exception e) {
		Utils.log(e);
		this.allRight = false;
		
		try {
			this.socketWithModel.close();
		}
		catch(Exception ex) {
			Utils.log("Socket already closed.", ex);
		}
	}
}