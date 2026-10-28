package com.simplevisuals.mixin.client;

import com.simplevisuals.client.cit.CitRegistry;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel; // WICHTIG: Der korrekte Typ
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/model/ItemModel;update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/item/ItemModelManager;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/util/HeldItemContext;I)V"
            )
    )
    private void redirectModelUpdate(ItemModel instance, ItemRenderState state, ItemStack stack, ItemModelManager resolver, ItemDisplayContext displayContext, ClientWorld world, HeldItemContext heldItemContext, int seed) {

        // FIX: Variable Typ auf BlockStateModel geändert (passt jetzt zu CitRegistry)
        BlockStateModel customGeometry = CitRegistry.getModel(stack);

        if (customGeometry != null) {
            tryUpdateState(state, customGeometry, displayContext, stack, world, heldItemContext, seed);
        } else {
            instance.update(state, stack, resolver, displayContext, world, heldItemContext, seed);
        }
    }

    // FIX: Parameter Typ auf BlockStateModel geändert
    private void tryUpdateState(ItemRenderState state, BlockStateModel model, ItemDisplayContext ctx, ItemStack stack, ClientWorld world, HeldItemContext held, int seed) {
        // 1. Neue Layer im State erstellen
        ItemRenderState.LayerRenderState layer = state.newLayer();

        // 2. RenderLayer auf den Block-Atlas setzen (dort sind Item-Texturen)
        layer.setRenderLayer(RenderLayers.itemEntityTranslucentCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));

        // 3. Quads übertragen
        Random random = Random.create(seed);
        List<BlockModelPart> parts = model.getParts(random);

        for (BlockModelPart part : parts) {
            // WICHTIG: Quads für "keine Richtung"
            layer.getQuads().addAll(part.getQuads(null));

            // WICHTIG: Quads für alle Himmelsrichtungen (da unser JSON "north"/"south" nutzt)
            for (Direction dir : Direction.values()) {
                layer.getQuads().addAll(part.getQuads(dir));
            }
        }

        // 4. Glint (Verzauberung)
        if (stack.hasGlint()) {
            layer.setGlint(ItemRenderState.Glint.STANDARD);
        }
    }
}