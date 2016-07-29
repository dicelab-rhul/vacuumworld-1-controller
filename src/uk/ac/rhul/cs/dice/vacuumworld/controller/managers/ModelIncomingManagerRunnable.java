package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.ModelUpdate;
import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;

public class ModelIncomingManagerRunnable implements Runnable {
	private Queue<ModelUpdate> modelUpdates;
	private Socket socketWithModel;
	private ObjectInputStream fromModel;
	private boolean allRight;
	
	public ModelIncomingManagerRunnable(Socket socketWithModel, Queue<ModelUpdate> modelUpdates) throws IOException {
		this.socketWithModel = socketWithModel;
		this.fromModel = new ObjectInputStream(this.socketWithModel.getInputStream());
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
			Object update = this.fromModel.readObject();
			
			if(update instanceof ModelUpdate) {
				// maybe here the controller can inspect and pre-process the update (in the future / should the need arise).
				this.modelUpdates.add((ModelUpdate) update);
			}
		}
		catch(ClassNotFoundException e) {
			Utils.log(e);
		}
		catch(IOException e) {
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
}