package farsight.mixin;

import farsight.FarsightMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class ClientOptionsMixin
{
    @Inject(method = "load", at = @At("HEAD"))
    private void onInit(final CallbackInfo ci)
    {
        this.renderDistance = new OptionInstance(
          "options.renderDistance",
          OptionInstance.noTooltip(),
          (optionText, value) -> genericValueLabel(optionText, Component.translatable("options.chunks", new Object[] {value})),
          new OptionInstance.IntRange(2, FarsightMod.config.getCommonConfig().maxRenderDistance),
          12,
          value -> {
              Minecraft.getInstance().levelRenderer.needsUpdate();
          }
        );

        this.simulationDistance = new OptionInstance(
          "options.simulationDistance",
          OptionInstance.noTooltip(),
          (optionText, value) -> genericValueLabel(optionText, Component.translatable("options.chunks", new Object[] {value})),
          new OptionInstance.IntRange(5, FarsightMod.config.getCommonConfig().maxRenderDistance),
          12,
          value -> {
          }
        );
    }

    @Mutable
    @Shadow
    @Final
    private OptionInstance<Integer> renderDistance;

    @Shadow
    public static Component genericValueLabel(final Component prefix, final Component value)
    {
        return null;
    }

    @Mutable
    @Shadow
    @Final
    private OptionInstance<Integer> simulationDistance;
}
