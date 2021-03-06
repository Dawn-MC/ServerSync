package com.superzanti.serversync;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.superzanti.serversync.util.MCConfigReader.MCCCategory;
import com.superzanti.serversync.util.MCConfigReader.MCCConfig;
import com.superzanti.serversync.util.MCConfigReader.MCCElement;
import com.superzanti.serversync.util.MCConfigReader.MCCReader;
import com.superzanti.serversync.util.MCConfigReader.MCCWriter;
import com.superzanti.serversync.util.enums.EConfigDefaults;
import com.superzanti.serversync.util.enums.EConfigType;

final class ConfigDefaults extends HashMap<EConfigDefaults, String> {
	private static final long serialVersionUID = 71158792045085436L;
	
	public ConfigDefaults() {
		this.put(EConfigDefaults.SERVER_IP, "127.0.0.1");
		this.put(EConfigDefaults.SERVER_PORT, "38067");
		this.put(EConfigDefaults.LAST_UPDATE, "");
		this.put(EConfigDefaults.PUSH_CLIENT_MODS, "false");
		this.put(EConfigDefaults.REFUSE_CLIENT_MODS, "false");
	}
}

/**
 * Handles all functionality to do with serversyncs config file and
 * other configuration properties
 * @author Rheimus
 *
 */
public class SyncConfig {
	private static final String CONFIG_LOCATION = "config" + File.separator + "serversync";
	private static final HashMap<EConfigDefaults, String> defaults = new ConfigDefaults();
	private static final String CATEGORY_GENERAL = "general";
	private static final String CATEGORY_RULES = "rules";
	private static final String CATEGORY_CONNECTION = "serverconnection";
	private static final String CATEGORY_OTHER = "misc";
	
	private MCCConfig config;
	
	private Path configPath;
	public final EConfigType configType;
	// COMMON //////////////////////////////
	public String SERVER_IP;
	public String LAST_UPDATE;
	public List<String> FILE_IGNORE_LIST;
	public List<String> CONFIG_INCLUDE_LIST;
	public Locale LOCALE;
	////////////////////////////////////////
	
	// SERVER //////////////////////////////
	public int SERVER_PORT;
	public Boolean PUSH_CLIENT_MODS;
	public List<String> DIRECTORY_INCLUDE_LIST;
	////////////////////////////////////////
	
	// CLIENT //////////////////////////////
	public Boolean REFUSE_CLIENT_MODS = false;
	////////////////////////////////////////
	
	public static boolean pullServerConfig = true;
	
	public SyncConfig(EConfigType type) {
		configType = type;
		config = new MCCConfig();
		if (configType == EConfigType.SERVER) {			
			configPath = Paths.get(CONFIG_LOCATION + File.separator + "serversync-server.cfg");
		} else {
			configPath = Paths.get(CONFIG_LOCATION + File.separator + "serversync-client.cfg");
		}
		
		if (!Files.exists(configPath.getParent())) {
			try {
				Files.createDirectories(configPath.getParent());
			} catch (IOException e) {
				System.out.println("Failed to create directories for: " + configPath.toString());
			}
		}
		
		if (!Files.exists(configPath)) {			
			createConfiguraton();
		} else {			
			readExistingConfiguration();
		}
		init();
	}
	
	private void readExistingConfiguration() {
		try {
			config.readConfig(new MCCReader(Files.newBufferedReader(configPath)));
		} catch (IOException e) {
			System.out.println("Failed to read config file: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private boolean createConfiguraton() {
		try {
			Files.createFile(configPath);
		} catch (IOException e) {
			System.out.println("Failed to create config file: " + e.getMessage());
			return false;
		}
		
		if (configType == EConfigType.SERVER) {
			SERVER_PORT = Integer.parseInt(defaults.get(EConfigDefaults.SERVER_PORT));
			PUSH_CLIENT_MODS = Boolean.parseBoolean(defaults.get(EConfigDefaults.PUSH_CLIENT_MODS));
			LAST_UPDATE = defaults.get(EConfigDefaults.LAST_UPDATE);
			
				ArrayList<String> comments = new ArrayList<String>();
				ArrayList<String> defaultValueList = new ArrayList<>();
				
				MCCCategory general = new MCCCategory(SyncConfig.CATEGORY_GENERAL);
					comments.add("# set true to push client side mods from clientmods directory, set on server [default: false]");
					general.add(new MCCElement(SyncConfig.CATEGORY_GENERAL, "B", "PUSH_CLIENT_MODS", "false", comments));
					comments.clear();
				
				MCCCategory rules = new MCCCategory(SyncConfig.CATEGORY_RULES);
					comments.add("# These configs are included, by default configs are not synced");
					rules.add(new MCCElement(SyncConfig.CATEGORY_RULES, "S", "CONFIG_INCLUDE_LIST", new ArrayList<String>(), comments));
					comments.clear();

					defaultValueList.add("mods");
					comments.add("# These directories are included, by default mods and configs are included");
					rules.add(new MCCElement(SyncConfig.CATEGORY_RULES, "S", "DIRECTORY_INCLUDE_LIST", new ArrayList<String>(defaultValueList), comments));
					comments.clear();
					defaultValueList.clear();
				
					comments.add("# These files are ignored by serversync, list auto updates with mods added to the clientmods directory");
					rules.add(new MCCElement(SyncConfig.CATEGORY_RULES, "S", "FILE_IGNORE_LIST", new ArrayList<String>(), comments));
					comments.clear();
				
				MCCCategory serverConnection = new MCCCategory(SyncConfig.CATEGORY_CONNECTION);
					comments.add("# The port that your server will be serving on [range: 1 ~ 49151, default: 38067]");
					serverConnection.add(new MCCElement(SyncConfig.CATEGORY_CONNECTION, "I", "SERVER_PORT", "38067", comments));
					comments.clear();
					
				MCCCategory other = new MCCCategory(SyncConfig.CATEGORY_OTHER);
					comments.add("# Your locale string");
					other.add(new MCCElement(SyncConfig.CATEGORY_OTHER, "S", "LOCALE", Locale.getDefault().toString(), comments));
					comments.clear();
				
				config.put(SyncConfig.CATEGORY_GENERAL, general);
				config.put(SyncConfig.CATEGORY_RULES, rules);
				config.put(SyncConfig.CATEGORY_CONNECTION, serverConnection);
				config.put(SyncConfig.CATEGORY_OTHER, other);
				
				try {
					config.writeConfig(new MCCWriter(Files.newBufferedWriter(configPath)));
				} catch (IOException e) {
					System.out.println("Failed to write server config file: " + e.getMessage());
					e.printStackTrace();
				}
			
		} else {
			// Client config
			ArrayList<String> comments = new ArrayList<String>();
			
			MCCCategory general = new MCCCategory(SyncConfig.CATEGORY_GENERAL);
				comments.add("Set this to true to refuse client mods pushed by the server, [default: false]");
				general.add(new MCCElement(SyncConfig.CATEGORY_GENERAL, "B", "REFUSE_CLIENT_MODS", "false", comments));
				comments.clear();

			MCCCategory rules = new MCCCategory(SyncConfig.CATEGORY_RULES);
				comments.add("These configs are included, by default configs are not synced.");
				rules.add(new MCCElement(SyncConfig.CATEGORY_RULES, "S", "CONFIG_INCLUDE_LIST", new ArrayList<String>(), comments));
				comments.clear();
				
				comments.add("These files are ignored by serversync, add your client mods here to stop serversync deleting them.");
				rules.add(new MCCElement(SyncConfig.CATEGORY_RULES, "S", "FILE_IGNORE_LIST", new ArrayList<String>(), comments));
				comments.clear();
			
			MCCCategory connection = new MCCCategory(SyncConfig.CATEGORY_CONNECTION);
				comments.add("The IP address of the server [default: 127.0.0.1]");
				connection.add(new MCCElement(SyncConfig.CATEGORY_CONNECTION, "S", "SERVER_IP", "127.0.0.1", comments));
				comments.clear();
				
				comments.add("The port that your server will be serving on [range: 1 ~ 49151, default: 38067]");
				connection.add(new MCCElement(SyncConfig.CATEGORY_CONNECTION, "I", "SERVER_PORT", "38067", comments));
				comments.clear();
				
			MCCCategory other = new MCCCategory(SyncConfig.CATEGORY_OTHER);
				comments.add("# Your locale string");
				other.add(new MCCElement(SyncConfig.CATEGORY_OTHER, "S", "LOCALE", Locale.getDefault().toString(), comments));
				comments.clear();
				
			config.put(SyncConfig.CATEGORY_GENERAL, general);
			config.put(SyncConfig.CATEGORY_RULES, rules);
			config.put(SyncConfig.CATEGORY_CONNECTION, connection);
			config.put(SyncConfig.CATEGORY_OTHER, other);
			
			try {
				config.writeConfig(new MCCWriter(Files.newBufferedWriter(configPath)));
			} catch (IOException e) {
				System.out.println("Failed to write client config file: " + e.getMessage());
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public boolean writeConfigUpdates() {
		try {
			config.writeConfig(new MCCWriter(Files.newBufferedWriter(configPath,StandardOpenOption.TRUNCATE_EXISTING)));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void init() {
		try {			
			LOCALE = new Locale(config.getEntryByName("LOCALE").getString());
			
			try {				
				FILE_IGNORE_LIST = config.getEntryByName("FILE_IGNORE_LIST").getList();
			} catch (NullPointerException e) {
				// Specific conversion from old config files
				FILE_IGNORE_LIST = config.getEntryByName("MOD_IGNORE_LIST").getList();
			}
			
			CONFIG_INCLUDE_LIST = config.getEntryByName("CONFIG_INCLUDE_LIST").getList();
			
			if (configType == EConfigType.SERVER) {				
				PUSH_CLIENT_MODS = config.getEntryByName("PUSH_CLIENT_MODS").getBoolean();
				DIRECTORY_INCLUDE_LIST = config.getEntryByName("DIRECTORY_INCLUDE_LIST").getList();
				SERVER_PORT = config.getEntryByName("SERVER_PORT").getInt();
			} else if (configType == EConfigType.CLIENT) {				
				SERVER_IP = config.getEntryByName("SERVER_IP").getString();
				SERVER_PORT = config.getEntryByName("SERVER_PORT").getInt();
				REFUSE_CLIENT_MODS = config.getEntryByName("REFUSE_CLIENT_MODS").getBoolean();
			}
		} catch(NullPointerException e) {
			System.out.println("could not retrieve an entry from the config file, have you altered the entry names?");
		}

		System.out.println("finished loading config");
	}
}
