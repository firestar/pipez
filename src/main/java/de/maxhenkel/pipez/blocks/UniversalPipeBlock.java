package de.maxhenkel.pipez.blocks;

import de.maxhenkel.pipez.Main;
import de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity;
import de.maxhenkel.pipez.blocks.tileentity.UniversalPipeTileEntity;
import de.maxhenkel.pipez.blocks.tileentity.UpgradeTileEntity;
import de.maxhenkel.pipez.capabilities.ModCapabilities;
import de.maxhenkel.pipez.gui.ExtractContainer;
import de.maxhenkel.pipez.gui.containerfactory.PipeContainerProvider;
import de.maxhenkel.pipez.items.UpgradeItem;
import de.maxhenkel.pipez.items.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;

public class UniversalPipeBlock extends PipeBlock {

    protected UniversalPipeBlock() {
        setRegistryName(new ResourceLocation(Main.MODID, "universal_pipe"));
    }

    @Override
    public boolean canConnectTo(LevelAccessor world, BlockPos pos, Direction facing) {
        BlockEntity te = world.getBlockEntity(pos.relative(facing));
        return te != null && (
                te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite()).isPresent()
                        || te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite()).isPresent()
                        || te.getCapability(CapabilityEnergy.ENERGY, facing.getOpposite()).isPresent()
                        || te.getCapability(ModCapabilities.GAS_HANDLER_CAPABILITY, facing.getOpposite()).isPresent()
        );
    }
    @Override
    public InteractionResult onWrenchClicked(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit, Direction side) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (side != null) {
            System.out.println("Hit Universal Wrench side not null");
            if (worldIn.getBlockState(pos.relative(side)).getBlock() != this) {
                boolean extractingItem = isExtractingItem(worldIn, pos, side);
                boolean extractingFluid = isExtractingFluid(worldIn, pos, side);
                boolean extractingEnergy = isExtractingEnergy(worldIn, pos, side);

                // None selected
                if(!extractingItem && !extractingFluid && !extractingEnergy){ // select All
                    System.out.println("Hit Universal Wrench select all");
                    setExtracting(worldIn, pos, side, true, 0);
                    setExtracting(worldIn, pos, side, true, 1);
                    setExtracting(worldIn, pos, side, true, 2);
                    setDisconnected(worldIn, pos, side, false);

                    // All selected
                }else if (extractingItem && extractingFluid && extractingEnergy){ // select Item
                    System.out.println("Hit Universal Wrench select item");
                    setExtracting(worldIn, pos, side, true, 0);
                    setExtracting(worldIn, pos, side, false, 1);
                    setExtracting(worldIn, pos, side, false, 2);
                    setDisconnected(worldIn, pos, side, false);

                    // Item Selected
                } else if(extractingItem && !extractingFluid && !extractingEnergy){ // select fluid
                    System.out.println("Hit Universal Wrench select fluid");
                    setExtracting(worldIn, pos, side, false, 0);
                    setExtracting(worldIn, pos, side, true, 1);
                    setDisconnected(worldIn, pos, side, false);

                    // Fluid Selected
                }else if(!extractingItem && extractingFluid && !extractingEnergy){ // select energy
                    System.out.println("Hit Universal Wrench select energy");
                    setExtracting(worldIn, pos, side, false, 1);
                    setExtracting(worldIn, pos, side, true, 2);
                    setDisconnected(worldIn, pos, side, false);

                    // Energy Selected
                }else if(!extractingItem && !extractingFluid && extractingEnergy){ // select disconnected
                    System.out.println("Hit Universal Wrench disconnect");
                    setExtracting(worldIn, pos, side, false, 0);
                    setExtracting(worldIn, pos, side, false, 1);
                    setExtracting(worldIn, pos, side, false, 2);
                    setDisconnected(worldIn, pos, side, true);
                }
            } else {
                System.out.println("Hit Universal Wrench disconnect");
                setDisconnected(worldIn, pos, side, true);
            }
        } else {
            System.out.println("Hit Universal Wrench side null");
            // Core
            side = hit.getDirection();
            if (worldIn.getBlockState(pos.relative(side)).getBlock() != this) {
                System.out.println("Hit Universal Wrench reconnect");
                setExtracting(worldIn, pos, side, false, 0);
                setExtracting(worldIn, pos, side, false, 1);
                setExtracting(worldIn, pos, side, false, 2);
                if (isAbleToConnect(worldIn, pos, side)) {
                    setDisconnected(worldIn, pos, side, false);
                }
            } else {
                System.out.println("Hit Universal Wrench reconnect");
                setDisconnected(worldIn, pos, side, false);
                setDisconnected(worldIn, pos.relative(side), side.getOpposite(), false);
            }
        }

        PipeTileEntity.markPipesDirty(worldIn, pos);
        return InteractionResult.SUCCESS;
    }
    @Override
    public InteractionResult onPipeSideForceActivated(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, @Nullable Direction side) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (WrenchItem.isWrench(heldItem)) {
            return onWrenchClicked(state, world, pos, player, hand, hit, side);
        } else if (heldItem.getItem() instanceof UpgradeItem && player.isShiftKeyDown() && side != null) {
            BlockEntity te = world.getBlockEntity(pos);
            if (!(te instanceof UpgradeTileEntity)) {
                return InteractionResult.PASS;
            }
            UpgradeTileEntity upgradeTe = (UpgradeTileEntity) te;
            ItemStack oldUpgrade;
            if (player.getAbilities().instabuild) {
                oldUpgrade = upgradeTe.setUpgradeItem(side, heldItem.copy().split(1));
            } else {
                oldUpgrade = upgradeTe.setUpgradeItem(side, heldItem.split(1));
            }
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, oldUpgrade);
            } else {
                if (!player.getInventory().add(oldUpgrade)) {
                    player.drop(oldUpgrade, true);
                }
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isPipe(LevelAccessor world, BlockPos pos, Direction facing) {
        BlockState state = world.getBlockState(pos.relative(facing));
        return state.getBlock().equals(this);
    }

    @Override
    BlockEntity createTileEntity(BlockPos pos, BlockState state) {
        return new UniversalPipeTileEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        Direction side = getSelection(state, worldIn, pos, player).getKey();
        if (side != null) {
            return onPipeSideActivated(state, worldIn, pos, player, handIn, hit, side);
        } else {
            return super.use(state, worldIn, pos, player, handIn, hit);
        }
    }
    @Override
    public InteractionResult onPipeSideActivated(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit, Direction direction) {
        BlockEntity tileEntity = worldIn.getBlockEntity(pos);
        if (tileEntity instanceof UniversalPipeTileEntity && (isExtractingEnergy(worldIn, pos, direction) || isExtractingItem(worldIn, pos, direction) || isExtractingFluid(worldIn, pos, direction))) {
            if (worldIn.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            UniversalPipeTileEntity pipe = (UniversalPipeTileEntity) tileEntity;
            PipeContainerProvider.openGui(player, pipe, direction, -1, (i, playerInventory, playerEntity) -> new ExtractContainer(i, playerInventory, pipe, direction, -1));
            return InteractionResult.SUCCESS;
        }
        return super.onPipeSideActivated(state, worldIn, pos, player, handIn, hit, direction);
    }
}
