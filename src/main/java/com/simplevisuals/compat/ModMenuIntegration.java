package com.simplevisuals.compat;

import com.simplevisuals.config.SimplevisualsConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    @SuppressWarnings("deprecation") // AutoConfig ist sicher auf dem Client, Warnung ignorieren
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(SimplevisualsConfig.class, parent).get();
    }
}