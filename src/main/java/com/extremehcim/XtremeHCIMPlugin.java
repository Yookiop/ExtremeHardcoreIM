package com.extremehcim;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
		name = "ExtremeHCIM",
		description = "Play the game with 1 true life and there are no safe deaths",
		tags = {"HCIM", "hp", "nightmare", "mode", "damage", "hit", "health", "extreme", "fragile"},
		enabledByDefault = false,
		conflicts = {}
)
public class XtremeHCIMPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private XtremeHCIMConfig config;

	private boolean playerIsFragile = false;
	private final String GLASSMANCONFIGGROUP = "GLASSMAN";
	private final String GLASSMANVALID = "VALID";

	@Override
	protected void startUp() throws Exception
	{
		log.info("ExtremeHCIM plugin started");
		System.out.println("ExtremeHCIM plugin starting up!"); // Debug print

		// Check if plugin is enabled in config
		if (config.enableXtremeHCIM())
		{
			activateXtremeHCIMMode();
		}
		else
		{
			sendGamemodeMessage("ExtremeHCIM plugin loaded. Enable it in the config to activate!", Color.YELLOW);
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("ExtremeHCIM plugin stopped");
		restoreGame(false);
	}

	// Rest of your methods remain the same...
	@Subscribe
	public void onGameTick(GameTick t)
	{
		// Check config state every tick and sync
		boolean configEnabled = config.enableXtremeHCIM();

		if (configEnabled && !playerIsFragile)
		{
			// Config enabled but mode not active - activate it
			if (client.getBoostedSkillLevel(Skill.HITPOINTS) > 1)
			{
				activateXtremeHCIMMode();
			}
			else
			{
				sendGamemodeMessage("You need more than 1 HP to activate Xtreme HCIM mode safely!", Color.RED);
			}
		}
		else if (!configEnabled && playerIsFragile)
		{
			// Config disabled but mode is active - deactivate it
			restoreGame(true);
			sendGamemodeMessage("Xtreme HCIM mode has been deactivated.", Color.YELLOW);
		}

		if (playerIsFragile)
		{
			int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);
			setHPOrbText(currentHP);
			setHPStatText(currentHP, currentHP);
			setHPListeners(false);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied damage)
	{
		if (damage.getActor() != client.getLocalPlayer()) {return;}

		// Check if player has 0 HP after taking damage
		if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= 0 && playerIsFragile)
		{
			restoreGame(true);
			sendGamemodeMessage("You've reached 0 HP and lost your Xtreme HCIM status", Color.RED);
		}
		else if (playerIsFragile && config.showWarnings() && damage.getHitsplat().getAmount() > 0)
		{
			int currentHP = client.getBoostedSkillLevel(Skill.HITPOINTS);
			if (currentHP <= 5)
			{
				sendGamemodeMessage("⚠️ WARNING: Only " + currentHP + " HP remaining!", Color.RED);
			}
		}
	}

	private void activateXtremeHCIMMode()
	{
		if (playerIsFragile)
		{
			sendGamemodeMessage("Xtreme HCIM mode is already active!", Color.ORANGE);
			return;
		}

		overrideSprites();
		sendGamemodeMessage("Xtreme HCIM mode activated! You have one life and there are no safe deaths. How far will you get?", Color.MAGENTA);
		playerIsFragile = true;
		setPlayerConfig(GLASSMANVALID, Boolean.toString(playerIsFragile));
	}

	private boolean isPlayerFragile()
	{
		String playerString = getPlayerConfig(GLASSMANVALID);
		if (playerString == null) {return false;}
		return Boolean.parseBoolean(playerString);
	}

	private void removePlayerFromFragileMode()
	{
		setPlayerConfig(GLASSMANVALID, "false");
	}

	private void setPlayerConfig(String key, Object value)
	{
		if (value != null) {
			configManager.setRSProfileConfiguration(GLASSMANCONFIGGROUP, key, value);
		}
		else {
			configManager.unsetRSProfileConfiguration(GLASSMANCONFIGGROUP, key);
		}
	}

	private String getPlayerConfig(String key)
	{
		return configManager.getRSProfileConfiguration(GLASSMANCONFIGGROUP, key);
	}

	private void sendGamemodeMessage(String msg, Color formatColor)
	{
		String message = ColorUtil.wrapWithColorTag(String.format(msg), formatColor);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}

	private void restoreGame(boolean removePlayerFromGamemode)
	{
		playerIsFragile = false;
		if (removePlayerFromGamemode) {removePlayerFromFragileMode();}
		setHPListeners(true);
		if (client.getLocalPlayer() != null)
		{
			setHPOrbText(client.getBoostedSkillLevel(Skill.HITPOINTS));
			setHPStatText(client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS));
		}
		restoreSprites();
	}

	private void setHPListeners(boolean setListeners)
	{
		Widget HPWidget = client.getWidget(InterfaceID.Stats.HITPOINTS);
		if (HPWidget != null) {
			HPWidget.setHasListener(setListeners);
		}
		HPWidget = client.getWidget(InterfaceID.Orbs.ORB_HEALTH);
		if (HPWidget != null) {
			HPWidget.setHasListener(setListeners);
		}
	}

	private void setHPOrbText(int levelToDisplay)
	{
		Widget HPTextWidget = client.getWidget(InterfaceID.Orbs.HEALTH_TEXT);
		if (HPTextWidget != null)
		{
			HPTextWidget.setText(Integer.toString(levelToDisplay));
		}
	}

	private void setHPStatText(int topStatLevel, int bottomStatLevel)
	{
		Widget HPStatWidget = client.getWidget(InterfaceID.Stats.HITPOINTS);
		if (HPStatWidget != null) {
			Widget[] HPStatWidgetComponents = HPStatWidget.getDynamicChildren();
			if (HPStatWidgetComponents != null && HPStatWidgetComponents.length > 4) {
				HPStatWidgetComponents[3].setText(Integer.toString(topStatLevel));
				HPStatWidgetComponents[4].setText(Integer.toString(bottomStatLevel));
			}
		}
	}

	private void overrideSprites()
	{
		for (SpriteOverride spriteOverride : SpriteOverride.values())
		{
			try
			{
				final String spriteFile = "/" + spriteOverride.getSpriteID() + ".png";
				BufferedImage spriteImage = ImageUtil.getResourceStreamFromClass(getClass(), spriteFile);
				SpritePixels spritePixels = ImageUtil.getImageSpritePixels(spriteImage, client);
				client.getSpriteOverrides().put(spriteOverride.getSpriteID(), spritePixels);
			}
			catch (RuntimeException ex)
			{
				log.debug("Unable to load sprite: ", ex);
			}
		}
		resetGraphics();
	}

	private void restoreSprites()
	{
		client.getWidgetSpriteCache().reset();
		for (SpriteOverride spriteOverride : SpriteOverride.values())
		{
			client.getSpriteOverrides().remove(spriteOverride.getSpriteID());
		}
		resetGraphics();
	}

	private void resetGraphics()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			client.setGameState(GameState.LOADING);
		}
	}
}