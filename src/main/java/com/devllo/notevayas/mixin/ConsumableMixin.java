package com.devllo.notevayas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.devllo.notevayas.ModEffects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

/**
 * Con munchies activo, cualquier comida da 50% mas de saturacion.
 *
 * Se engancha en Consumable.onConsume (que es donde vanilla aplica la comida) porque es
 * el punto mas cercano que todavia tiene la LivingEntity a mano; FoodData.eat no la tiene.
 * Al ser @At("RETURN") vanilla ya sumo la saturacion normal y aca solo se agrega el extra.
 */
@Mixin(Consumable.class)
public class ConsumableMixin {
	@Inject(method = "onConsume", at = @At("RETURN"))
	private void notevayas$munchiesDanMasSaturacion(Level level, LivingEntity entidad, ItemStack stack,
			CallbackInfoReturnable<ItemStack> cir) {
		if (level.isClientSide() || !(entidad instanceof Player jugador)) {
			return;
		}
		if (!jugador.hasEffect(ModEffects.MUNCHIES)) {
			return;
		}

		FoodProperties comida = stack.get(DataComponents.FOOD);
		if (comida == null) {
			return;
		}

		FoodData datos = jugador.getFoodData();
		datos.setSaturation(datos.getSaturationLevel() + comida.saturation() * ModEffects.BONUS_SATURACION);
	}
}
