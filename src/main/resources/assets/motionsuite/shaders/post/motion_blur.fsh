#version 330

uniform sampler2D InSampler;
uniform sampler2D PrevSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform MotionCfg {
    float BlendFactor;
};

out vec4 fragColor;

const float LSB = 1.0 / 255.0;

void main() {
    vec3 current = texture(InSampler, texCoord).rgb;
    vec3 previous = texture(PrevSampler, texCoord).rgb;
    float blend = clamp(BlendFactor, 0.0, 0.99);

    vec3 blended = sqrt(mix(current * current, previous * previous, blend));

    vec3 delta = current - previous;
    vec3 stalled = 1.0 - step(0.5 * LSB, abs(blended - previous));
    // CloudVisuals: clear 8-bit quantization residue faster (>=10% per frame)
    // so a faint copy of an old frame cannot linger for seconds.
    vec3 stepSize = max(vec3(LSB), abs(delta) * 0.1);
    vec3 nudged = previous + sign(delta) * min(abs(delta), stepSize);

    fragColor = vec4(mix(blended, nudged, stalled), 1.0);
}
