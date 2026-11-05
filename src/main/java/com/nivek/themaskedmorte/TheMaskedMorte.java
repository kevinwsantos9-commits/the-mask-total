package com.nivek.themaskedmorte;

import com.nivek.themaskedmorte.net.MorphManager;
import net.fabricmc.api.ModInitializer;

public class TheMaskedMorte implements ModInitializer {
    public static final String MOD_ID = "themaskedmorte";

    @Override
    public void onInitialize() {
        ModItems.init();
        MorphManager.init();
    }
}
