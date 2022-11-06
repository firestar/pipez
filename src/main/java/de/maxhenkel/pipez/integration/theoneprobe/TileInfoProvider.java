package de.maxhenkel.pipez.integration.theoneprobe;

import de.maxhenkel.pipez.Main;
import de.maxhenkel.pipez.blocks.PipeBlock;
import de.maxhenkel.pipez.blocks.tileentity.PipeLogicTileEntity;
import de.maxhenkel.pipez.blocks.tileentity.types.PipeType;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedList;
import java.util.List;

public class TileInfoProvider implements IProbeInfoProvider {

    public static final ResourceLocation ID = new ResourceLocation(Main.MODID, "probeinfoprovider");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo info, Player player, Level world, BlockState state, IProbeHitData hitData) {
        BlockEntity te = world.getBlockEntity(hitData.getPos());

        if (state.getBlock() instanceof PipeBlock) {
            PipeBlock pipe = (PipeBlock) state.getBlock();
            Direction selectedSide = pipe.getSelection(state, world, hitData.getPos(), player).getKey();
            if (selectedSide == null) {
                return;
            }
            if (!(te instanceof PipeLogicTileEntity)) {
                return;
            }

            PipeLogicTileEntity pipeTile = (PipeLogicTileEntity) te;

            if (!pipeTile.isExtractingItems(selectedSide) && !pipeTile.isExtractingFluids(selectedSide) && !pipeTile.isExtractingEnergy(selectedSide)) {
                return;
            }

            ItemStack upgrade = pipeTile.getUpgradeItem(selectedSide);

            IProbeInfo i;
            List<PipeType<?>> types = pipeTile.getExtractingTypes(selectedSide);
            List<String> typeStr = new LinkedList<>();
            for (PipeType<?> type : types) {
                typeStr.add(type.getKeyText().getString());
            }
            if(typeStr.size()>0) {
                info = info.text(new TranslatableComponent("tooltip.pipez.types", typeStr.stream().reduce("", (out, r) -> out + (out.equals("") ? "" : ", ") + r)));
            }

            if (upgrade.isEmpty()) {
                i = info.text(new TranslatableComponent("tooltip.pipez.no_upgrade"));
            } else {
                i = info.horizontal()
                        .item(upgrade)
                        .vertical()
                        .itemLabel(upgrade);
            }

            for (PipeType<?> type : types) {
                if (pipeTile.isEnabled(selectedSide, type)) {
                    i = i.text(type.getTransferText(pipeTile.getUpgrade(selectedSide)));
                }
            }

        }
    }
}
