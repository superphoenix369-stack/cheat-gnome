package com.cheatgnome.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.cheatgnome.gameplay.Fx;

/**
 * A fake gnome left behind by the decoy prank. Vanishes in a puff when touched, or on its own after a minute.
 * It never drops anything.
 */
public class DecoyGnomeBlock extends Block {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final int LIFETIME_TICKS = 20 * 60;

	private static final VoxelShape SHAPE = Block.box(4.5, 0, 6, 11.5, 16, 11);

	public DecoyGnomeBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel serverLevel) {
			poof(serverLevel, pos);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
		if (level instanceof ServerLevel serverLevel) {
			poof(serverLevel, pos);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		poof(level, pos);
	}

	public static void poof(ServerLevel level, BlockPos pos) {
		if (level.getBlockState(pos).getBlock() instanceof DecoyGnomeBlock) {
			level.removeBlock(pos, false);
			Fx.particles(level, "minecraft:poof", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.25, 12, 0.02);
			Fx.sound(level, "minecraft:entity.allay.item_thrown", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.8F, 1.6F);
		}
	}
}
