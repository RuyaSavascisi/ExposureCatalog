package io.github.mortuusars.exposure_catalog.forge;

import io.github.mortuusars.exposure_catalog.Config;
import io.github.mortuusars.exposure_catalog.ExposureCatalog;
import io.github.mortuusars.exposure_catalog.forge.event.ClientEvents;
import io.github.mortuusars.exposure_catalog.forge.event.CommonEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ExposureCatalog.ID)
public class ExposureCatalogForge {
    public ExposureCatalogForge() {
        ExposureCatalog.init();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.Common.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.Client.SPEC);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        RegisterImpl.BLOCKS.register(modEventBus);
        RegisterImpl.BLOCK_ENTITY_TYPES.register(modEventBus);
        RegisterImpl.ITEMS.register(modEventBus);
        RegisterImpl.ENTITY_TYPES.register(modEventBus);
        RegisterImpl.MENU_TYPES.register(modEventBus);
        RegisterImpl.RECIPE_SERIALIZERS.register(modEventBus);
        RegisterImpl.SOUND_EVENTS.register(modEventBus);
        RegisterImpl.COMMAND_ARGUMENT_TYPES.register(modEventBus);

        modEventBus.register(CommonEvents.ModBus.class);
        MinecraftForge.EVENT_BUS.register(CommonEvents.ForgeBus.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.register(ClientEvents.ModBus.class);
            MinecraftForge.EVENT_BUS.register(ClientEvents.ForgeBus.class);
        });
    }
}
