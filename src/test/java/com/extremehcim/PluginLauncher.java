package com.extremehcim;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		System.setProperty("runelite.development", "true");
		System.setProperty("runelite.dev.mode", "true");

		try {
			ExternalPluginManager.loadBuiltin(XtremeHCIMPlugin.class);
			System.out.println("Plugin loaded successfully!");
		} catch (Exception e) {
			System.err.println("Failed to load plugin: " + e.getMessage());
			e.printStackTrace();
		}

		RuneLite.main(args);
	}
}