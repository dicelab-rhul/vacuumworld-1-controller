package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.StopSignal;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ViewRequest;

public class ViewIncomingManagerRunnable implements Runnable {
	private volatile StopSignal sharedStopSignal;
	private Queue<ViewRequest> viewRequests;
	private Socket socketWithView;
	private ObjectInputStream fromView;
	private boolean allRight;
	
	public ViewIncomingManagerRunnable(StopSignal sharedStopSignal, Socket socketWithView, ObjectInputStream fromView, Queue<ViewRequest> viewRequests) throws IOException {
		this.sharedStopSignal = sharedStopSignal;
		this.socketWithView = socketWithView;
		this.fromView = fromView;
		this.viewRequests = viewRequests;
		this.allRight = true;
	}
	
	@Override
	public void run() {
		while(this.allRight) {
			if(this.sharedStopSignal.mustStop()) {
				Utils.logWithClass(this.getClass().getSimpleName(), "Shared stop signal is true: quitting...");
				
				return;
			}
			
			getAndStoreViewRequest();
		}
	}
	
	private void getAndStoreViewRequest() {
		try {
			Utils.logWithClass(this.getClass().getSimpleName(), "Waiting for view request for model from view...");
			Object request = safeRead();
			
			if(request == null || this.sharedStopSignal.mustStop()) {
				return;
			}
			
			Utils.logWithClass(this.getClass().getSimpleName(), "Received view request for model from view.");
			
			if(request instanceof ViewRequest) {
				// maybe here the controller can inspect and pre-process the request (in the future / should the need arise).
				
				Utils.logWithClass(this.getClass().getSimpleName(), "Adding view request for model to the queue...");
				this.viewRequests.add((ViewRequest) request);
				Utils.logWithClass(this.getClass().getSimpleName(), "Added view request for model to the queue.");
			}
		}
		catch(Exception e) {
			manageException(e);
		}
	}

	private void manageException(Exception e) {
		if(this.sharedStopSignal.mustStop()) {
			return;
		}
		else {
			this.allRight = false;
			Utils.log(e);
			closeSocketWithView();
		}
	}

	private void closeSocketWithView() {
		try {
			this.socketWithView.close();
		}
		catch (IOException e) {
			Utils.fakeLog(e);
		}
	}
	
	private Object safeRead() throws ClassNotFoundException, IOException {
		return this.fromView.readObject();
	}
}