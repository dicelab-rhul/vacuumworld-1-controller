package uk.ac.rhul.cs.dice.vacuumworld.controller.managers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;

import uk.ac.rhul.cs.dice.vacuumworld.controller.utils.Utils;
import uk.ac.rhul.cs.dice.vacuumworld.wvcommon.ViewRequest;

public class ViewIncomingManagerRunnable implements Runnable {
	private Queue<ViewRequest> viewRequests;
	private Socket socketWithView;
	private ObjectInputStream fromView;
	private boolean allRight;
	
	public ViewIncomingManagerRunnable(Socket socketWithView, ObjectInputStream fromView, Queue<ViewRequest> viewRequests) throws IOException {
		this.socketWithView = socketWithView;
		this.fromView = fromView;
		this.viewRequests = viewRequests;
		this.allRight = true;
	}
	
	@Override
	public void run() {
		while(this.allRight) {
			getAndStoreViewRequest();
		}
	}
	
	private void getAndStoreViewRequest() {
		try {
			Object request = this.fromView.readObject();
			
			if(request instanceof ViewRequest) {
				// maybe here the controller can inspect and pre-process the request (in the future / should the need arise).
				this.viewRequests.add((ViewRequest) request);
			}
		}
		catch(ClassNotFoundException e) {
			Utils.log(e);
		}
		catch(IOException e) {
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
}