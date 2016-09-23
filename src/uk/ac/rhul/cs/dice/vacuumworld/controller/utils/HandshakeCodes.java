package uk.ac.rhul.cs.dice.vacuumworld.controller.utils;

public enum HandshakeCodes {
	VHVC,
	CHCV,
	CHCM,
	CHVM,
	MHMC,
	MHMV,
	CHMV;
	
	public static HandshakeCodes fromString(String s) {
		switch(s) {
		case "VHVC":
			return VHVC;
		case "CHCV":
			return CHCV;
		case "CHCM":
			return CHCM;
		case "CHVM":
			return CHVM;
		case "MHMC":
			return MHMC;
		case "MHMV":
			return MHMV;
		case "CHMV":
			return CHMV;
		default:
			return null;
		}
	}
	
	@Override
	public String toString() {
		switch(this) {
		case VHVC:
			return "VHVC";
		case CHCV:
			return "CHCV";
		case CHCM:
			return "CHCM";
		case CHVM:
			return "CHVM";
		case MHMC:
			return "MHMC";
		case MHMV:
			return "MHMV";
		case CHMV:
			return "CHMV";
		default:
			throw new IllegalArgumentException();
		}
	}
}