package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ModelMessagesEnum;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ModelUpdate;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.StopSignal;

public class ModelIncomingManagerRunnable implements Runnable {
	private volatile StopSignal sharedStopSignal;
	private Queue<ModelUpdate> modelUpdates;
	private Socket socketWithModel;
	private ObjectInputStream fromModel;
	private boolean allRight;
	
	public ModelIncomingManagerRunnable(StopSignal sharedStopSignal, Socket socketWithModel, ObjectInputStream fromModel, Queue<ModelUpdate> modelUpdates) throws IOException {
		this.sharedStopSignal = sharedStopSignal;
		this.socketWithModel = socketWithModel;
		this.fromModel = fromModel;
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
			
			getAndStoreModelUpdate();
		}
	}
	
	private void getAndStoreModelUpdate() {
		try {
			Utils.logWithClass(this.getClass().getSimpleName(), "Waiting for model update for view from model...");
			Object update = safeRead();
			
			if(update == null || this.sharedStopSignal.mustStop()) {
				return;
			}
			
			Utils.logWithClass(this.getClass().getSimpleName(), "Received model update for view from model.");
			
			if(!checkForTerminationSignal((ModelUpdate) update)) {
				// maybe here the controller can inspect and pre-process the update (in the future / should the need arise).
				
				Utils.logWithClass(this.getClass().getSimpleName(), "Adding model update for view to the queue...");
				this.modelUpdates.add((ModelUpdate) update);
				Utils.logWithClass(this.getClass().getSimpleName(), "Added model update for view to the queue.");
			}
			else {
				manageStopSignalFromModel((ModelUpdate) update);
			}
		}
		catch(Exception e) {
			manageException(e);
		}
	}

	private boolean checkForTerminationSignal(ModelUpdate update) {
		return ModelMessagesEnum.STOP_CONTROLLER.equals(update.getCode()) || ModelMessagesEnum.STOP_FORWARD.equals(update.getCode());
	}

	private void manageStopSignalFromModel(ModelUpdate update) {
		Utils.logWithClass(this.getClass().getSimpleName(), "Received stop signal from model.");
		
		switch(update.getCode()) {
		case STOP_FORWARD:
			this.modelUpdates.add(update);
			this.sharedStopSignal.stop();
			break;
		case STOP_CONTROLLER :
		default:
			this.sharedStopSignal.stop();
			break;
		}
	}

	private void closeSocketWithModel() {
		try {
			this.socketWithModel.close();
		}
		catch (IOException e) {
			Utils.fakeLog(e);
		}
	}
	
	private Object safeRead() throws ClassNotFoundException, IOException {
		return this.fromModel.readObject();
	}
	
	private void manageException(Exception e) {
		if(this.sharedStopSignal.mustStop()) {
			return;
		}
		else {
			this.allRight = false;
			Utils.log(e);
			closeSocketWithModel();
		}
	}
}