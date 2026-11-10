package com.cheatgnome.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.cheatgnome.CheatGnome;
import com.cheatgnome.gameplay.GnomeInteractions;

/**
 * The gnome statue. Faces the player who placed it; the face drawn on it comes from the {@link #FACE} property
 * and its colors come from the block entity's {@link GnomeLook}.
 */
public class GnomeBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty FACE = IntegerProperty.create("face", 0, 16);

	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(4.5, 0, 6, 11.5, 11, 11),
			Block.box(6, 11, 7, 10, 16, 10)
	);

	public GnomeBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FACE);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		GnomeLook look = GnomeLook.fromStack(context.getItemInHand());
		return this.defaultBlockState()
				.setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(FACE, look.face());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);

		if (level.getBlockEntity(pos) instanceof GnomeBlockEntity gnome) {
			gnome.setLook(GnomeLook.fromStack(stack));
			gnome.markVisited(level.getGameTime());
		}
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GnomeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, CheatGnome.GNOME_BLOCK_ENTITY, GnomeBlockEntity::serverTick);
	}

	// ---------------------------------------------------------------------
	// Interaction
	// ---------------------------------------------------------------------

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof GnomeBlockEntity gnome) {
			GnomeInteractions.onEmptyHandUse(serverPlayer, gnome);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		// Sneaking with an item lets players place blocks against the gnome as usual.
		if (player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof GnomeBlockEntity gnome) {
			return GnomeInteractions.onItemUse(serverPlayer, gnome, stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}

		return InteractionResult.SUCCESS;
	}

	/** Punching the gnome (without sneaking) can make it cranky. */
	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		super.attack(state, level, pos, player);

		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof GnomeBlockEntity gnome) {
			GnomeInteractions.onPunched(serverPlayer, gnome);
		}
	}

	/** The gnome can only be picked up while sneaking, so a stray punch never breaks it. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (!player.isShiftKeyDown()) {
			return 0.0F;
		}

		return super.getDestroyProgress(state, player, level, pos);
	}

	/** Drops the gnome with its colors kept (it has no loot table on purpose). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (level instanceof ServerLevel && !player.getAbilities().instabuild
				&& level.getBlockEntity(pos) instanceof GnomeBlockEntity gnome) {
			Block.popResource(level, pos, gnome.getLook().toStack());
		}

		return super.playerWillDestroy(level, pos, state, player);
	}

	public static Component gnomeName() {
		return Component.translatable("block.cheatgnome.gnome");
	}
}
