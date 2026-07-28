package com.devllo.notevayas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.devllo.notevayas.ModEffects;

import net.minecraft.world.entity.player.Player;

/**
 * Con munchies activo el hambre baja 50% mas rapido.
 *
 * Se intercepta Player.causeFoodExhaustion y no FoodData.addExhaustion porque FoodData
 * no tiene referencia al jugador, asi que ahi adentro no se puede consultar el efecto.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
	private float notevayas$munchiesAceleranElHambre(float exhaustion) {
		Player self = (Player) (Object) this;
		return self.hasEffect(ModEffects.MUNCHIES)
				? exhaustion * ModEffects.MULTIPLICADOR_HAMBRE
				: exhaustion;
	}
}
