package com.cheatgnome;

import com.mojang.serialization.Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import com.cheatgnome.block.DecoyGnomeBlock;
import com.cheatgnome.block.GnomeBlock;
import com.cheatgnome.block.GnomeBlockEntity;
import com.cheatgnome.block.GnomeLook;
import com.cheatgnome.gameplay.Cheats;
import com.cheatgnome.gameplay.GnomeInteractions;
import com.cheatgnome.gameplay.Mischief;
import com.cheatgnome.net.GnomeCheatPayload;
import com.cheatgnome.net.GnomeSettingPayload;
import com.cheatgnome.net.OpenGnomeMenuPayload;
import com.cheatgnome.recipe.GnomeRecipe;

public class CheatGnome implements ModInitializer {
	public static final String MOD_ID = "cheatgnome";
	public static final Logger LOGGER = LoggerFactory.getLogger("Cheat Gnome");

	public static final ResourceKey<Block> GNOME_KEY = blockKey("gnome");
	public static final GnomeBlock GNOME_BLOCK = new GnomeBlock(BlockBehaviour.Properties.of()
			.setId(GNOME_KEY)
			.strength(1.0F, 1200.0F)
			.sound(SoundType.STONE)
			.noOcclusion()
			.pushReaction(PushReaction.IMMOVEABLE));
	public static final BlockItem GNOME_ITEM = new BlockItem(GNOME_BLOCK, new Item.Properties()
			.setId(ResourceKey.create(Registries.ITEM, GNOME_KEY.identifier()))
			.rarity(Rarity.RARE)
			.stacksTo(16));

	public static final ResourceKey<Block> DECOY_GNOME_KEY = blockKey("decoy_gnome");
	public static final DecoyGnomeBlock DECOY_GNOME_BLOCK = new DecoyGnomeBlock(BlockBehaviour.Properties.of()
			.setId(DECOY_GNOME_KEY)
			.instabreak()
			.sound(SoundType.STONE)
			.noOcclusion()
			.pushReaction(PushReaction.POPPED));

	public static BlockEntityType<GnomeBlockEntity> GNOME_BLOCK_ENTITY;

	/** Remembers whether a player already got their free first-join gnome. */
	public static final AttachmentType<Boolean> RECEIVED_GNOME = AttachmentRegistry.create(id("received_gnome"),
			builder -> builder.persistent(Codec.BOOL).copyOnDeath());

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static ResourceKey<Block> blockKey(String path) {
		return ResourceKey.create(Registries.BLOCK, id(path));
	}

	@Override
	public void onInitialize() {
		GnomeConfig.load();

		// Blocks, items, block entity
		Registry.register(BuiltInRegistries.BLOCK, GNOME_KEY, GNOME_BLOCK);
		Registry.register(BuiltInRegistries.ITEM, GNOME_KEY.identifier(), GNOME_ITEM);
		Registry.register(BuiltInRegistries.BLOCK, DECOY_GNOME_KEY, DECOY_GNOME_BLOCK);
		GNOME_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, GNOME_KEY.identifier(),
				FabricBlockEntityTypeBuilder.create(GnomeBlockEntity::new, GNOME_BLOCK).build());

		// Recipe
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id("gnome_statue"), GnomeRecipe.SERIALIZER);

		// Creative inventory: the classic gnome plus a few dressed-up ones
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(GnomeLook.DEFAULT.toStack());
			output.accept(new GnomeLook(11, 0, 17 + 11).toStack());  // blue hat, cool shades
			output.accept(new GnomeLook(5, 12, 17 + 5).toStack());   // lime hat, brown beard, cheeky
			output.accept(new GnomeLook(10, 7, 17 + 15).toStack());  // purple hat, gray beard, spooky
			output.accept(new GnomeLook(6, 0, 17 + 2).toStack());    // pink hat, lovestruck
		});

		// Networking
		PayloadTypeRegistry.clientboundPlay().register(OpenGnomeMenuPayload.TYPE, OpenGnomeMenuPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GnomeCheatPayload.TYPE, GnomeCheatPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GnomeSettingPayload.TYPE, GnomeSettingPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(GnomeCheatPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			ServerLevel level = (ServerLevel) player.level();
			BlockPos pos = payload.pos();

			if (!isNearGnome(player, level, pos) || !GnomeInteractions.usesCheatMenu(player)) {
				return;
			}

			Cheats.run(payload.cheat(), player, level, pos);
		});

		ServerPlayNetworking.registerGlobalReceiver(GnomeSettingPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();

			if (!GnomeInteractions.hasCheats(player)) {
				return;
			}

			if (GnomeConfig.get().set(payload.key(), payload.value())) {
				GnomeConfig.save();
			}
		});

		// First-join gnome
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;

			if (GnomeConfig.get().giveGnomeOnFirstJoin && !player.getAttachedOrElse(RECEIVED_GNOME, false)) {
				player.setAttached(RECEIVED_GNOME, true);
				GnomeInteractions.give(player, GnomeLook.DEFAULT.toStack());
				player.sendSystemMessage(Component.translatable("gnome.cheatgnome.first_join"));
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(Mischief::tickRainClouds);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> Mischief.clearRainClouds());

		LOGGER.info("The cheat gnome is ready. Psst... try right-clicking it.");
	}

	private static boolean isNearGnome(ServerPlayer player, ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof GnomeBlock
				&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 10 * 10;
	}
}
