package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ModelMessagesEnum;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ModelUpdate;

public class ModelIncomingManagerRunnable implements Runnable {
	private Queue<ModelUpdate> modelUpdates;
	private Socket socketWithModel;
	private ObjectInputStream fromModel;
	private boolean allRight;
	
	public ModelIncomingManagerRunnable(Socket socketWithModel, ObjectInputStream fromModel, Queue<ModelUpdate> modelUpdates) throws IOException {
		this.socketWithModel = socketWithModel;
		this.fromModel = fromModel;
		this.modelUpdates = modelUpdates;
		this.allRight = true;
	}
	
	@Override
	public void run() {
		while(this.allRight) {
			getAndStoreModelUpdate();
		}
	}
	
	private void getAndStoreModelUpdate() {
		try {
			ModelUpdate update = (ModelUpdate) this.fromModel.readObject();
			
			if(!checkForTerminationSignal(update)) {
				// maybe here the controller can inspect and pre-process the update (in the future / should the need arise).
				this.modelUpdates.add((ModelUpdate) update);
				Utils.log(Utils.LOGS_PATH + "session.txt", "added model update to queue.");
			}
			else {
				manageStopSignalFromModel(update);
			}
		}
		catch(ClassNotFoundException e) {
			Utils.log(e);
			Utils.log(Utils.LOGS_PATH + "session.txt", "could not decode class of model update.");
		}
		catch(IOException e) {
			this.allRight = false;
			Utils.log(e);
			Utils.log(Utils.LOGS_PATH + "session.txt", "generic exception in reading model update.");
			closeSocketWithModel();
		}
	}

	private boolean checkForTerminationSignal(ModelUpdate update) {
		return ModelMessagesEnum.STOP_CONTROLLER.equals(update.getCode()) || ModelMessagesEnum.STOP_FORWARD.equals(update.getCode());
	}

	private void manageStopSignalFromModel(ModelUpdate update) {
		switch(update.getCode()) {
		case STOP_FORWARD:
			this.modelUpdates.add(update);
			//and fall through
		case STOP_CONTROLLER :
			System.exit(0);
			break;
		default:
			return;
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