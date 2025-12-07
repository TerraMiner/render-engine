#version 330 core

layout(location = 0) in vec4 rectData;
layout(location = 1) in vec4 uvData;
layout(location = 2) in uint packedColor;
layout(location = 3) in vec2 transformData;
layout(location = 4) in float ignoreZoom;
layout(location = 5) in float ignoreCamera;
layout(location = 6) in float renderType;
layout(location = 7) in vec4 scissorRect;
layout(location = 8) in float textEffect;

const vec4 vertexData[6] = vec4[](
vec4(0.0, 0.0, 0.0, 0.0),
vec4(0.0, 1.0, 0.0, 1.0),
vec4(1.0, 1.0, 1.0, 1.0),

vec4(0.0, 0.0, 0.0, 0.0),
vec4(1.0, 1.0, 1.0, 1.0),
vec4(1.0, 0.0, 1.0, 0.0)
);

out vec2 texCoords;
out vec4 vertexColor;
flat out float fragRenderType;
out vec2 rectSize;
flat out vec4 fragUvData;
flat out vec4 fragScissorRect;
flat out vec2 fragTransformData;
flat out float fragTextEffect;

uniform mat4 projection;
uniform vec2 cameraScale;
uniform vec2 cameraCenter;
uniform vec2 cameraOffset;

void main() {
    vec4 vertex = vertexData[gl_VertexID];
    vec2 pos;

    vec2 actualCameraOffset = cameraOffset * (1.0 - ignoreCamera);
    vec2 actualCameraScale = mix(vec2(1.0), cameraScale, 1.0 - ignoreZoom);

    if (renderType == 3.0) {
        vec2 p1 = rectData.xy;
        vec2 p2 = rectData.zw;

        if (ignoreZoom == 0.0) {
            p1 = cameraCenter + (p1 - cameraCenter + actualCameraOffset) * actualCameraScale;
            p2 = cameraCenter + (p2 - cameraCenter + actualCameraOffset) * actualCameraScale;
        }

        vec2 dir = p2 - p1;
        float len = length(dir);

        if (len > 0.0) {
            dir = normalize(dir);
            vec2 perp = vec2(-dir.y, dir.x) * transformData.x * 0.5;

            if (gl_VertexID == 0) {
                pos = p1 - perp;
            } else if (gl_VertexID == 1) {
                pos = p1 + perp;
            } else if (gl_VertexID == 2) {
                pos = p2 + perp;
            } else if (gl_VertexID == 3) {
                pos = p1 - perp;
            } else if (gl_VertexID == 4) {
                pos = p2 + perp;
            } else {
                pos = p2 - perp;
            }
        } else {
            pos = p1;
        }
    } else {
        vec2 size = rectData.zw;
        rectSize = size;

        vec2 relPos = vertex.xy * size;

        if (transformData.y != 0.0) {
            float cosA = cos(transformData.y);
            float sinA = sin(transformData.y);
            vec2 center = size * 0.5;
            vec2 rotatedPos = vec2(
            cosA * (relPos.x - center.x) - sinA * (relPos.y - center.y),
            sinA * (relPos.x - center.x) + cosA * (relPos.y - center.y)
            );
            relPos = rotatedPos + center;
        }

        pos = rectData.xy + relPos;

        if (ignoreZoom == 0.0) {
            pos = cameraCenter + (pos - cameraCenter + actualCameraOffset) * actualCameraScale;
        }
    }

    pos = floor(pos);

    gl_Position = projection * vec4(pos, 1.0, 1.0);

    texCoords = uvData.xy + vertex.zw * (uvData.zw - uvData.xy);
    uint r = (packedColor >> 24u) & 0xFFu;
    uint g = (packedColor >> 16u) & 0xFFu;
    uint b = (packedColor >> 8u) & 0xFFu;
    uint a = packedColor & 0xFFu;
    vertexColor = vec4(float(r) / 255.0, float(g) / 255.0, float(b) / 255.0, float(a) / 255.0);
    fragRenderType = renderType;
    fragUvData = uvData;
    fragScissorRect = scissorRect;
    fragTransformData = transformData;
    fragTextEffect = textEffect;
}