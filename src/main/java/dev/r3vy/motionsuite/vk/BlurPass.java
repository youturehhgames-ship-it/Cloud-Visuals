package dev.r3vy.motionsuite.vk;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.r3vy.motionsuite.blur.BlurGate;
import dev.r3vy.motionsuite.blur.FrameBlend;
import dev.r3vy.motionsuite.config.Settings;
import net.minecraft.client.MinecraftClient;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.pass.MainPass;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlurPass implements MainPass {
    private static final Logger LOGGER = LoggerFactory.getLogger("motionsuite");
    private static final int TRIANGLE_VERTICES = 3;

    private static BlurPass active;

    private final MainPass inner;
    private Framebuffer hist;
    private Framebuffer scratch;
    private boolean seeded;
    private boolean faulted;

    public BlurPass(MainPass inner) {
        this.inner = inner;
        Pipes.init();
        active = this;
        Renderer.getInstance().addOnResizeCallback(this::rebuildBufs);
        rebuildBufs();
    }

    public static void onClientTick() {
        BlurPass pass = active;
        if (pass != null && !MinecraftClient.getInstance().isWindowFocused()) {
            pass.releaseBufs();
        }
    }

    public static void onDisconnect() {
        BlurPass pass = active;
        if (pass != null) {
            pass.releaseBufs();
        }
    }

    @Override
    public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
        inner.begin(commandBuffer, stack);
    }

    @Override
    public void end(VkCommandBuffer commandBuffer) {
        if (!shouldBlur()) {
            if (!BlurGate.inWorld() || !MinecraftClient.getInstance().isWindowFocused()) {
                seeded = false;
            }
            FrameBlend.reset();
            inner.end(commandBuffer);
            return;
        }
        if (!bufsReady()) {
            rebuildBufs();
        }
        if (hist == null || scratch == null || Pipes.get() == null) {
            inner.end(commandBuffer);
            return;
        }
        try {
            runBlur(commandBuffer);
        } catch (RuntimeException error) {
            faulted = true;
            releaseBufs();
            LOGGER.error("Motion blur pass failed and was disabled", error);
            inner.end(commandBuffer);
        }
    }

    @Override
    public void cleanUp() {
        releaseBufs();
        inner.cleanUp();
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void onResize() {
        inner.onResize();
        rebuildBufs();
    }

    @Override
    public void rebindMainTarget() {
        inner.rebindMainTarget();
    }

    @Override
    public void bindAsTexture() {
        inner.bindAsTexture();
    }

    @Override
    public GpuTexture getColorAttachment() {
        return inner.getColorAttachment();
    }

    @Override
    public GpuTextureView getColorAttachmentView() {
        return inner.getColorAttachmentView();
    }

    @Override
    public GpuTexture getDepthAttachment() {
        return inner.getDepthAttachment();
    }

    private void rebuildBufs() {
        SwapChain swapChain = Renderer.getInstance().getSwapChain();
        int width = swapChain.getWidth();
        int height = swapChain.getHeight();
        if (width == 0 || height == 0) {
            releaseBufs();
            return;
        }
        if (hist != null && scratch != null && hist.getWidth() == width && hist.getHeight() == height) {
            seeded = false;
            return;
        }
        if (hist != null) {
            hist.cleanUp();
        }
        if (scratch != null) {
            scratch.cleanUp();
        }
        hist = Framebuffer.builder(width, height, 1, false).build();
        scratch = Framebuffer.builder(width, height, 1, false).build();
        seeded = false;
    }

    private void releaseBufs() {
        if (hist != null) {
            hist.cleanUp();
            hist = null;
        }
        if (scratch != null) {
            scratch.cleanUp();
            scratch = null;
        }
        seeded = false;
    }

    private boolean shouldBlur() {
        return !faulted
                && Settings.BLUR.amount() > 0
                && BlurGate.inWorld()
                && MinecraftClient.getInstance().isWindowFocused();
    }

    private boolean bufsReady() {
        if (hist == null || scratch == null) {
            return false;
        }
        SwapChain swapChain = Renderer.getInstance().getSwapChain();
        return swapChain.getWidth() > 0
                && swapChain.getHeight() > 0
                && hist.getWidth() == swapChain.getWidth()
                && hist.getHeight() == swapChain.getHeight();
    }

    private void runBlur(VkCommandBuffer commandBuffer) {
        Renderer renderer = Renderer.getInstance();
        VulkanImage frame = renderer.getSwapChain().getColorAttachment();
        VulkanImage current = scratch.getColorAttachment();
        VulkanImage history = hist.getColorAttachment();
        float blend = FrameBlend.next();

        renderer.endRenderPass(commandBuffer);

        if (!seeded || blend <= 0.0f) {
            ImageUtil.blitFramebuffer(frame, history);
            seeded = true;
            inner.end(commandBuffer);
            return;
        }

        ImageUtil.blitFramebuffer(frame, current);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            frame.transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        }
        current.readOnlyLayout();
        history.readOnlyLayout();
        VTextureSelector.bindTexture(0, current);
        VTextureSelector.bindTexture(1, history);
        Pipes.putBlend(blend);

        inner.rebindMainTarget();
        draw(renderer, commandBuffer);
        renderer.endRenderPass(commandBuffer);

        ImageUtil.blitFramebuffer(frame, history);
        inner.end(commandBuffer);
    }

    private static void draw(Renderer renderer, VkCommandBuffer commandBuffer) {
        GraphicsPipeline pipeline = Pipes.get();
        VRenderSystem.disableCull();
        try {
            VRenderSystem.setPrimitiveTopologyGL(GL11.GL_TRIANGLES);
            renderer.bindGraphicsPipeline(pipeline);
            renderer.uploadAndBindUBOs(pipeline);
            VK11.vkCmdDraw(commandBuffer, TRIANGLE_VERTICES, 1, 0, 0);
        } finally {
            VRenderSystem.enableCull();
        }
    }
}
