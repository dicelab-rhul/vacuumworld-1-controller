package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ViewRequest;

public class ModelOutgoingManagerRunnable implements Runnable {
	private Queue<ViewRequest> viewRequests;
	private Socket socketWithModel;
	private ObjectOutputStream toModel;
	private boolean allRight;
	
	public ModelOutgoingManagerRunnable(Socket socketWithModel, ObjectOutputStream toModel, Queue<ViewRequest> viewRequests) throws IOException {
		this.socketWithModel = socketWithModel;
		this.toModel = toModel;
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
			System.out.println("Before polling view request for model");
			ViewRequest request = this.viewRequests.poll();
			// maybe here the controller can inspect and pre-process the request (in the future / should the need arise).
			System.out.println("Before sending view request to model");
			this.toModel.writeObject(request);
			this.toModel.flush();
			System.out.println("After sending view request to model");
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