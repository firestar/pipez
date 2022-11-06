package de.maxhenkel.pipez.blocks.tileentity;

import de.maxhenkel.corelib.blockentity.ITickableBlockEntity;
import de.maxhenkel.pipez.DirectionalPosition;
import de.maxhenkel.pipez.blocks.PipeBlock;
import de.maxhenkel.pipez.blocks.tileentity.types.EnergyPipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public abstract class PipeTileEntity extends BlockEntity implements ITickableBlockEntity {

    @Nullable
    protected List<Connection>[] connectionCache = new List[3];
    protected boolean[] extractingSideEnergy;
    protected boolean[] extractingSideItem;
    protected boolean[] extractingSideFluid;
    protected boolean[] disconnectedSides;

    /**
     * Invalidating the cache five ticks after load, because Mekanism is broken!
     */
    private int invalidateCountdown;

    public PipeTileEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
        extractingSideFluid = new boolean[Direction.values().length];
        extractingSideEnergy = new boolean[Direction.values().length];
        extractingSideItem = new boolean[Direction.values().length];

        disconnectedSides = new boolean[Direction.values().length];
    }

    public List<Connection> getConnectionsItem() {
        if (level == null) {
            return new ArrayList<>();
        }
        if (connectionCache[0] == null) {
            updateCache();
            if (connectionCache[0] == null) {
                return new ArrayList<>();
            }
        }
        return connectionCache[0];
    }
    public List<Connection> getConnectionsFluid() {
        if (level == null) {
            return new ArrayList<>();
        }
        if (connectionCache[1] == null) {
            updateCache();
            if (connectionCache[1] == null) {
                return new ArrayList<>();
            }
        }
        return connectionCache[1];
    }
    public List<Connection> getConnectionsEnergy() {
        if (level == null) {
            return new ArrayList<>();
        }
        if (connectionCache[2] == null) {
            updateCache();
            if (connectionCache[2] == null) {
                return new ArrayList<>();
            }
        }
        return connectionCache[2];
    }

    public static void markPipesDirty(Level world, BlockPos pos) {
        List<BlockPos> travelPositions = new ArrayList<>();
        LinkedList<BlockPos> queue = new LinkedList<>();
        Block block = world.getBlockState(pos).getBlock();
        if (!(block instanceof PipeBlock)) {
            return;
        }
        PipeBlock pipeBlock = (PipeBlock) block;

        PipeTileEntity pipeTe = pipeBlock.getTileEntity(world, pos);
        if (pipeTe != null) {
            for (Direction side : Direction.values()) {
                boolean changed = false;
                if (pipeTe.isExtractingItems(side)) {
                    if (!pipeBlock.canConnectTo(world, pos, side)) {
                        pipeTe.setExtractingItem(side, false);
                        if (!pipeTe.hasReasonToStayItem()) {
                            pipeBlock.setHasData(world, pos, false);
                        }
                        changed = true;
                    }
                }
                if (pipeTe.isExtractingEnergy(side)) {
                    if (!pipeBlock.canConnectTo(world, pos, side)) {
                        pipeTe.setExtractingEnergy(side, false);
                        if (!pipeTe.hasReasonToStayEnergy()) {
                            pipeBlock.setHasData(world, pos, false);
                        }
                        changed = true;
                    }
                }
                if (pipeTe.isExtractingFluids(side)) {
                    if (!pipeBlock.canConnectTo(world, pos, side)) {
                        pipeTe.setExtractingFluid(side, false);
                        if (!pipeTe.hasReasonToStayFluid()) {
                            pipeBlock.setHasData(world, pos, false);
                        }
                        changed = true;

                    }
                }
                if (changed) pipeTe.syncData();
            }
        }

        travelPositions.add(pos);
        addToDirtyList(world, pos, pipeBlock, travelPositions, queue);
        while (queue.size() > 0) {
            BlockPos blockPos = queue.removeFirst();
            block = world.getBlockState(blockPos).getBlock();
            if (block instanceof PipeBlock) {
                addToDirtyList(world, blockPos, (PipeBlock) block, travelPositions, queue);
            }
        }
        for (BlockPos p : travelPositions) {
            BlockEntity te = world.getBlockEntity(p);
            if (!(te instanceof PipeTileEntity)) {
                continue;
            }
            PipeTileEntity pipe = (PipeTileEntity) te;
            pipe.connectionCache = new List[]{null, null, null};
        }
    }

    private static void addToDirtyList(Level world, BlockPos pos, PipeBlock pipeBlock, List<BlockPos> travelPositions, LinkedList<BlockPos> queue) {
        for (Direction direction : Direction.values()) {
            if (pipeBlock.isConnected(world, pos, direction)) {
                BlockPos p = pos.relative(direction);
                if (!travelPositions.contains(p) && !queue.contains(p)) {
                    travelPositions.add(p);
                    queue.add(p);
                }
            }
        }
    }

    private Map<DirectionalPosition, Integer> buildConnections(Level world, BlockPos position, int type){
        Map<DirectionalPosition, Integer> connections = new HashMap<>();
        Map<BlockPos, Integer> queue = new HashMap<>();
        List<BlockPos> travelPositions = new ArrayList<>();
        addToQueue(level, worldPosition, queue, travelPositions, connections, 1, type);

        while (queue.size() > 0) {
            Map.Entry<BlockPos, Integer> blockPosIntegerEntry = queue.entrySet().stream().findAny().get();
            addToQueue(level, blockPosIntegerEntry.getKey(), queue, travelPositions, connections, blockPosIntegerEntry.getValue(), type);
            travelPositions.add(blockPosIntegerEntry.getKey());
            queue.remove(blockPosIntegerEntry.getKey());
        }
        return connections;
    }

    private void updateCache() {
        BlockState blockState = getBlockState();
        if (!(blockState.getBlock() instanceof PipeBlock)) {
            connectionCache = new List[]{null, null, null};
            return;
        }
        if (!isExtractingItems() && !isExtractingFluids() && !isExtractingEnergy()) {
            connectionCache = new List[]{null, null, null};
            return;
        }


        if (isExtractingItems() && isExtractingFluids() && isExtractingEnergy()) {
            connectionCache[0] = buildConnections(level, worldPosition, 0).entrySet().stream().map(entry -> new Connection(entry.getKey().getPos(), entry.getKey().getDirection(), entry.getValue())).collect(Collectors.toList());
            connectionCache[1] = connectionCache[0];
            connectionCache[2] = connectionCache[0];
            return;
        }
        if (isExtractingItems()) {
            connectionCache[0] = buildConnections(level, worldPosition, 0).entrySet().stream().map(entry -> new Connection(entry.getKey().getPos(), entry.getKey().getDirection(), entry.getValue())).collect(Collectors.toList());
        }
        if (isExtractingFluids()) {
            connectionCache[1] = buildConnections(level, worldPosition, 1).entrySet().stream().map(entry -> new Connection(entry.getKey().getPos(), entry.getKey().getDirection(), entry.getValue())).collect(Collectors.toList());
        }
        if (isExtractingEnergy()) {
            connectionCache[2] = buildConnections(level, worldPosition, 2).entrySet().stream().map(entry -> new Connection(entry.getKey().getPos(), entry.getKey().getDirection(), entry.getValue())).collect(Collectors.toList());
        }
    }

    public void addToQueue(Level world, BlockPos position, Map<BlockPos, Integer> queue, List<BlockPos> travelPositions, Map<DirectionalPosition, Integer> insertPositions, int distance, int type) {
        Block block = world.getBlockState(position).getBlock();
        if (!(block instanceof PipeBlock)) {
            return;
        }
        PipeBlock pipeBlock = (PipeBlock) block;
        for (Direction direction : Direction.values()) {
            if (pipeBlock.isConnected(world, position, direction)) {
                BlockPos p = position.relative(direction);
                DirectionalPosition dp = new DirectionalPosition(p, direction.getOpposite());
                if (canInsertFluid(position, direction) && type==1) {
                    if (!insertPositions.containsKey(dp)) {
                        insertPositions.put(dp, distance);
                    } else {
                        if (insertPositions.get(dp) > distance) {
                            insertPositions.put(dp, distance);
                        }
                    }
                } else if (canInsertItem(position, direction) && type==0) {
                    if (!insertPositions.containsKey(dp)) {
                        insertPositions.put(dp, distance);
                    } else {
                        if (insertPositions.get(dp) > distance) {
                            insertPositions.put(dp, distance);
                        }
                    }
                } else if (canInsertEnergy(position, direction) && type==2) {
                    if (!insertPositions.containsKey(dp)) {
                        insertPositions.put(dp, distance);
                    } else {
                        if (insertPositions.get(dp) > distance) {
                            insertPositions.put(dp, distance);
                        }
                    }
                } else {
                    if (!travelPositions.contains(p) && !queue.containsKey(p)) {
                        queue.put(p, distance + 1);
                    }
                }
            }
        }
    }

    public boolean canInsertItem(BlockPos pos, Direction direction) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof PipeTileEntity) {
            PipeTileEntity pipe = (PipeTileEntity) te;
            if (pipe.isExtractingItems(direction)) {
                return false;
            }
        }

        BlockEntity tileEntity = level.getBlockEntity(pos.relative(direction));
        if (tileEntity == null) {
            return false;
        }
        if (tileEntity instanceof PipeTileEntity) {
            return false;
        }
        return canInsert(tileEntity, direction.getOpposite());
    }
    public boolean canInsertEnergy(BlockPos pos, Direction direction) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof PipeTileEntity) {
            PipeTileEntity pipe = (PipeTileEntity) te;
            if (pipe.isExtractingEnergy(direction)) {
                return false;
            }
        }

        BlockEntity tileEntity = level.getBlockEntity(pos.relative(direction));
        if (tileEntity == null) {
            return false;
        }
        if (tileEntity instanceof PipeTileEntity) {
            return false;
        }
        return canInsert(tileEntity, direction.getOpposite());
    }
    public boolean canInsertFluid(BlockPos pos, Direction direction) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof PipeTileEntity) {
            PipeTileEntity pipe = (PipeTileEntity) te;
            if (pipe.isExtractingFluids(direction)) {
                return false;
            }
        }

        BlockEntity tileEntity = level.getBlockEntity(pos.relative(direction));
        if (tileEntity == null) {
            return false;
        }
        if (tileEntity instanceof PipeTileEntity) {
            return false;
        }
        return canInsert(tileEntity, direction.getOpposite());
    }

    public abstract boolean canInsert(BlockEntity tileEntity, Direction direction);

    @Override
    public void tick() {
        if (invalidateCountdown > 0) {
            invalidateCountdown--;
            if (invalidateCountdown <= 0) {
                connectionCache = new List[]{null, null, null};
            }
        }
    }

    public boolean isExtractingItems(Direction side) {
        return extractingSideItem[side.get3DDataValue()];
    }
    public boolean isExtractingFluids(Direction side) {
        return extractingSideFluid[side.get3DDataValue()];
    }
    public boolean isExtractingEnergy(Direction side) {
        return extractingSideEnergy[side.get3DDataValue()];
    }

    public boolean isExtractingEnergy() {
        for (boolean extract : extractingSideEnergy) {
            if (extract) {
                return true;
            }
        }
        return false;
    }
    public boolean isExtractingFluids() {
        for (boolean extract : extractingSideFluid) {
            if (extract) {
                return true;
            }
        }
        return false;
    }
    public boolean isExtractingItems() {
        for (boolean extract : extractingSideItem) {
            if (extract) {
                return true;
            }
        }
        return false;
    }

    public boolean hasReasonToStayItem() {
        if (isExtractingItems()) {
            return true;
        }
        for (boolean disconnected : disconnectedSides) {
            if (disconnected) {
                return true;
            }
        }
        return false;
    }
    public boolean hasReasonToStayFluid() {
        if (isExtractingFluids()) {
            return true;
        }
        for (boolean disconnected : disconnectedSides) {
            if (disconnected) {
                return true;
            }
        }
        return false;
    }
    public boolean hasReasonToStayEnergy() {
        if (isExtractingEnergy()) {
            return true;
        }
        for (boolean disconnected : disconnectedSides) {
            if (disconnected) {
                return true;
            }
        }
        return false;
    }

    public void setExtractingItem(Direction side, boolean extracting) {
        extractingSideItem[side.get3DDataValue()] = extracting;
        System.out.println(side.getName()+" set item extracting to "+extracting);
        setChanged();
    }
    public void setExtractingFluid(Direction side, boolean extracting) {
        extractingSideFluid[side.get3DDataValue()] = extracting;
        System.out.println(side.getName()+" set fluid extracting to "+extracting);
        setChanged();
    }
    public void setExtractingEnergy(Direction side, boolean extracting) {
        extractingSideEnergy[side.get3DDataValue()] = extracting;
        System.out.println(side.getName()+" set energy extracting to "+extracting);
        setChanged();
    }

    public boolean isDisconnected(Direction side) {
        return disconnectedSides[side.get3DDataValue()];
    }

    public void setDisconnected(Direction side, boolean disconnected) {
        disconnectedSides[side.get3DDataValue()] = disconnected;
        setChanged();
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        extractingSideItem = new boolean[Direction.values().length];
        extractingSideFluid = new boolean[Direction.values().length];
        extractingSideEnergy = new boolean[Direction.values().length];

        if(compound.contains("ExtractingSideItem")) {
            ListTag extractingListItems = compound.getList("ExtractingSideItem", Tag.TAG_BYTE);
            if (extractingListItems.size() >= extractingSideItem.length) {
                for (int i = 0; i < extractingSideItem.length; i++) {
                    ByteTag b = (ByteTag) extractingListItems.get(i);
                    extractingSideItem[i] = b.getAsByte() != 0;
                    System.out.println(i+" set item extracting to "+extractingSideItem[i]);
                }
            }
        }
        if(compound.contains("ExtractingSideFluid")) {
            ListTag extractingListFluids = compound.getList("ExtractingSideFluid", Tag.TAG_BYTE);
            if (extractingListFluids.size() >= extractingSideFluid.length) {
                for (int i = 0; i < extractingSideFluid.length; i++) {
                    ByteTag b = (ByteTag) extractingListFluids.get(i);
                    extractingSideFluid[i] = b.getAsByte() != 0;
                    System.out.println(i+" set fluid extracting to "+extractingSideFluid[i]);
                }
            }
        }
        if(compound.contains("ExtractingSideEnergy")) {
            ListTag extractingListEnergy = compound.getList("ExtractingSideEnergy", Tag.TAG_BYTE);
            if (extractingListEnergy.size() >= extractingSideEnergy.length) {
                for (int i = 0; i < extractingSideEnergy.length; i++) {
                    ByteTag b = (ByteTag) extractingListEnergy.get(i);
                    extractingSideEnergy[i] = b.getAsByte() != 0;
                    System.out.println(i+" set energy extracting to "+extractingSideEnergy[i]);
                }
            }
        }

        if(compound.contains("ExtractingSides")) { // backwards compatibility
            ListTag extractingListEnergy = compound.getList("ExtractingSides", Tag.TAG_BYTE);
            if (extractingListEnergy.size() >= extractingSideEnergy.length) {
                for (int i = 0; i < extractingSideEnergy.length; i++) {
                    ByteTag b = (ByteTag) extractingListEnergy.get(i);
                    extractingSideEnergy[i] = b.getAsByte() != 0;
                    extractingSideFluid[i] = b.getAsByte() != 0;
                    extractingSideItem[i] = b.getAsByte() != 0;
                    System.out.println(i+" set item extracting to "+extractingSideItem[i]);
                    System.out.println(i+" set fluid extracting to "+extractingSideFluid[i]);
                    System.out.println(i+" set energy extracting to "+extractingSideEnergy[i]);
                }
            }
        }
        disconnectedSides = new boolean[Direction.values().length];
        ListTag disconnectedList = compound.getList("DisconnectedSides", Tag.TAG_BYTE);
        if (disconnectedList.size() >= disconnectedSides.length) {
            for (int i = 0; i < disconnectedSides.length; i++) {
                ByteTag b = (ByteTag) disconnectedList.get(i);
                disconnectedSides[i] = b.getAsByte() != 0;
            }
        }
        invalidateCountdown = 5;
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);

        ListTag extractingListItem = new ListTag();
        for (boolean extractingSide : extractingSideItem) {
            extractingListItem.add(ByteTag.valueOf(extractingSide));
            System.out.println(this.getBlockPos().getX()+","+this.getBlockPos().getY()+","+this.getBlockPos().getZ()+": "+extractingListItem.size()+" saved item extracting to "+extractingSide);
        }
        compound.put("ExtractingSideItem", extractingListItem);

        ListTag extractingListFluid = new ListTag();
        for (boolean extractingSide : extractingSideFluid) {
            extractingListFluid.add(ByteTag.valueOf(extractingSide));
            System.out.println(this.getBlockPos().getX()+","+this.getBlockPos().getY()+","+this.getBlockPos().getZ()+": "+extractingListFluid.size()+" saved fluid extracting to "+extractingSide);
        }
        compound.put("ExtractingSideFluid", extractingListFluid);

        ListTag extractingListEnergy = new ListTag();
        for (boolean extractingSide : extractingSideEnergy) {
            extractingListEnergy.add(ByteTag.valueOf(extractingSide));
            System.out.println(this.getBlockPos().getX()+","+this.getBlockPos().getY()+","+this.getBlockPos().getZ()+": "+extractingListEnergy.size()+" saved energy extracting to "+extractingSide);
        }
        compound.put("ExtractingSideEnergy", extractingListEnergy);

        if(compound.contains("ExtractingSides")) compound.remove("ExtractingSides");

        ListTag disconnectedList = new ListTag();
        for (boolean disconnected : disconnectedSides) {
            disconnectedList.add(ByteTag.valueOf(disconnected));
        }
        compound.put("DisconnectedSides", disconnectedList);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void syncData(ServerPlayer player) {
        player.connection.send(getUpdatePacket());
    }

    public void syncData() {
        if (level == null || level.isClientSide) {
            return;
        }
        LevelChunk chunk = level.getChunkAt(getBlockPos());
        ((ServerChunkCache) level.getChunkSource()).chunkMap.getPlayers(chunk.getPos(), false).forEach(e -> e.connection.send(getUpdatePacket()));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag updateTag = super.getUpdateTag();
        saveAdditional(updateTag);
        return updateTag;
    }

    public static class Connection {
        private final BlockPos pos;
        private final Direction direction;
        private final int distance;

        public Connection(BlockPos pos, Direction direction, int distance) {
            this.pos = pos;
            this.direction = direction;
            this.distance = distance;
        }

        public BlockPos getPos() {
            return pos;
        }

        public Direction getDirection() {
            return direction;
        }

        public int getDistance() {
            return distance;
        }

        @Override
        public String toString() {
            return "Connection{" +
                    "pos=" + pos +
                    ", direction=" + direction +
                    ", distance=" + distance +
                    '}';
        }
    }

}
