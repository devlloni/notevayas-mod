package com.devllo.notevayas;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

/**
 * Receta de los consumibles. Es una receta custom (y no 4 JSON shapeless) porque tiene
 * que hacer dos cosas que un JSON no puede:
 *
 *  1. Copiar cepa y calidad del cogollo al resultado. Si hay cogollos de distinta
 *     calidad, se usa la MENOR.
 *  2. Aplicar la regla del moledor: si esta en la grilla al armar porros, salen 4 en vez
 *     de 2 y el moledor se queda gastando 1 de durabilidad.
 *
 * crafting_transmute copia components pero solo admite 2 ingredientes, asi que solo
 * habria servido para el porro.
 *
 * Ser CustomRecipe NO impide salir en el libro de recetas: isSpecial() solo apaga el
 * autocompletado de la grilla. Lo que muestra el libro es lo que devuelve display(), que
 * por defecto viene vacio; abajo se arma una entrada por combinacion (ver display()).
 */
public class RecetaConsumible extends CustomRecipe {
	/** El tag de cogollos secos, para mostrar "cualquier cepa" en el libro de recetas. */
	private static final TagKey<Item> COGOLLOS = TagKey.create(Registries.ITEM, NoTeVayas.id("cogollos"));
	/** Una combinacion: que hace falta ademas de los cogollos, y que sale. */
	private record Combinacion(int cogollos, List<Item> extras, Item resultado, int cantidad) {
	}

	private static final List<Combinacion> COMBINACIONES = List.of(
			// 1 cogollo + 1 papel -> 2 porros
			new Combinacion(1, List.of(ModItems.PAPEL), ModItems.PORRO, 2),
			// 2 cogollos + 1 papel + 1 azucar -> 1 blunt
			new Combinacion(2, List.of(ModItems.PAPEL, Items.SUGAR), ModItems.BLUNT, 1),
			// 1 cogollo + 1 cacao + 1 trigo + 1 huevo -> 3 brownies
			new Combinacion(1, List.of(Items.COCOA_BEANS, Items.WHEAT, Items.EGG), ModItems.BROWNIE, 3),
			// 1 torta + 3 cogollos -> 1 space cake
			new Combinacion(3, List.of(Items.CAKE), ModItems.SPACE_CAKE, 1));

	/** Con moledor en la grilla, el porro rinde el doble. */
	private static final int MULTIPLICADOR_MOLEDOR = 2;

	public RecetaConsumible() {
		super();
	}

	@Override
	public RecipeSerializer<? extends CustomRecipe> getSerializer() {
		return ModRecipes.CONSUMIBLE;
	}

	@Override
	public CraftingBookCategory category() {
		return CraftingBookCategory.MISC;
	}

	// ---------------------------------------------------------------- analisis de grilla

	/** Lo que hay en la grilla, ya clasificado. */
	private record Contenido(List<ItemStack> cogollos, List<ItemStack> otros, boolean hayMoledor) {
	}

	private static Contenido analizar(CraftingInput input) {
		List<ItemStack> cogollos = new ArrayList<>();
		List<ItemStack> otros = new ArrayList<>();
		boolean moledor = false;

		for (ItemStack stack : input.items()) {
			if (stack.isEmpty()) {
				continue;
			}
			if (stack.getItem() instanceof CogolloItem cogollo && !cogollo.esFresco()) {
				cogollos.add(stack);
			} else if (stack.is(ModItems.MOLEDOR)) {
				moledor = true;
			} else {
				otros.add(stack);
			}
		}
		return new Contenido(cogollos, otros, moledor);
	}

	/** Devuelve la combinacion que encaja con la grilla, o null. */
	private static Combinacion buscar(Contenido contenido) {
		for (Combinacion combo : COMBINACIONES) {
			if (contenido.cogollos().size() != combo.cogollos()
					|| contenido.otros().size() != combo.extras().size()) {
				continue;
			}

			List<Item> pendientes = new ArrayList<>(combo.extras());
			boolean encaja = true;
			for (ItemStack stack : contenido.otros()) {
				if (!pendientes.remove(stack.getItem())) {
					encaja = false;
					break;
				}
			}

			if (encaja && pendientes.isEmpty()) {
				return combo;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- receta

	@Override
	public boolean matches(CraftingInput input, Level level) {
		Contenido contenido = analizar(input);
		if (contenido.cogollos().isEmpty()) {
			return false;
		}

		Combinacion combo = buscar(contenido);
		// El moledor solo tiene sentido en la receta del porro.
		return combo != null && (!contenido.hayMoledor() || combo.resultado() == ModItems.PORRO);
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		Contenido contenido = analizar(input);
		Combinacion combo = buscar(contenido);
		if (combo == null) {
			return ItemStack.EMPTY;
		}

		// Cepa del primer cogollo; calidad = la MENOR de todas.
		Cepa cepa = ((CogolloItem) contenido.cogollos().getFirst().getItem()).cepa();
		int calidad = contenido.cogollos().stream()
				.mapToInt(stack -> CogolloItem.calidadDe(stack).nivel())
				.min()
				.orElse(Calidad.REGULAR);

		int cantidad = combo.cantidad();
		if (contenido.hayMoledor() && combo.resultado() == ModItems.PORRO) {
			cantidad *= MULTIPLICADOR_MOLEDOR;
		}

		ItemStack resultado = new ItemStack(combo.resultado(), cantidad);
		resultado.set(ModComponents.CEPA, cepa);
		resultado.set(ModComponents.CALIDAD, new Calidad(calidad));
		return resultado;
	}

	// ---------------------------------------------------------------- libro de recetas

	/**
	 * Una entrada del libro por combinacion, mas una extra para el porro con moledor (que
	 * rinde el doble). Los cogollos se muestran como el tag #notevayas:cogollos, asi que
	 * el libro cicla las 4 cepas en vez de fijar una.
	 *
	 * El resultado se muestra sin components: la cepa y la calidad salen recien al
	 * craftear, porque dependen de los cogollos que uses.
	 */
	@Override
	public List<RecipeDisplay> display() {
		List<RecipeDisplay> entradas = new ArrayList<>();
		for (Combinacion combo : COMBINACIONES) {
			entradas.add(mostrar(combo, false));
			if (combo.resultado() == ModItems.PORRO) {
				entradas.add(mostrar(combo, true));
			}
		}
		return entradas;
	}

	private static RecipeDisplay mostrar(Combinacion combo, boolean conMoledor) {
		List<SlotDisplay> ingredientes = new ArrayList<>();
		for (int i = 0; i < combo.cogollos(); i++) {
			ingredientes.add(new SlotDisplay.TagSlotDisplay(COGOLLOS));
		}
		for (Item extra : combo.extras()) {
			ingredientes.add(new SlotDisplay.ItemSlotDisplay(extra));
		}

		int cantidad = combo.cantidad();
		if (conMoledor) {
			ingredientes.add(new SlotDisplay.ItemSlotDisplay(ModItems.MOLEDOR));
			cantidad *= MULTIPLICADOR_MOLEDOR;
		}

		return new ShapelessCraftingRecipeDisplay(
				ingredientes,
				new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(combo.resultado(), cantidad)),
				new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE));
	}

	/** El moledor no se consume: se queda en la grilla con 1 punto menos de durabilidad. */
	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
		NonNullList<ItemStack> restos = NonNullList.withSize(input.size(), ItemStack.EMPTY);

		for (int i = 0; i < input.size(); i++) {
			ItemStack stack = input.getItem(i);
			if (!stack.is(ModItems.MOLEDOR)) {
				continue;
			}

			ItemStack moledor = stack.copy();
			if (moledor.getDamageValue() + 1 < moledor.getMaxDamage()) {
				moledor.setDamageValue(moledor.getDamageValue() + 1);
				restos.set(i, moledor);
			}
			// Si se le acabo la durabilidad, no se devuelve: se rompe.
		}
		return restos;
	}
}
