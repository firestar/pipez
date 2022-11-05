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
            getModelEnergy().getCachedModel()
        };
    }


    Model getModelAll() {
        return Model.UNIVERSAL_PIPE_EXTRACT;
    }
    Model getModelItem() {
        return Model.UNIVERSAL_PIPE_EXTRACT_ITEM_ONLY;
    }
    Model getModelFluid() {
        return Model.UNIVERSAL_PIPE_EXTRACT_FLUID_ONLY;
    }
    Model getModelEnergy() {
        return Model.UNIVERSAL_PIPE_EXTRACT_FLUID_ONLY;
    }

    @Override
    public void render(PipeTileEntity pipe, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        for (Direction side : Direction.values()) {
            VertexConsumer b = null;
            List<BakedQuad> quads = null;
            if (pipe.isExtractingItems(side) && pipe.isExtractingFluids(side) && pipe.isExtractingEnergy(side)) {
                BakedModel iBakedModel = cachedModels[0].get();
                quads = iBakedModel.getQuads(null, null, minecraft.level.random, EmptyModelData.INSTANCE);
                b = buffer.getBuffer(RenderType.solid());
            }else if (pipe.isExtractingItems(side)) {
                BakedModel iBakedModel = cachedModels[1].get();
                quads = iBakedModel.getQuads(null, null, minecraft.level.random, EmptyModelData.INSTANCE);
                buffer.getBuffer(RenderType.solid());
            }else if (pipe.isExtractingFluids(side)) {
                BakedModel iBakedModel = cachedModels[2].get();
                quads = iBakedModel.getQuads(null, null, minecraft.level.random, EmptyModelData.INSTANCE);
                b = buffer.getBuffer(RenderType.solid());
            }else if (pipe.isExtractingEnergy(side)) {
                BakedModel iBakedModel = cachedModels[3].get();
                quads = iBakedModel.getQuads(null, null, minecraft.level.random, EmptyModelData.INSTANCE);
                b = buffer.getBuffer(RenderType.solid());
            }
            renderExtractor(side, matrixStack, b, quads, combinedLight, combinedOverlay);
        }
    }

    @Override
    Model getModel() {
        return null;
    }
}
