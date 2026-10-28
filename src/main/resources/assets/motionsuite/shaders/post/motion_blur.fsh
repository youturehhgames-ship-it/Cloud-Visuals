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
    vec3 nudged = previous + sign(delta) * min(abs(delta), vec3(LSB));

    fragColor = vec4(mix(blended, nudged, stalled), 1.0);
}
