package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;

public class ControllerModelRunnable implements Runnable {
	private String modelIp;
	private int modelPort;
	private Queue<ViewRequest> viewRequests;
	private Queue<ModelUpdate> modelUpdates;
	private Socket connectionWithModel;
	private InputStream input;
	private ObjectInputStream i;
	private OutputStream output;
	private ObjectOutputStream o;
	private boolean allRight;
	
	public ControllerModelRunnable(String modelIp, int modelPort, Queue<ViewRequest> viewRequests, Queue<ModelUpdate> modelUpdates) {
		this.modelIp = modelIp;
		this.modelPort = modelPort;
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
			if(!this.viewRequests.isEmpty()) {
				ViewRequest request = this.viewRequests.poll();
				this.o.writeObject(request);
			}
			
			this.modelUpdates.add((ModelUpdate) this.i.readObject());
		}
	}

	private void manageException(Exception e) {
		Utils.log(e);
		this.allRight = false;
		
		try {
			this.connectionWithModel.close();
		}
		catch(Exception ex) {
			Utils.log("Socket already closed.", ex);
		}
	}

	private void connect() throws IOException {
		if(!checkConnected()) {
			this.connectionWithModel = new Socket(this.modelIp, this.modelPort);
			this.input = this.connectionWithModel.getInputStream();
			this.output = this.connectionWithModel.getOutputStream();
			this.i = new ObjectInputStream(this.input);
			this.o = new ObjectOutputStream(this.output);
			this.allRight = true;
		}	
	}

	private boolean checkConnected() {
		if(this.connectionWithModel == null) {
			return false;
		}
		else if(this.connectionWithModel.isConnected()) {
			return true;
		}
		
		return false;
	}
}