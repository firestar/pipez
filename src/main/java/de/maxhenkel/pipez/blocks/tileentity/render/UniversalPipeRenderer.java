package de.maxhenkel.pipez.blocks.tileentity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.maxhenkel.corelib.CachedValue;
import de.maxhenkel.pipez.ModelRegistry.Model;
import de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraftforge.client.model.data.EmptyModelData;

import java.util.List;

public class UniversalPipeRenderer extends PipeRenderer {
    protected CachedValue<BakedModel>[] cachedModels;
    public UniversalPipeRenderer(BlockEntityRendererProvider.Context renderer) {
        super(renderer);
        cachedModels = new CachedValue[]{
            getModelAll().getCachedModel(),
            getModelItem().getCachedModel(),
            getModelFluid().getCachedModel(),
            getModelEnergy().getCachedModel(),
            getModelItemEnergy().getCachedModel(),
            getModelFluidItem().getCachedModel(),
            getModelEnergyFluid().getCachedModel(),
        };
    }


    Model getModelAll() {
        return Model.UNIVERSAL_PIPE_EXTRACT;
    }
    Model getModelItem() {
        return Model.UNIVERSAL_PIPE_EXTRACT_ITEM_ONLY;
    }
    Model getModelItemEnergy() {
        return Model.UNIVERSAL_PIPE_EXTRACT_ITEM_AND_ENERGY;
    }
    Model getModelFluid() {
        return Model.UNIVERSAL_PIPE_EXTRACT_FLUID_ONLY;
    }
    Model getModelFluidItem() {
        return Model.UNIVERSAL_PIPE_EXTRACT_FLUID_AND_ITEM;
    }
    Model getModelEnergy() {
        return Model.UNIVERSAL_PIPE_EXTRACT_ENERGY_ONLY;
    }
    Model getModelEnergyFluid() {
        return Model.UNIVERSAL_PIPE_EXTRACT_ENERGY_AND_FLUID;
    }

    private void renderModel(Direction side, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, int cache ){
        BakedModel iBakedModel = cachedModels[cache].get();
        List<BakedQuad> quads = iBakedModel.getQuads(null, null, minecraft.level.random, EmptyModelData.INSTANCE);
        VertexConsumer b = buffer.getBuffer(RenderType.solid());
        renderExtractor(side, matrixStack, b, quads, combinedLight, combinedOverlay);
    }
    @Override
    public void render(PipeTileEntity pipe, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        for (Direction side : Direction.values()) {
            String code = (pipe.isExtractingItems(side)?1:0)+""+(pipe.isExtractingFluids(side)?1:0)+""+(pipe.isExtractingEnergy(side)?1:0);
            switch (code){
                //    ife
                case "111" -> renderModel(side, matrixStack, buffer, combinedLight, combinedOverlay, 0);
                case "100" -> renderModel(side, matrixStack, buffer, combinedLight, combinedOverlay, 1);
                case "010" -> renderModel(side, matrixStack, buffer, combinedLight, combinedOverlay, 2);
                case "001" -> renderModel(side, matrixStack, buffer, combinedLight, combinedOverlay, 3);
                case "101" -> renderModel(side, matrixStack, buffer, combinedLight, combinedOverlay, 4);
                case "011" -> renderModel(side, matrixStack, buffer, combinedLight, combinedOverlay, 5);
                case "110" -> renderModel(side, matrixStack, buffer, combinedLight, combinedOverlay, 6);
            }
        }
    }

    @Override
    Model getModel() {
        return Model.UNIVERSAL_PIPE_EXTRACT;
    }
}
