package dev.r3vy.motionsuite.vk;

import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.PipelineConfig;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.util.MappedBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

final class Pipes {
    private static final String SHADER_NAME = "mblur";
    private static final String SHADER_ROOT = "assets/motionsuite/shaders/basic/mblur/";
    private static final String BLEND_UNIFORM = "BlendFactor";
    private static final int BLEND_BLOCK_SIZE = 16;

    private static GraphicsPipeline pipe;
    private static MappedBuffer blendBuf;

    private Pipes() {
    }

    static void init() {
        if (pipe != null) {
            return;
        }
        MappedBuffer buffer = new MappedBuffer(BLEND_BLOCK_SIZE);
        String vertexSource = read(SHADER_ROOT + SHADER_NAME + ".vsh");
        String fragmentSource = read(SHADER_ROOT + SHADER_NAME + ".fsh");

        PipelineConfig config = PipelineConfig.builder()
                .withShader(SPIRVUtils.ShaderKind.VERTEX_SHADER, SHADER_NAME)
                .withShader(SPIRVUtils.ShaderKind.FRAGMENT_SHADER, SHADER_NAME)
                .addImageDescriptor(0, "sampler2D", "Sampler0", 0)
                .addImageDescriptor(1, "sampler2D", "Sampler1", 1)
                .addUB(PipelineConfig.UB.builder(2, PipelineConfig.getStageFromString("fragment"))
                        .addUniform("float", BLEND_UNIFORM)
                        .build())
                .build();

        Pipeline.Builder builder = new Pipeline.Builder(CustomVertexFormat.NONE, SHADER_NAME);
        builder.setUniformSupplierGetter(info -> supplierFor(info, buffer));
        builder.applyConfig(config);
        builder.setShaderSrc(SPIRVUtils.ShaderKind.VERTEX_SHADER, vertexSource);
        builder.setShaderSrc(SPIRVUtils.ShaderKind.FRAGMENT_SHADER, fragmentSource);

        pipe = builder.createGraphicsPipeline();
        blendBuf = buffer;
    }

    static GraphicsPipeline get() {
        return pipe;
    }

    static void putBlend(float blend) {
        if (blendBuf != null) {
            blendBuf.putFloat(0, blend);
        }
    }

    private static Supplier<MappedBuffer> supplierFor(Uniform.Info info, MappedBuffer buffer) {
        return BLEND_UNIFORM.equals(info.name) ? () -> buffer : null;
    }

    private static String read(String path) {
        try (InputStream stream = Pipes.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read resource " + path, e);
        }
    }
}
