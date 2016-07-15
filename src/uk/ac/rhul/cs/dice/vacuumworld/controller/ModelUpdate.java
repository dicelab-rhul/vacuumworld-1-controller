package uk.ac.rhul.cs.dice.vacuumworld.controller;

import java.io.Serializable;

public class ModelUpdate implements Serializable {
	private static final long serialVersionUID = -6938956664743713596L;
	private Serializable payload;
	
	public ModelUpdate(Serializable payload) {
		this.payload = payload;
	}

	public Object getPayload() {
		return this.payload;
	}
}