package net.orcinus.galosphere.events;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.HorseRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterEntitySpectatorShadersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.orcinus.galosphere.Galosphere;
import net.orcinus.galosphere.client.gui.CombustionTableScreen;
import net.orcinus.galosphere.client.gui.GoldenBreathOverlay;
import net.orcinus.galosphere.client.gui.SpectreOverlay;
import net.orcinus.galosphere.client.model.SparkleModel;
import net.orcinus.galosphere.client.model.SpectreModel;
import net.orcinus.galosphere.client.model.SterlingArmorModel;
import net.orcinus.galosphere.client.particles.AuraParticle;
import net.orcinus.galosphere.client.particles.CrystalRainParticle;
import net.orcinus.galosphere.client.particles.IndicatorParticle;
import net.orcinus.galosphere.client.particles.providers.SilverBombProvider;
import net.orcinus.galosphere.client.particles.providers.WarpedProvider;
import net.orcinus.galosphere.client.renderer.GlowFlareEntityRenderer;
import net.orcinus.galosphere.client.renderer.SparkleRenderer;
import net.orcinus.galosphere.client.renderer.SpectreRenderer;
import net.orcinus.galosphere.client.renderer.layer.BannerLayer;
import net.orcinus.galosphere.client.renderer.layer.HorseBannerLayer;
import net.orcinus.galosphere.init.GEntityTypes;
import net.orcinus.galosphere.init.GItems;
import net.orcinus.galosphere.init.GMenuTypes;
import net.orcinus.galosphere.init.GModelLayers;
import net.orcinus.galosphere.init.GParticleTypes;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = Galosphere.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    public static int clearWeatherTime;

    @SubscribeEvent 
    public static void onClientSetup(final FMLClientSetupEvent event) {

        MenuScreens.register(GMenuTypes.COMBUSTION_TABLE.get(), CombustionTableScreen::new);

        IEventBus eventBus = MinecraftForge.EVENT_BUS;
        eventBus.register(new GoldenBreathOverlay());
        eventBus.register(new SpectreOverlay());

        event.enqueueWork(() -> {
            ItemProperties.register(Items.CROSSBOW, new ResourceLocation(Galosphere.MODID, "glow_flare"), (stack, world, entity, p_174608_) -> entity != null && CrossbowItem.isCharged(stack) && CrossbowItem.containsChargedProjectile(stack, GItems.GLOW_FLARE.get()) ? 1.0F : 0.0F);
            ItemProperties.register(GItems.BAROMETER.get(), new ResourceLocation(Galosphere.MODID, "weather_level"), new ClampedItemPropertyFunction() {
                private double rotation;
                private int ticksBeforeChange;

                @Override
                public float unclampedCall(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int i) {
                    Entity entity = livingEntity != null ? livingEntity : itemStack.getEntityRepresentation();
                    float[][] predicates = new float[][]{{0.25F, 0.3F, 0.5F, 0.7F, 0.9F}, {0.9F, 0.7F, 0.5F, 0.3F, 0.25F}};
                    if (entity == null) {
                        return 0.0f;
                    }
                    if (clientLevel == null && entity.level instanceof ClientLevel clientWorld) {
                        clientLevel = clientWorld;
                    }
                    if (clientLevel == null) {
                        return 0.0f;
                    }
                    float max;
                    float speed = 0.00525F;
                    if (ClientEvents.clearWeatherTime < 12000) {
                        max = predicates[clientLevel.isRaining() ? 1 : 0][ClientEvents.clearWeatherTime / 3000];
                    } else {
                        max = clientLevel.getLevelData().isRaining() ? 0.0F : 1.0F;
                    }
                    float rainLevel = clientLevel.getRainLevel(1.0F);
                    if ((rainLevel > 0.9F || rainLevel < 0.1F) && ClientEvents.clearWeatherTime == 0 && this.ticksBeforeChange == 0) {
                        this.ticksBeforeChange = 800;
                    }
                    if (this.ticksBeforeChange > 0) {
                        this.ticksBeforeChange--;
                    }
                    if (!clientLevel.dimensionType().natural() && clientLevel.getRandom().nextFloat() < 0.1F) {
                        this.rotation = Mth.positiveModulo(Math.random() - this.rotation, 1);
                    }
                    if (this.rotation < max && this.ticksBeforeChange == 0) {
                        this.rotation += speed;
                    }
                    if (this.rotation > max && this.ticksBeforeChange == 0) {
                        this.rotation -= speed;
                    }
                    return this.rotation >= 0.99 ? 1 : (float) this.rotation;
                }

            });
        });

    }

    @SubscribeEvent
    public static void loadEntityShader(RegisterEntitySpectatorShadersEvent event) {
        event.register(GEntityTypes.SPECTRE.get(), new ResourceLocation(Galosphere.MODID, "shaders/post/fay.json"));
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        HorseRenderer horseRenderer = event.getRenderer(EntityType.HORSE);
        if (horseRenderer == null) return;
        horseRenderer.addLayer(new HorseBannerLayer(horseRenderer));
        event.getSkins().forEach(skin -> {
            PlayerRenderer playerRenderer = event.getSkin(skin);
            if (playerRenderer == null) return;
            playerRenderer.addLayer(new BannerLayer<>(playerRenderer));
        });
    }

    @SubscribeEvent
    public static void registerEntityLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GModelLayers.SPARKLE, SparkleModel::createBodyLayer);
        event.registerLayerDefinition(GModelLayers.STERLING_HELMET, SterlingArmorModel::createBodyLayer);
        event.registerLayerDefinition(GModelLayers.SPECTRE, SpectreModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GEntityTypes.SPARKLE.get(), SparkleRenderer::new);
        event.registerEntityRenderer(GEntityTypes.SIVLER_BOMB.get(), context -> new ThrownItemRenderer<>(context, 1.5F, false));
        event.registerEntityRenderer(GEntityTypes.SPECTRE.get(), SpectreRenderer::new);
        event.registerEntityRenderer(GEntityTypes.GLOW_FLARE.get(), GlowFlareEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.register(GParticleTypes.AURA_LISTENER.get(), AuraParticle.Provider::new);
        event.register(GParticleTypes.SILVER_BOMB.get(), new SilverBombProvider());
        event.register(GParticleTypes.WARPED.get(), WarpedProvider::new);
        event.register(GParticleTypes.ALLURITE_RAIN.get(), CrystalRainParticle.Provider::new);
        event.register(GParticleTypes.LUMIERE_RAIN.get(), CrystalRainParticle.Provider::new);
        event.register(GParticleTypes.AURA_RINGER_INDICATOR.get(), IndicatorParticle.Provider::new);
    }

}
