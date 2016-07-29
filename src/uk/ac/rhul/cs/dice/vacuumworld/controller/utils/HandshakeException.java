package uk.ac.rhul.cs.dice.vacuumworld.controller.utils;

public class HandshakeException extends Exception {
	private static final long serialVersionUID = -1531185904535571737L;

	public HandshakeException(Exception e) {
		this.initCause(e);
	}
	
	public HandshakeException(String message) {
		super(message);
	}
}