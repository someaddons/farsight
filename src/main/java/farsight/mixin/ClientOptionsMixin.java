package farsight.mixin;

import farsight.FarsightMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public abstract class ClientOptionsMixin
{
    @Inject(method = "load", at = @At("HEAD"))
    private void onInit(final CallbackInfo ci)
    {
        this.viewDistance = new SimpleOption(
          "options.renderDistance",
          SimpleOption.emptyTooltip(),
          (optionText, value) -> getGenericValueText(optionText, Text.translatable("options.chunks", new Object[] {value})),
          new SimpleOption.ValidatingIntSliderCallbacks(2, FarsightMod.config.getCommonConfig().maxRenderDistance),
          12,
          value -> {
              MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate();
          }
        );

        this.simulationDistance = new SimpleOption(
          "options.simulationDistance",
          SimpleOption.emptyTooltip(),
          (optionText, value) -> getGenericValueText(optionText, Text.translatable("options.chunks", new Object[] {value})),
          new SimpleOption.ValidatingIntSliderCallbacks(5, FarsightMod.config.getCommonConfig().maxRenderDistance),
          12,
          value -> {
          }
        );
    }

    @Mutable
    @Shadow
    @Final
    private SimpleOption<Integer> viewDistance;

    @Shadow
    public static Text getGenericValueText(final Text prefix, final Text value)
    {
        return null;
    }

    @Mutable
    @Shadow
    @Final
    private SimpleOption<Integer> simulationDistance;
}
