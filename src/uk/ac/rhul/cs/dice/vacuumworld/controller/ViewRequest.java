package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.Serializable;

public class ViewRequest implements Serializable {
	private static final long serialVersionUID = -7584947665906439069L;
	private ViewRequestsEnum code;
	private Serializable payload;
	
	public ViewRequest(ViewRequestsEnum code, Serializable payload) {
		this.code = code;
		this.payload = payload;
	}

	public ViewRequestsEnum getCode() {
		return this.code;
	}

	public Object getPayload() {
		return this.payload;
	}
}