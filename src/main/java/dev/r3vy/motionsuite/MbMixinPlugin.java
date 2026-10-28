package dev.r3vy.motionsuite;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class MbMixinPlugin implements IMixinConfigPlugin {
    private static final String VULKAN_MIXIN = "vk.VkRsMixin";
    private static final Set<String> OPENGL_ONLY_MIXINS = Set.of("PostPassMixin", "GameRendererMixin");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean openGlOnly = OPENGL_ONLY_MIXINS.stream().anyMatch(mixinClassName::endsWith);
        return !openGlOnly || !VkLoader.present();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return VkLoader.present() ? List.of(VULKAN_MIXIN) : List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
