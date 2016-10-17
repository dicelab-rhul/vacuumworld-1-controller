package uk.ac.rhul.cs.dice.vacuumworld.controller.utils;

import java.io.FileReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class ConfigData {
	private static String modelIp;
	private static int modelPort;
	private static String controllerIp;
	private static int controllerPort;
	private static String logsPath;
	private static int timeoutInSeconds;
	
	private ConfigData() {}

	public static String getModelIp() {
		return ConfigData.modelIp;
	}

	public static int getModelPort() {
		return ConfigData.modelPort;
	}

	public static String getControllerIp() {
		return ConfigData.controllerIp;
	}

	public static int getControllerPort() {
		return ConfigData.controllerPort;
	}

	public static String getLogsPath() {
		return ConfigData.logsPath;
	}
	
	public static int getTimeoutInSeconds() {
		return ConfigData.timeoutInSeconds;
	}
	
	public static boolean initConfigData(String configFilePath) {
		try(JsonReader reader = Json.createReader(new FileReader(configFilePath))) {
			return initData(reader);
		}
		catch(Exception e) {
			Utils.log(e);
			
			return false;
		}
	}
	
	private static boolean initData(JsonReader reader) {
		JsonObject config = reader.readObject();
		
		ConfigData.modelIp = config.getString("model_ip");
		ConfigData.modelPort = config.getInt("model_port");
		ConfigData.controllerIp = config.getString("controller_ip");
		ConfigData.controllerPort = config.getInt("controller_port");
		ConfigData.logsPath = config.getString("logs_path");
		ConfigData.timeoutInSeconds = config.getInt("timeout_in_seconds");
		
		return true;
	}
}