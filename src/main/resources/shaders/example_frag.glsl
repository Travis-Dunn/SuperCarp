#version 120

uniform sampler2D diffuse;
varying vec3 vNormal;
varying vec2 vTexCoord;
varying vec3 vPosition;

void main() {
    vec4 texColor = texture2D(diffuse, vTexCoord);

    // Simple lighting calculation
    vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));
    vec3 normal = normalize(vNormal);
    float light = max(dot(normal, lightDir), 0.0);

    // Blue-ish color with lighting
    vec3 baseColor = vec3(0.3, 0.5, 0.8);
    vec3 ambient = texColor.rgb * 0.3;
    vec3 diffuse = texColor.rgb * light * 0.7;

    gl_FragColor = vec4(ambient + diffuse, 1.0);
    //gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
}