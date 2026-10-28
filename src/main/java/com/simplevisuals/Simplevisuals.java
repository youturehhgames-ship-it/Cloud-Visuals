package com.simplevisuals;

import com.simplevisuals.command.ModCommands;
import com.simplevisuals.config.SimplevisualsConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simplevisuals implements ModInitializer {
	public static final String MOD_ID = "simplevisuals";
    private static SimplevisualsConfig CONFIG;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        LOGGER.info("Initializing Simplevisuals mod...");

        AutoConfig.register(SimplevisualsConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(SimplevisualsConfig.class).getConfig();

        ModCommands.register();

    }

    public static SimplevisualsConfig getConfig() { return CONFIG; }
}

