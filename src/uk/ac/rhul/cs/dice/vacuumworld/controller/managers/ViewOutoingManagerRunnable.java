package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.view.ModelUpdate;

public class ViewOutoingManagerRunnable implements Runnable {
	private Queue<ModelUpdate> modelUpdates;
	private Socket socketWithView;
	private ObjectOutputStream toView;
	private boolean allRight;
	
	public ViewOutoingManagerRunnable(Socket socketWithView, ObjectOutputStream toView, Queue<ModelUpdate> modelUpdates) throws IOException {
		this.socketWithView = socketWithView;
		this.toView = toView;
		this.modelUpdates = modelUpdates;
		this.allRight = true;
	}
	
	@Override
	public void run() {
		while(this.allRight) {
			if(!this.modelUpdates.isEmpty()) {
				forwardModelUpdate();
			}
		}
	}

	private void forwardModelUpdate() {
		try {
			ModelUpdate update = this.modelUpdates.poll();
			// maybe here the controller can inspect and pre-process the update (in the future / should the need arise).
			this.toView.writeObject(update);
			this.toView.flush();
		}
		catch(IOException e) {
			this.allRight = false;
			Utils.log(e);
			closeSocketWithView();
		}
		catch(Exception e) {
			Utils.log(e);
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
}