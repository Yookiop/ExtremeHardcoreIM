package com.extremehcim;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.PlayerSpawned;
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
import java.util.*;
import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
		name = "Glassman",
		description = "Play the game as a Glassman (One Health Point / Nightmare mode)",
		tags = {"glass", "man", "1", "hp", "nightmare", "mode", "damage", "hit", "health", "heart", "fragile"}
)

public class GlassmanPlugin extends Plugin
{
	@Inject	private Client client;

	@Inject	private ConfigManager configManager;

	private boolean playerIsFragile = true;
	private final String GLASSMANCONFIGGROUP = "GLASSMAN";
	private final String GLASSMANVALID = "VALID";

	private final Set<WorldArea> tutorialIslandWorldArea = ImmutableSet.of(
			new WorldArea(3053, 3072, 103, 64, 0),
			new WorldArea(3059, 3051, 77, 21, 0),
			new WorldArea(3072, 9493, 45, 41, 0)
	);

	@Override
	protected void shutDown() throws Exception
	{
		restoreGame(false);
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned p)
	{
		if (p.getPlayer() == client.getLocalPlayer())
		{
			if (locationIsOnTutorialIsland(client.getLocalPlayer().getWorldLocation()))
			{
				if (getCombatExperience() == 0) {
					overrideSprites();
					sendGamemodeMessage("You begin to feel fragile... as though with just one hit your journey" +
							" will be over and your heart will shatter. How far will you get?",	Color.MAGENTA);
					playerIsFragile = true;
					setPlayerConfig(GLASSMANVALID,Boolean.toString(playerIsFragile));
				}
				else
				{
					sendGamemodeMessage("You have previously entered combat and are ineligible to be a Glassman.",
							Color.RED);
					restoreGame(true);
				}
			}
			else
			{
				playerIsFragile = isPlayerFragile();
				if (!playerIsFragile)
				{
					sendGamemodeMessage("You are not eligible to be a Glassman.", Color.RED);
					restoreGame(true);
				}
				else
				{
					overrideSprites();
				}
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick t) {
		if (playerIsFragile)
		{
			setHPOrbText(1);
			setHPStatText(1,1);
			setHPListeners(false);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied damage)
	{
		if (damage.getActor() != client.getLocalPlayer()) {return;}

		if (damage.getHitsplat().getAmount() > 0 && playerIsFragile)
		{
			restoreGame(true);

			sendGamemodeMessage("And with one blow, your fragile heart shatters. Having taken damage, you are no" +
					" longer worthy of Glassman status...", Color.RED);
		}
	}

	private long getCombatExperience()
	{
		return client.getSkillExperience(Skill.ATTACK) + client.getSkillExperience(Skill.STRENGTH) +
				client.getSkillExperience(Skill.DEFENCE) + client.getSkillExperience(Skill.MAGIC) +
				client.getSkillExperience(Skill.RANGED);
	}

	private boolean isPlayerFragile()
	{
		String playerString = getPlayerConfig(GLASSMANVALID);
		if (playerString == null) {return false;}
		return Boolean.parseBoolean(playerString);
	}

	private void removePlayerFromFragileMode()
	{
		setPlayerConfig(GLASSMANVALID,false);
	}

	private void setPlayerConfig(String key, Object value) {
		if (value != null) {
			configManager.setRSProfileConfiguration(GLASSMANCONFIGGROUP, key, value);
		}
		else {
			configManager.unsetRSProfileConfiguration(GLASSMANCONFIGGROUP, key);
		}
	}

	private String getPlayerConfig(String key) {
		return configManager.getRSProfileConfiguration(GLASSMANCONFIGGROUP, key);
	}

	private void sendGamemodeMessage(String msg, Color formatColor)	{
		String message = ColorUtil.wrapWithColorTag(String.format(msg),formatColor);
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}

	private boolean locationIsOnTutorialIsland(WorldPoint playerLocation) {
		for (WorldArea worldArea : tutorialIslandWorldArea) {
			if (worldArea.contains2D(playerLocation)) {
				return true;
			}
		}
		return false;
	}

	private void restoreGame(boolean removePlayerFromGamemode) {
		playerIsFragile = false;
		if (removePlayerFromGamemode) {removePlayerFromFragileMode();}
		setHPListeners(true);
		setHPOrbText(client.getBoostedSkillLevel(Skill.HITPOINTS));
		setHPStatText(client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS));
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
			HPStatWidgetComponents[3].setText(Integer.toString(topStatLevel));
			HPStatWidgetComponents[4].setText(Integer.toString(bottomStatLevel));
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