package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.StopSignal;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ViewRequest;

public class ModelOutgoingManagerRunnable implements Runnable {
	private volatile StopSignal sharedStopSignal;
	private Queue<ViewRequest> viewRequests;
	private Socket socketWithModel;
	private ObjectOutputStream toModel;
	private boolean allRight;
	
	public ModelOutgoingManagerRunnable(StopSignal sharedStopSignal, Socket socketWithModel, ObjectOutputStream toModel, Queue<ViewRequest> viewRequests) throws IOException {
		this.sharedStopSignal = sharedStopSignal;
		this.socketWithModel = socketWithModel;
		this.toModel = toModel;
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
			
			if(!this.viewRequests.isEmpty()) {
				forwardViewRequest();
			}
		}
	}

	private void forwardViewRequest() {
		try {
			Utils.logWithClass(this.getClass().getSimpleName(), "Before polling view request for model...");
			ViewRequest request = safePoll();
			
			if(request == null || this.sharedStopSignal.mustStop()) {
				return;
			}
			
			Utils.logWithClass(this.getClass().getSimpleName(), "After polling view request for model.");
			
			// maybe here the controller can inspect and pre-process the request (in the future / should the need arise).
			
			Utils.logWithClass(this.getClass().getSimpleName(), "Before sending view request to model...");
			this.toModel.writeObject(request);
			this.toModel.flush();
			Utils.logWithClass(this.getClass().getSimpleName(), "After sending view request to model.");
		}
		catch(Exception e) {
			this.allRight = false;
			Utils.log(e);
			closeSocketWithModel();
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
	
	private ViewRequest safePoll() {
		while(!this.sharedStopSignal.mustStop()) {
			if(!this.viewRequests.isEmpty()) {
				return this.viewRequests.poll();
			}
		}
		
		return null;
	}
}