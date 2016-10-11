package uk.ac.rhul.cs.dice.vacuumworld.controller.utils;

public class StopSignal {
	private boolean signal;
	
	public StopSignal(){
		this.signal = false;
	}
	
	public void stop() {
		this.signal = true;
	}
	
	public boolean mustStop() {
		return this.signal;
	}
}