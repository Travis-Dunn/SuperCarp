#version 120

attribute vec3 position;
attribute vec2 texCoord;
attribute vec3 normal;

uniform mat4 matMVP;

varying vec3 vNormal;
varying vec2 vTexCoord;
varying vec3 vPosition;

void main() {
    gl_Position = matMVP * vec4(position, 1.0);
    vNormal = normal;
    vTexCoord = texCoord;
    vPosition = position;
}