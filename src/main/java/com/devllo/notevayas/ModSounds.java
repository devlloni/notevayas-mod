package com.devllo.notevayas;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final Identifier NO_TE_VAYAS_ID = NoTeVayas.id("notevayas");

	// Variable range: the audible radius is derived from the volume passed to playSound
	// (16 blocks at volume <= 1.0, 16 * volume above that).
	// Swap this for SoundEvent.createFixedRangeEvent(NO_TE_VAYAS_ID, blocks) to pin the range instead.
	public static final SoundEvent NO_TE_VAYAS = SoundEvent.createVariableRangeEvent(NO_TE_VAYAS_ID);

	private ModSounds() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.SOUND_EVENT, NO_TE_VAYAS_ID, NO_TE_VAYAS);
	}
}
