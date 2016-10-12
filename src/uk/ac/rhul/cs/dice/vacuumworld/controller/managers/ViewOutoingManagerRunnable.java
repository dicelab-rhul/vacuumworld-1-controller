package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ModelUpdate;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.StopSignal;

public class ViewOutoingManagerRunnable implements Runnable {
	private volatile StopSignal sharedStopSignal;
	private Queue<ModelUpdate> modelUpdates;
	private Socket socketWithView;
	private ObjectOutputStream toView;
	private boolean allRight;
	
	public ViewOutoingManagerRunnable(StopSignal sharedStopSignal, Socket socketWithView, ObjectOutputStream toView, Queue<ModelUpdate> modelUpdates) throws IOException {
		this.sharedStopSignal = sharedStopSignal;
		this.socketWithView = socketWithView;
		this.toView = toView;
		this.modelUpdates = modelUpdates;
		this.allRight = true;
	}
	
	@Override
	public void run() {
		while(this.allRight) {
			if(this.sharedStopSignal.mustStop()) {
				Utils.logWithClass(this.getClass().getSimpleName(), "Shared stop signal is true: quitting...");
				
				return;
			}
			
			if(!this.modelUpdates.isEmpty()) {
				forwardModelUpdate();
			}
		}
	}

	private void forwardModelUpdate() {
		try {
			Utils.logWithClass(this.getClass().getSimpleName(), "Before polling model update for view...");
			ModelUpdate update = safePoll();
			
			if(update == null || this.sharedStopSignal.mustStop()) {
				return;
			}
			
			Utils.logWithClass(this.getClass().getSimpleName(), "After polling model update for view.");
			
			// maybe here the controller can inspect and pre-process the update (in the future / should the need arise).
			
			Utils.logWithClass(this.getClass().getSimpleName(), "Before sending model update to view...");
			this.toView.writeObject(update);
			this.toView.flush();
			Utils.logWithClass(this.getClass().getSimpleName(), "After sending model update to view.\n");
		}
		catch(Exception e) {
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
			Utils.log(e);
		}
	}
	
	private ModelUpdate safePoll() {
		while(!this.sharedStopSignal.mustStop()) {
			if(!this.modelUpdates.isEmpty()) {
				return this.modelUpdates.poll();
			}
		}
		
		return null;
	}
}