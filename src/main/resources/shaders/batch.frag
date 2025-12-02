#version 330 core

in vec2 texCoords;
in vec4 vertexColor;
flat in float fragRenderType;
in vec2 rectSize;
flat in vec4 fragUvData;
flat in vec4 fragScissorRect;
flat in vec2 fragTransformData;
flat in float fragTextEffect;

uniform sampler2D sampler;
uniform float time;

void main() {
    if (fragScissorRect.x >= 0.0) {
        vec2 fragPos = gl_FragCoord.xy;

        if (fragPos.x < fragScissorRect.x ||
        fragPos.x > fragScissorRect.x + fragScissorRect.z ||
        fragPos.y < fragScissorRect.y ||
        fragPos.y > fragScissorRect.y + fragScissorRect.w) {
            discard;
        }
    }

    vec4 color;

    if (fragRenderType == 0.0) {
        color = texture2D(sampler, texCoords) * vertexColor;
        if (color.a < 0.00001)
        discard;
    }
    else if (fragRenderType == 1.0) {
        color = vertexColor;
    }
    else if (fragRenderType == 2.0) {
        float borderThickness = fragTransformData.x;

        vec2 uvSize = fragUvData.zw - fragUvData.xy;
        float borderWidthX = (borderThickness / rectSize.x) * uvSize.x;
        float borderWidthY = (borderThickness / rectSize.y) * uvSize.y;

        vec2 localUV = (texCoords - fragUvData.xy) / uvSize;

        if (localUV.x < borderWidthX / uvSize.x || localUV.x > 1.0 - borderWidthX / uvSize.x ||
        localUV.y < borderWidthY / uvSize.y || localUV.y > 1.0 - borderWidthY / uvSize.y) {
            color = vertexColor;
        } else {
            discard;
        }
    }
    else if (fragRenderType == 3.0) {
        color = vertexColor;
    }
    else if (fragRenderType == 4.0) {
        vec4 texel = texture2D(sampler, texCoords);
        vec2 texelSize = 1.0 / vec2(textureSize(sampler, 0));

        float outlineThickness = fragTransformData.x;

        float outlineAlpha = 0.0;

        if (outlineThickness > 0.1) {
            float maxAlpha = 0.0;
            float minDist = 999.0;

            int samples = int(ceil(outlineThickness)) + 1;

            for (int y = -samples; y <= samples; y++) {
                for (int x = -samples; x <= samples; x++) {
                    vec2 offset = vec2(float(x), float(y)) * texelSize;
                    float sampleAlpha = texture2D(sampler, texCoords + offset).a;

                    if (sampleAlpha > 0.95) {
                        float dist = length(vec2(x, y));
                        minDist = min(minDist, dist);
                        maxAlpha = max(maxAlpha, sampleAlpha);
                    }
                }
            }

            if (maxAlpha > 0.95 && minDist <= outlineThickness) {
                float fade = 1.0 - smoothstep(0.0, outlineThickness, minDist);
                outlineAlpha = fade;
            }
        }

        if (texel.a > 0.01) {
            vec3 effectColor;

            if (fragTextEffect == 1.0) {
                float hue = mod(time * 0.3, 1.0) * 6.0;

                if (hue < 1.0) {
                    effectColor = vec3(1.0, hue, 0.0);
                } else if (hue < 2.0) {
                    effectColor = vec3(2.0 - hue, 1.0, 0.0);
                } else if (hue < 3.0) {
                    effectColor = vec3(0.0, 1.0, hue - 2.0);
                } else if (hue < 4.0) {
                    effectColor = vec3(0.0, 4.0 - hue, 1.0);
                } else if (hue < 5.0) {
                    effectColor = vec3(hue - 4.0, 0.0, 1.0);
                } else {
                    effectColor = vec3(1.0, 0.0, 6.0 - hue);
                }
            }
            else if (fragTextEffect == 2.0) {
                float xPos = gl_FragCoord.x / 800.0;
                float shift = mod(time * 0.3, 1.0);
                float hue = mod(xPos * 1.0 + shift, 1.0) * 6.0;

                float segment = mod(hue, 1.0);

                if (hue < 1.0) {
                    effectColor = mix(vec3(1.0, 0.0, 0.0), vec3(1.0, 1.0, 0.0), segment);
                } else if (hue < 2.0) {
                    effectColor = mix(vec3(1.0, 1.0, 0.0), vec3(0.0, 1.0, 0.0), segment);
                } else if (hue < 3.0) {
                    effectColor = mix(vec3(0.0, 1.0, 0.0), vec3(0.0, 1.0, 1.0), segment);
                } else if (hue < 4.0) {
                    effectColor = mix(vec3(0.0, 1.0, 1.0), vec3(0.0, 0.0, 1.0), segment);
                } else if (hue < 5.0) {
                    effectColor = mix(vec3(0.0, 0.0, 1.0), vec3(1.0, 0.0, 1.0), segment);
                } else {
                    effectColor = mix(vec3(1.0, 0.0, 1.0), vec3(1.0, 0.0, 0.0), segment);
                }
            }
            else {
                effectColor = vertexColor.rgb;
            }

            vec4 textColor = vec4(effectColor, texel.a);

            if (outlineAlpha > 0.01) {
                vec4 outlineColor = vec4(0.0, 0.0, 0.0, outlineAlpha);
                color.rgb = mix(outlineColor.rgb, textColor.rgb, textColor.a);
                color.a = max(outlineColor.a, textColor.a);
            } else {
                color = textColor;
            }

            color.a = color.a * vertexColor.a;
        }

        else if (outlineAlpha > 0.0001) {
            color = vec4(0.0, 0.0, 0.0, outlineAlpha * vertexColor.a);
        } else {
            discard;
        }
    }
    else {
        color = vertexColor;
    }

    gl_FragColor = color;
}