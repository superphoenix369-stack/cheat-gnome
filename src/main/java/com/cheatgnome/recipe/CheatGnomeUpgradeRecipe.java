package com.cheatgnome.recipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.cheatgnome.CheatGnome;
import com.cheatgnome.block.GnomeLook;

/**
 * Garden Gnome + Nether Star, anywhere in the grid, makes a Cheat Gnome with the same hat, beard, coat and face.
 */
public class CheatGnomeUpgradeRecipe extends CustomRecipe {
	public static final CheatGnomeUpgradeRecipe INSTANCE = new CheatGnomeUpgradeRecipe();
	public static final MapCodec<CheatGnomeUpgradeRecipe> CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, CheatGnomeUpgradeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<CheatGnomeUpgradeRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return !findGardenGnome(input).isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		ItemStack gardenGnome = findGardenGnome(input);
		return gardenGnome.isEmpty() ? ItemStack.EMPTY : GnomeLook.fromStack(gardenGnome).toStack(true);
	}

	/** Returns the Garden Gnome if the grid holds exactly one Garden Gnome and one Nether Star, otherwise empty. */
	private static ItemStack findGardenGnome(CraftingInput input) {
		ItemStack gnome = ItemStack.EMPTY;
		int stars = 0;

		for (ItemStack stack : input.items()) {
			if (stack.isEmpty()) {
				continue;
			}

			if (stack.is(CheatGnome.GNOME_ITEM) && gnome.isEmpty()) {
				gnome = stack;
			} else if (stack.is(Items.NETHER_STAR) && stars == 0) {
				stars++;
			} else {
				return ItemStack.EMPTY;
			}
		}

		return stars == 1 ? gnome : ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<? extends CustomRecipe> getSerializer() {
		return SERIALIZER;
	}
}
