package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;

public class ViewControllerRunnable implements Runnable {
	private Queue<ViewRequest> viewRequests;
	private Queue<ModelUpdate> modelUpdates;
	private ServerSocket serverSocket;
	private Socket connectionWithView;
	private InputStream input;
	private ObjectInputStream i;
	private OutputStream output;
	private ObjectOutputStream o;
	private boolean allRight;
	
	public ViewControllerRunnable(Queue<ViewRequest> viewRequests, Queue<ModelUpdate> modelUpdates) {
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
			this.viewRequests.add((ViewRequest) this.i.readObject());
			
			if(!this.modelUpdates.isEmpty()) {
				ModelUpdate update = this.modelUpdates.poll();
				this.o.writeObject(update);
			}
		}
	}
	
	private void manageException(Exception e) {
		Utils.log(e);
		this.allRight = false;
		
		try {
			this.connectionWithView.close();
		}
		catch(Exception ex) {
			Utils.log("Socket already closed.", ex);
		}
	}

	private void connect() throws IOException {
		if(!checkConnected()) {
			this.connectionWithView = this.serverSocket.accept();
			this.input = this.connectionWithView.getInputStream();
			this.output = this.connectionWithView.getOutputStream();
			this.i = new ObjectInputStream(this.input);
			this.o = new ObjectOutputStream(this.output);
			this.allRight = true;
		}	
	}

	private boolean checkConnected() {
		if(this.connectionWithView == null) {
			return false;
		}
		else if(this.connectionWithView.isConnected()) {
			return true;
		}
		
		return false;
	}
}