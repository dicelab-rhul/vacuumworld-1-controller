package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.ViewRequest;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;

public class ModelOutgoingManagerRunnable implements Runnable {
	private Queue<ViewRequest> viewRequests;
	private Socket socketWithModel;
	private ObjectOutputStream toModel;
	private boolean allRight;
	
	public ModelOutgoingManagerRunnable(Socket socketWithModel, Queue<ViewRequest> viewRequests) throws IOException {
		this.socketWithModel = socketWithModel;
		this.toModel = new ObjectOutputStream(this.socketWithModel.getOutputStream());
		this.viewRequests = viewRequests;
		this.allRight = true;
	}
	
	@Override
	public void run() {
		while(this.allRight) {
			if(!this.viewRequests.isEmpty()) {
				forwardViewRequest();
			}
		}
	}

	private void forwardViewRequest() {
		try {
			ViewRequest request = this.viewRequests.poll();
			// maybe here the controller can inspect and pre-process the request (in the future / should the need arise).
			this.toModel.writeObject(request);
			this.toModel.flush();
		}
		catch(IOException e) {
			this.allRight = false;
			Utils.log(e);
			closeSocketWithModel();
		}
		catch(Exception e) {
			Utils.log(e);
		}
	}

	private void closeSocketWithModel() {
		try {
			this.socketWithModel.close();
		}
		catch (IOException e) {
			Utils.log(e);
		}
	}
}