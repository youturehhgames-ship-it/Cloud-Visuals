#version 450

layout(binding = 0) uniform sampler2D Sampler0;
layout(binding = 1) uniform sampler2D Sampler1;

layout(binding = 2) uniform MotionCfg {
    float BlendFactor;
} cfg;

layout(location = 0) in vec2 outUV;

layout(location = 0) out vec4 fragColor;

const float LSB = 1.0 / 255.0;

void main() {
    vec3 current = texture(Sampler0, outUV).rgb;
    vec3 previous = texture(Sampler1, outUV).rgb;
    float blend = clamp(cfg.BlendFactor, 0.0, 0.99);

    vec3 blended = sqrt(mix(current * current, previous * previous, blend));

    vec3 delta = current - previous;
    vec3 stalled = 1.0 - step(0.5 * LSB, abs(blended - previous));
    vec3 nudged = previous + sign(delta) * min(abs(delta), vec3(LSB));

    fragColor = vec4(mix(blended, nudged, stalled), 1.0);
}
