package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Pose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Document3DPreview - Advanced AR preview visualization with OpenGL ES
 *
 * Features:
 * - 3D wireframe box showing scan boundaries
 * - Perspective-corrected document preview
 * - Real-time transformation visualization
 * - Animated rotation and scaling
 * - Holographic-style effects
 * - Realistic lighting and shadows
 * - Smooth orientation transitions
 * - Custom GLSL shaders for effects
 */
public class Document3DPreview {

    private static final String TAG = "Document3DPreview";

    // Shader sources - Wireframe box
    private static final String WIREFRAME_VERTEX_SHADER =
        "#version 300 es\n" +
        "uniform mat4 u_MVP;\n" +
        "uniform float u_Time;\n" +
        "uniform float u_PulseIntensity;\n" +
        "layout(location = 0) in vec4 a_Position;\n" +
        "layout(location = 1) in vec4 a_Color;\n" +
        "out vec4 v_Color;\n" +
        "out float v_Pulse;\n" +
        "void main() {\n" +
        "    gl_Position = u_MVP * a_Position;\n" +
        "    v_Color = a_Color;\n" +
        "    v_Pulse = 0.5 + 0.5 * sin(u_Time * 3.0 + a_Position.y);\n" +
        "}\n";

    private static final String WIREFRAME_FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform float u_Alpha;\n" +
        "in vec4 v_Color;\n" +
        "in float v_Pulse;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec3 color = v_Color.rgb * (0.8 + 0.2 * v_Pulse);\n" +
        "    fragColor = vec4(color, v_Color.a * u_Alpha);\n" +
        "}\n";

    // Shader sources - Holographic effect
    private static final String HOLOGRAM_VERTEX_SHADER =
        "#version 300 es\n" +
        "uniform mat4 u_MVP;\n" +
        "uniform float u_Time;\n" +
        "layout(location = 0) in vec4 a_Position;\n" +
        "layout(location = 1) in vec2 a_TexCoord;\n" +
        "layout(location = 2) in vec3 a_Normal;\n" +
        "out vec2 v_TexCoord;\n" +
        "out vec3 v_Normal;\n" +
        "out float v_Scanline;\n" +
        "void main() {\n" +
        "    gl_Position = u_MVP * a_Position;\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "    v_Normal = a_Normal;\n" +
        "    v_Scanline = fract(a_Position.y * 10.0 + u_Time);\n" +
        "}\n";

    private static final String HOLOGRAM_FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform sampler2D u_Texture;\n" +
        "uniform float u_HologramIntensity;\n" +
        "uniform vec3 u_HologramColor;\n" +
        "in vec2 v_TexCoord;\n" +
        "in vec3 v_Normal;\n" +
        "in float v_Scanline;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    vec4 texColor = texture(u_Texture, v_TexCoord);\n" +
        "    float scanlineEffect = smoothstep(0.4, 0.6, v_Scanline);\n" +
        "    vec3 hologramTint = u_HologramColor * u_HologramIntensity;\n" +
        "    vec3 finalColor = mix(texColor.rgb, hologramTint, 0.3);\n" +
        "    finalColor += scanlineEffect * 0.1;\n" +
        "    float edge = pow(1.0 - abs(dot(v_Normal, vec3(0.0, 0.0, 1.0))), 2.0);\n" +
        "    finalColor += u_HologramColor * edge * 0.5;\n" +
        "    fragColor = vec4(finalColor, texColor.a * 0.8);\n" +
        "}\n";

    // Shader sources - Shadow plane
    private static final String SHADOW_VERTEX_SHADER =
        "#version 300 es\n" +
        "uniform mat4 u_MVP;\n" +
        "layout(location = 0) in vec4 a_Position;\n" +
        "out vec2 v_Position;\n" +
        "void main() {\n" +
        "    gl_Position = u_MVP * a_Position;\n" +
        "    v_Position = a_Position.xy;\n" +
        "}\n";

    private static final String SHADOW_FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform float u_ShadowIntensity;\n" +
        "in vec2 v_Position;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    float dist = length(v_Position);\n" +
        "    float alpha = u_ShadowIntensity * (1.0 - smoothstep(0.0, 1.0, dist));\n" +
        "    fragColor = vec4(0.0, 0.0, 0.0, alpha);\n" +
        "}\n";

    // Shader programs
    private int wireframeProgram;
    private int hologramProgram;
    private int shadowProgram;

    // Buffers
    private FloatBuffer wireframeVertexBuffer;
    private ShortBuffer wireframeIndexBuffer;
    private FloatBuffer previewVertexBuffer;
    private FloatBuffer previewTexCoordBuffer;
    private FloatBuffer previewNormalBuffer;
    private ShortBuffer previewIndexBuffer;
    private FloatBuffer shadowVertexBuffer;

    // Textures
    private int previewTextureId = -1;

    // Matrices
    private float[] modelMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] mvpMatrix = new float[16];
    private float[] tempMatrix = new float[16];

    // Animation state
    private float animationTime = 0f;
    private float rotationAngle = 0f;
    private float scaleValue = 1.0f;
    private float targetScale = 1.0f;
    private float pulseIntensity = 1.0f;
    private float hologramIntensity = 0.5f;
    private float shadowIntensity = 0.3f;

    // Preview state
    private float[] documentCorners3D = null;
    private boolean isAnimating = false;
    private boolean showHologram = true;
    private boolean showShadow = true;
    private boolean showWireframe = true;

    // Colors
    private float[] hologramColor = {0.0f, 0.7f, 1.0f}; // Cyan
    private float[] wireframeColor = {0.0f, 1.0f, 0.5f, 1.0f}; // Green

    // Dimensions
    private float boxDepth = 0.02f; // 2cm depth
    private float boxWidth = 0.21f; // A4 width
    private float boxHeight = 0.297f; // A4 height

    /**
     * Constructor
     */
    public Document3DPreview(Context context) {
        // Context stored if needed for resources
    }

    // ================================
    // Initialization
    // ================================

    /**
     * Initialize OpenGL resources
     */
    public void initialize() {
        Log.d(TAG, "Initializing Document3DPreview");

        try {
            // Create shader programs
            wireframeProgram = createShaderProgram(WIREFRAME_VERTEX_SHADER, WIREFRAME_FRAGMENT_SHADER);
            hologramProgram = createShaderProgram(HOLOGRAM_VERTEX_SHADER, HOLOGRAM_FRAGMENT_SHADER);
            shadowProgram = createShaderProgram(SHADOW_VERTEX_SHADER, SHADOW_FRAGMENT_SHADER);

            // Setup buffers
            setupWireframeBuffers();
            setupPreviewBuffers();
            setupShadowBuffers();

            // Initialize matrices
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.setIdentityM(viewMatrix, 0);
            Matrix.setIdentityM(projectionMatrix, 0);

            Log.i(TAG, "Document3DPreview initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Document3DPreview", e);
            throw new RuntimeException("Initialization failed", e);
        }
    }

    /**
     * Setup wireframe box buffers
     */
    private void setupWireframeBuffers() {
        // 8 vertices for box (cube corners)
        float[] vertices = {
            // Front face
            -boxWidth/2, -boxHeight/2, 0.0f,
             boxWidth/2, -boxHeight/2, 0.0f,
             boxWidth/2,  boxHeight/2, 0.0f,
            -boxWidth/2,  boxHeight/2, 0.0f,
            // Back face
            -boxWidth/2, -boxHeight/2, -boxDepth,
             boxWidth/2, -boxHeight/2, -boxDepth,
             boxWidth/2,  boxHeight/2, -boxDepth,
            -boxWidth/2,  boxHeight/2, -boxDepth
        };

        // Wireframe indices (12 edges of cube)
        short[] indices = {
            // Front face
            0, 1, 1, 2, 2, 3, 3, 0,
            // Back face
            4, 5, 5, 6, 6, 7, 7, 4,
            // Connecting edges
            0, 4, 1, 5, 2, 6, 3, 7
        };

        wireframeVertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        wireframeVertexBuffer.put(vertices);
        wireframeVertexBuffer.position(0);

        wireframeIndexBuffer = ByteBuffer.allocateDirect(indices.length * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer();
        wireframeIndexBuffer.put(indices);
        wireframeIndexBuffer.position(0);
    }

    /**
     * Setup preview plane buffers
     */
    private void setupPreviewBuffers() {
        // Quad vertices for document preview
        float[] vertices = {
            -boxWidth/2, -boxHeight/2, -boxDepth/2,
             boxWidth/2, -boxHeight/2, -boxDepth/2,
             boxWidth/2,  boxHeight/2, -boxDepth/2,
            -boxWidth/2,  boxHeight/2, -boxDepth/2
        };

        // Texture coordinates
        float[] texCoords = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f
        };

        // Normals (facing camera)
        float[] normals = {
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f
        };

        // Indices
        short[] indices = {0, 1, 2, 0, 2, 3};

        previewVertexBuffer = createFloatBuffer(vertices);
        previewTexCoordBuffer = createFloatBuffer(texCoords);
        previewNormalBuffer = createFloatBuffer(normals);
        previewIndexBuffer = createShortBuffer(indices);
    }

    /**
     * Setup shadow plane buffers
     */
    private void setupShadowBuffers() {
        // Shadow quad (slightly larger than document)
        float shadowScale = 1.1f;
        float[] vertices = {
            -boxWidth/2 * shadowScale, -boxHeight/2 * shadowScale, -boxDepth,
             boxWidth/2 * shadowScale, -boxHeight/2 * shadowScale, -boxDepth,
             boxWidth/2 * shadowScale,  boxHeight/2 * shadowScale, -boxDepth,
            -boxWidth/2 * shadowScale,  boxHeight/2 * shadowScale, -boxDepth
        };

        shadowVertexBuffer = createFloatBuffer(vertices);
    }

    // ================================
    // Rendering
    // ================================

    /**
     * Render preview with all effects
     */
    public void render(Camera camera, Pose documentPose, float deltaTime) {
        // Update animation
        updateAnimation(deltaTime);

        // Update matrices
        updateMatrices(camera, documentPose);

        // Enable blending
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // Render in order (back to front)
        if (showShadow) {
            renderShadow();
        }

        if (showWireframe) {
            renderWireframe();
        }

        if (showHologram && previewTextureId != -1) {
            renderHolographicPreview();
        }

        GLES30.glDisable(GLES30.GL_BLEND);

        checkGLError("render");
    }

    /**
     * Update animation state
     */
    private void updateAnimation(float deltaTime) {
        animationTime += deltaTime;

        // Smooth scale animation
        if (scaleValue != targetScale) {
            float scaleDiff = targetScale - scaleValue;
            scaleValue += scaleDiff * deltaTime * 5.0f; // Smooth interpolation

            if (Math.abs(scaleDiff) < 0.01f) {
                scaleValue = targetScale;
            }
        }

        // Rotation animation
        if (isAnimating) {
            rotationAngle += deltaTime * 30.0f; // 30 degrees per second
            if (rotationAngle >= 360.0f) {
                rotationAngle -= 360.0f;
            }
        }

        // Pulse animation
        pulseIntensity = 0.8f + 0.2f * (float) Math.sin(animationTime * 2.0f);
    }

    /**
     * Update transformation matrices
     */
    private void updateMatrices(Camera camera, Pose documentPose) {
        // Get camera matrices
        camera.getViewMatrix(viewMatrix, 0);
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

        // Setup model matrix from document pose
        if (documentPose != null) {
            documentPose.toMatrix(modelMatrix, 0);
        } else {
            Matrix.setIdentityM(modelMatrix, 0);
        }

        // Apply scale
        Matrix.scaleM(modelMatrix, 0, scaleValue, scaleValue, scaleValue);

        // Apply rotation
        if (rotationAngle != 0) {
            Matrix.rotateM(modelMatrix, 0, rotationAngle, 0, 1, 0);
        }

        // Calculate MVP matrix
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
    }

    /**
     * Render wireframe box
     */
    private void renderWireframe() {
        GLES30.glUseProgram(wireframeProgram);

        // Set uniforms
        int mvpHandle = GLES30.glGetUniformLocation(wireframeProgram, "u_MVP");
        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        int timeHandle = GLES30.glGetUniformLocation(wireframeProgram, "u_Time");
        GLES30.glUniform1f(timeHandle, animationTime);

        int pulseHandle = GLES30.glGetUniformLocation(wireframeProgram, "u_PulseIntensity");
        GLES30.glUniform1f(pulseHandle, pulseIntensity);

        int alphaHandle = GLES30.glGetUniformLocation(wireframeProgram, "u_Alpha");
        GLES30.glUniform1f(alphaHandle, 0.8f);

        // Set vertex attributes
        int posHandle = GLES30.glGetAttribLocation(wireframeProgram, "a_Position");
        GLES30.glEnableVertexAttribArray(posHandle);
        GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, 0, wireframeVertexBuffer);

        int colorHandle = GLES30.glGetAttribLocation(wireframeProgram, "a_Color");
        GLES30.glDisableVertexAttribArray(colorHandle);
        GLES30.glVertexAttrib4f(colorHandle, wireframeColor[0], wireframeColor[1],
            wireframeColor[2], wireframeColor[3]);

        // Set line width
        GLES30.glLineWidth(3.0f);

        // Draw wireframe
        GLES30.glDrawElements(GLES30.GL_LINES, 24, GLES30.GL_UNSIGNED_SHORT, wireframeIndexBuffer);

        GLES30.glDisableVertexAttribArray(posHandle);
    }

    /**
     * Render holographic document preview
     */
    private void renderHolographicPreview() {
        GLES30.glUseProgram(hologramProgram);

        // Set uniforms
        int mvpHandle = GLES30.glGetUniformLocation(hologramProgram, "u_MVP");
        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        int timeHandle = GLES30.glGetUniformLocation(hologramProgram, "u_Time");
        GLES30.glUniform1f(timeHandle, animationTime);

        int intensityHandle = GLES30.glGetUniformLocation(hologramProgram, "u_HologramIntensity");
        GLES30.glUniform1f(intensityHandle, hologramIntensity);

        int colorHandle = GLES30.glGetUniformLocation(hologramProgram, "u_HologramColor");
        GLES30.glUniform3fv(colorHandle, 1, hologramColor, 0);

        // Bind texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, previewTextureId);
        int texHandle = GLES30.glGetUniformLocation(hologramProgram, "u_Texture");
        GLES30.glUniform1i(texHandle, 0);

        // Set vertex attributes
        int posHandle = GLES30.glGetAttribLocation(hologramProgram, "a_Position");
        GLES30.glEnableVertexAttribArray(posHandle);
        GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, 0, previewVertexBuffer);

        int texCoordHandle = GLES30.glGetAttribLocation(hologramProgram, "a_TexCoord");
        GLES30.glEnableVertexAttribArray(texCoordHandle);
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 0, previewTexCoordBuffer);

        int normalHandle = GLES30.glGetAttribLocation(hologramProgram, "a_Normal");
        GLES30.glEnableVertexAttribArray(normalHandle);
        GLES30.glVertexAttribPointer(normalHandle, 3, GLES30.GL_FLOAT, false, 0, previewNormalBuffer);

        // Draw preview quad
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_SHORT, previewIndexBuffer);

        GLES30.glDisableVertexAttribArray(posHandle);
        GLES30.glDisableVertexAttribArray(texCoordHandle);
        GLES30.glDisableVertexAttribArray(normalHandle);
    }

    /**
     * Render shadow plane
     */
    private void renderShadow() {
        GLES30.glUseProgram(shadowProgram);

        // Set uniforms
        int mvpHandle = GLES30.glGetUniformLocation(shadowProgram, "u_MVP");
        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        int intensityHandle = GLES30.glGetUniformLocation(shadowProgram, "u_ShadowIntensity");
        GLES30.glUniform1f(intensityHandle, shadowIntensity);

        // Set vertex attributes
        int posHandle = GLES30.glGetAttribLocation(shadowProgram, "a_Position");
        GLES30.glEnableVertexAttribArray(posHandle);
        GLES30.glVertexAttribPointer(posHandle, 3, GLES30.GL_FLOAT, false, 0, shadowVertexBuffer);

        // Draw shadow quad
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4);

        GLES30.glDisableVertexAttribArray(posHandle);
    }

    // ================================
    // Animation Control
    // ================================

    /**
     * Start rotation animation
     */
    public void startRotationAnimation() {
        isAnimating = true;
    }

    /**
     * Stop rotation animation
     */
    public void stopRotationAnimation() {
        isAnimating = false;
        rotationAngle = 0f;
    }

    /**
     * Animate scale
     */
    public void animateScale(float targetScale, float duration) {
        this.targetScale = targetScale;
    }

    /**
     * Set instant scale
     */
    public void setScale(float scale) {
        this.scaleValue = scale;
        this.targetScale = scale;
    }

    // ================================
    // Texture Management
    // ================================

    /**
     * Set preview texture from bitmap
     */
    public void setPreviewTexture(Bitmap bitmap) {
        if (previewTextureId == -1) {
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            previewTextureId = textures[0];
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, previewTextureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);

        Log.d(TAG, "Preview texture set: " + bitmap.getWidth() + "x" + bitmap.getHeight());
    }

    // ================================
    // Customization
    // ================================

    public void setHologramColor(float r, float g, float b) {
        hologramColor[0] = r;
        hologramColor[1] = g;
        hologramColor[2] = b;
    }

    public void setWireframeColor(float r, float g, float b, float a) {
        wireframeColor[0] = r;
        wireframeColor[1] = g;
        wireframeColor[2] = b;
        wireframeColor[3] = a;
    }

    public void setHologramIntensity(float intensity) {
        this.hologramIntensity = Math.max(0, Math.min(1, intensity));
    }

    public void setShadowIntensity(float intensity) {
        this.shadowIntensity = Math.max(0, Math.min(1, intensity));
    }

    public void setShowHologram(boolean show) {
        this.showHologram = show;
    }

    public void setShowShadow(boolean show) {
        this.showShadow = show;
    }

    public void setShowWireframe(boolean show) {
        this.showWireframe = show;
    }

    // ================================
    // Utility Methods
    // ================================

    private int createShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource);

        int program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vertexShader);
        GLES30.glAttachShader(program, fragmentShader);
        GLES30.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String error = GLES30.glGetProgramInfoLog(program);
            GLES30.glDeleteProgram(program);
            throw new RuntimeException("Program link failed: " + error);
        }

        return program;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String error = GLES30.glGetShaderInfoLog(shader);
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed: " + error);
        }

        return shader;
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    private ShortBuffer createShortBuffer(short[] data) {
        ShortBuffer buffer = ByteBuffer.allocateDirect(data.length * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    private void checkGLError(String operation) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG, operation + ": glError " + error);
        }
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (wireframeProgram != 0) {
            GLES30.glDeleteProgram(wireframeProgram);
        }
        if (hologramProgram != 0) {
            GLES30.glDeleteProgram(hologramProgram);
        }
        if (shadowProgram != 0) {
            GLES30.glDeleteProgram(shadowProgram);
        }
        if (previewTextureId != -1) {
            GLES30.glDeleteTextures(1, new int[]{previewTextureId}, 0);
        }

        Log.d(TAG, "Document3DPreview cleaned up");
    }
}

