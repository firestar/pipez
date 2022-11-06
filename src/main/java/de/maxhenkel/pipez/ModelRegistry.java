package de.maxhenkel.pipez;

import de.maxhenkel.corelib.CachedValue;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ForgeModelBakery;

public class ModelRegistry {

    public enum Model {
        ENERGY_PIPE_EXTRACT("block/energy_pipe_extract"),
        FLUID_PIPE_EXTRACT("block/fluid_pipe_extract"),
        GAS_PIPE_EXTRACT("block/gas_pipe_extract"),
        ITEM_PIPE_EXTRACT("block/item_pipe_extract"),
        UNIVERSAL_PIPE_EXTRACT("block/universal_pipe_extract"),
        UNIVERSAL_PIPE_EXTRACT_FLUID_ONLY("block/universal_pipe_extract_fluid"),
        UNIVERSAL_PIPE_EXTRACT_FLUID_AND_ITEM("block/universal_pipe_extract_fluid_item"),
        UNIVERSAL_PIPE_EXTRACT_ITEM_ONLY("block/universal_pipe_extract_item"),
        UNIVERSAL_PIPE_EXTRACT_ITEM_AND_ENERGY("block/universal_pipe_extract_item_energy"),
        UNIVERSAL_PIPE_EXTRACT_ENERGY_ONLY("block/universal_pipe_extract_energy"),
        UNIVERSAL_PIPE_EXTRACT_ENERGY_AND_FLUID("block/universal_pipe_extract_energy_fluid");

        private final ResourceLocation resource;
        private final CachedValue<BakedModel> cachedModel;

        Model(String name) {
            resource = new ResourceLocation(Main.MODID, name);
            cachedModel = new CachedValue<>(() -> {
                UnbakedModel modelOrMissing = ForgeModelBakery.instance().getModelOrMissing(resource);
                return modelOrMissing.bake(ForgeModelBakery.instance(), ForgeModelBakery.instance().getSpriteMap()::getSprite, BlockModelRotation.X0_Y0, resource);
            });
        }

        public ResourceLocation getResourceLocation() {
            return resource;
        }

        public CachedValue<BakedModel> getCachedModel() {
            return cachedModel;
        }
    }

    public static void onModelRegister(ModelRegistryEvent event) {
        for (Model model : Model.values()) {
            ForgeModelBakery.instance().addSpecialModel(model.getResourceLocation());
        }
    }

    public static void onModelBake(ModelBakeEvent event) {
        for (Model model : Model.values()) {
            model.getCachedModel().invalidate();
        }
    }

}
