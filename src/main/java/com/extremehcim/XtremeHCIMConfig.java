package com.extremehcim;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("extremehcim")
public interface XtremeHCIMConfig extends Config
{
    @ConfigItem(
            keyName = "enableXtremeHCIM",
            name = "Enable Xtreme HCIM",
            description = "Toggle Xtreme HCIM mode on/off (WARNING: No safe deaths!)"
    )
    default boolean enableXtremeHCIM()
    {
        return false;
    }

    @ConfigItem(
            keyName = "showWarnings",
            name = "Show Warnings",
            description = "Show warning messages when taking damage"
    )
    default boolean showWarnings()
    {
        return true;
    }
}