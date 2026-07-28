package com.devllo.notevayas;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Secadero: 4 slots que convierten cogollo fresco en cogollo seco, conservando la calidad.
 *
 * Un slot guarda o bien un cogollo fresco curandose (con su progreso) o bien el cogollo
 * seco ya listo para retirar.
 */
public class SecaderoBlockEntity extends BlockEntity {
	public static final int SLOTS = 4;

	/** Ticks que tarda un slot en curar. 4800 ticks = 4 minutos. */
	public static final int TICKS_DE_CURADO = 4800;

	private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private final int[] progreso = new int[SLOTS];

	public SecaderoBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SECADERO, pos, state);
	}

	// ---------------------------------------------------------------- estado

	public NonNullList<ItemStack> items() {
		return this.items;
	}

	/** Mete un cogollo fresco en el primer slot libre. Devuelve false si esta lleno. */
	public boolean meter(ItemStack fresco) {
		for (int i = 0; i < SLOTS; i++) {
			if (this.items.get(i).isEmpty()) {
				this.items.set(i, fresco.copyWithCount(1));
				this.progreso[i] = 0;
				setChanged();
				return true;
			}
		}
		return false;
	}

	/** Saca todos los cogollos ya secos y deja los que siguen curandose. */
	public List<ItemStack> sacarListos() {
		List<ItemStack> listos = new ArrayList<>();
		for (int i = 0; i < SLOTS; i++) {
			ItemStack slot = this.items.get(i);
			if (slot.getItem() instanceof CogolloItem cogollo && !cogollo.esFresco()) {
				listos.add(slot);
				this.items.set(i, ItemStack.EMPTY);
				this.progreso[i] = 0;
			}
		}
		if (!listos.isEmpty()) {
			setChanged();
		}
		return listos;
	}

	// ---------------------------------------------------------------- tick

	public static void tick(Level level, BlockPos pos, BlockState state, SecaderoBlockEntity secadero) {
		boolean cambio = false;

		for (int i = 0; i < SLOTS; i++) {
			ItemStack slot = secadero.items.get(i);
			if (!(slot.getItem() instanceof CogolloItem cogollo) || !cogollo.esFresco()) {
				continue;
			}

			secadero.progreso[i]++;
			if (secadero.progreso[i] < TICKS_DE_CURADO) {
				continue;
			}

			// Se conserva la calidad: se copia el component al cogollo seco.
			ItemStack seco = new ItemStack(ModItems.COGOLLOS.get(cogollo.cepa()));
			seco.set(ModComponents.CALIDAD, CogolloItem.calidadDe(slot));

			secadero.items.set(i, seco);
			secadero.progreso[i] = 0;
			cambio = true;
		}

		if (cambio) {
			secadero.setChanged();
		}
	}

	// ---------------------------------------------------------------- persistencia

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(this.items));
		output.putIntArray("progreso", this.progreso);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);

		this.items.clear();
		for (int i = 0; i < SLOTS; i++) {
			this.items.add(ItemStack.EMPTY);
		}
		input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(guardados -> {
			for (int i = 0; i < Math.min(SLOTS, guardados.size()); i++) {
				this.items.set(i, guardados.get(i));
			}
		});

		java.util.Arrays.fill(this.progreso, 0);
		input.getIntArray("progreso").ifPresent(guardado -> {
			System.arraycopy(guardado, 0, this.progreso, 0, Math.min(SLOTS, guardado.length));
		});
	}
}
