package com.cheatgnome.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.cheatgnome.CheatGnome;
import com.cheatgnome.gameplay.Mischief;

/** Remembers how the gnome looks and when it last saw a friendly face. */
public class GnomeBlockEntity extends BlockEntity {
	private GnomeLook look = GnomeLook.DEFAULT;
	/** Game time of the last gift handed out. */
	private long lastGiftTick = Long.MIN_VALUE / 2;
	/** Game time of the last interaction of any kind. */
	private long lastVisitTick = 0;
	/** Game time until which the gnome turns to stare at the nearest player. */
	private long stareUntilTick = 0;
	/** Game time of the last prank, so a lonely gnome doesn't prank non-stop. */
	private long lastPrankTick = 0;

	public GnomeBlockEntity(BlockPos pos, BlockState state) {
		super(CheatGnome.GNOME_BLOCK_ENTITY, pos, state);
	}

	public GnomeLook getLook() {
		return look;
	}

	public void setLook(GnomeLook look) {
		this.look = look;
		this.sync();
	}

	public long getLastGiftTick() {
		return lastGiftTick;
	}

	public void setLastGiftTick(long tick) {
		this.lastGiftTick = tick;
		this.setChanged();
	}

	public long getLastVisitTick() {
		return lastVisitTick;
	}

	public void markVisited(long tick) {
		this.lastVisitTick = tick;
		this.setChanged();
	}

	public void startStaring(long untilTick) {
		this.stareUntilTick = untilTick;
		this.setChanged();
	}

	public boolean isStaring(long now) {
		return now < stareUntilTick;
	}

	public long getLastPrankTick() {
		return lastPrankTick;
	}

	public void setLastPrankTick(long tick) {
		this.lastPrankTick = tick;
		this.setChanged();
	}

	/** Copies everything except position from another gnome (used when the gnome wanders off). */
	public void copyFrom(GnomeBlockEntity other) {
		this.look = other.look;
		this.lastGiftTick = other.lastGiftTick;
		this.lastVisitTick = other.lastVisitTick;
		this.stareUntilTick = other.stareUntilTick;
		this.lastPrankTick = other.lastPrankTick;
		this.sync();
	}

	private void sync() {
		this.setChanged();

		if (this.level != null) {
			BlockState state = this.getBlockState();
			this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
		}
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, GnomeBlockEntity gnome) {
		if (level instanceof ServerLevel serverLevel) {
			Mischief.tickGnome(serverLevel, pos, state, gnome);
		}
	}

	// ---------------------------------------------------------------------
	// Saving and syncing
	// ---------------------------------------------------------------------

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("hat", look.hat());
		output.putInt("beard", look.beard());
		output.putInt("terracotta", look.terracotta());
		output.putLong("last_gift", lastGiftTick);
		output.putLong("last_visit", lastVisitTick);
		output.putLong("stare_until", stareUntilTick);
		output.putLong("last_prank", lastPrankTick);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		GnomeLook oldLook = this.look;
		this.look = new GnomeLook(
				input.getIntOr("hat", GnomeLook.DEFAULT.hat()),
				input.getIntOr("beard", GnomeLook.DEFAULT.beard()),
				input.getIntOr("terracotta", GnomeLook.DEFAULT.terracotta())
		);
		this.lastGiftTick = input.getLongOr("last_gift", Long.MIN_VALUE / 2);
		this.lastVisitTick = input.getLongOr("last_visit", 0L);
		this.stareUntilTick = input.getLongOr("stare_until", 0L);
		this.lastPrankTick = input.getLongOr("last_prank", 0L);

		// On the client, redraw the statue when its colors arrive.
		if (this.level != null && this.level.isClientSide() && !oldLook.equals(this.look)) {
			BlockState state = this.getBlockState();
			this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveCustomOnly(registries);
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Read by the client's tint code from the chunk-meshing thread. */
	@Override
	public Object getRenderData() {
		return look;
	}
}
