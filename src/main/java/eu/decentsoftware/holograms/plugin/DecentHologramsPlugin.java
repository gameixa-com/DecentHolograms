package eu.decentsoftware.holograms.plugin;

import de.tr7zw.changeme.nbtapi.NBT;
import eu.decentsoftware.holograms.api.DecentHolograms;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.decentsoftware.holograms.api.commands.CommandManager;
import eu.decentsoftware.holograms.api.commands.DecentCommand;
import eu.decentsoftware.holograms.api.utils.reflect.Version;
import eu.decentsoftware.holograms.plugin.commands.HologramsCommand;
import eu.decentsoftware.holograms.plugin.features.DamageDisplayFeature;
import eu.decentsoftware.holograms.plugin.features.HealingDisplayFeature;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class DecentHologramsPlugin extends JavaPlugin {

	private boolean unsupportedServerVersion = false;
	private static final String API_URL = "https://team.sofixa.com/app/api/license.php?product=1&ip=";


	@Override
	public void onLoad() {
		if (Version.CURRENT == null) {
			unsupportedServerVersion = true;
			return;
		}

		DecentHologramsAPI.onLoad(this);
	}

	@Override
	public void onEnable() {
		if (unsupportedServerVersion) {
			getLogger().severe("Unsupported server version detected: " + Bukkit.getServer().getVersion());
			getLogger().severe("Plugin will now be disabled.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		DecentHologramsAPI.onEnable();

		DecentHolograms decentHolograms = DecentHologramsAPI.get();
		decentHolograms.getFeatureManager().registerFeature(new DamageDisplayFeature());
		decentHolograms.getFeatureManager().registerFeature(new HealingDisplayFeature());

		CommandManager commandManager = decentHolograms.getCommandManager();
		DecentCommand mainCommand = new HologramsCommand();
		commandManager.setMainCommand(mainCommand);
		commandManager.registerCommand(mainCommand);
		
		// Enable NBT API to avoid lag spikes when parsing NBT for the first time.
		NBT.preloadApi();
		licenseCheck();
	}

	@Override
	public void onDisable() {
		if (unsupportedServerVersion) {
			return;
		}

		DecentHologramsAPI.onDisable();
	}

	private void licenseCheck() {
		try {
			String localIPAddress = getLocalIPAddress();
			if (localIPAddress == null) {
				System.out.println("IP Adresi belirlenemedi.");
				return;
			}
			JSONObject response = sendGET("https://team.sofixa.com/app/api/license.php?product=1&ip=" + localIPAddress);
			Boolean status = (Boolean)response.get("status");
			if (status != null && status.booleanValue()) {
				Bukkit.getConsoleSender().sendMessage("Lisans bulundu!");
				Bukkit.getConsoleSender().sendMessage("Plugin Aktif!");
			} else {
				Bukkit.getConsoleSender().sendMessage("Lisans bulunamadi. Plugin kapatiliyor...");
				Bukkit.shutdown();
			}
		} catch (IOException|ParseException e) {
			e.printStackTrace();
		}
	}

	private String getLocalIPAddress() throws SocketException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface iface = interfaces.nextElement();
			if (iface.isLoopback() || !iface.isUp())
				continue;
			Enumeration<InetAddress> addresses = iface.getInetAddresses();
			while (addresses.hasMoreElements()) {
				InetAddress addr = addresses.nextElement();
				if (addr.isLinkLocalAddress() || addr.isLoopbackAddress() || addr.isMulticastAddress())
					continue;
				return addr.getHostAddress();
			}
		}
		return null;
	}

	private JSONObject sendGET(String url) throws IOException, ParseException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection)obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		int responseCode = con.getResponseCode();
		if (responseCode == 200) {
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuilder response = new StringBuilder();
			String inputLine;
			while ((inputLine = in.readLine()) != null)
				response.append(inputLine);
			in.close();
			JSONParser parser = new JSONParser();
			return (JSONObject)parser.parse(response.toString());
		}
		throw new IOException("Response code: " + responseCode);
	}
}
