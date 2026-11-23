package com.cheatgnome.recipe;

import java.util.List;

import com.mojang.serialization.MapCodec;
import org.jspecify.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import com.cheatgnome.GnomeConfig;
import com.cheatgnome.block.GnomeLook;

/**
 * The gnome statue recipe. The wool colors become the hat and beard, the terracotta becomes the coat and face.
 *
 * <pre>
 *   Diamond           Wool (hat)        Diamond
 *   Gold Ingot        Terracotta        Gold Ingot
 *   Totem of Undying  Wool (beard)      Echo Shard (or Nether Star, see config)
 * </pre>
 */
public class GnomeRecipe extends CustomRecipe {
	public static final GnomeRecipe INSTANCE = new GnomeRecipe();
	public static final MapCodec<GnomeRecipe> CODEC = MapCodec.unit(INSTANCE);
	public static final StreamCodec<RegistryFriendlyByteBuf, GnomeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
	public static final RecipeSerializer<GnomeRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return readLook(input) != null;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		GnomeLook look = readLook(input);
		return look == null ? ItemStack.EMPTY : look.toStack();
	}

	private static @Nullable GnomeLook readLook(CraftingInput input) {
		if (input.width() != 3 || input.height() != 3) {
			return null;
		}

		List<ItemStack> items = input.items();

		if (items.size() != 9) {
			return null;
		}

		Item corner = GnomeConfig.get().netherStarRecipe ? Items.NETHER_STAR : Items.ECHO_SHARD;

		if (!items.get(0).is(Items.DIAMOND) || !items.get(2).is(Items.DIAMOND)
				|| !items.get(3).is(Items.GOLD_INGOT) || !items.get(5).is(Items.GOLD_INGOT)
				|| !items.get(6).is(Items.TOTEM_OF_UNDYING) || !items.get(8).is(corner)) {
			return null;
		}

		int hat = GnomeLook.woolColor(items.get(1));
		int terracotta = GnomeLook.terracottaValue(items.get(4));
		int beard = GnomeLook.woolColor(items.get(7));

		if (hat < 0 || beard < 0 || terracotta < 0) {
			return null;
		}

		return new GnomeLook(hat, beard, terracotta);
	}

	@Override
	public RecipeSerializer<? extends CustomRecipe> getSerializer() {
		return SERIALIZER;
	}
}
