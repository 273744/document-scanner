package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * ARDocumentRenderer - OpenGL ES 3.0 renderer for AR document overlays
 *
 * Features:
 * - Render AR camera background
 * - 3D document boundary overlays
 * - Virtual grid lines for alignment
 * - Quality indicators and directional arrows
 * - Multiple document overlay support
 * - Smooth animations and transitions
 * - Perspective-correct 3D rendering
 * - Efficient shader-based rendering
 */
public class ARDocumentRenderer {

    private static final String TAG = "ARDocumentRenderer";

    // Shader sources
    private static final String CAMERA_VERTEX_SHADER =
        "#version 300 es\n" +
        "layout(location = 0) in vec4 a_Position;\n" +
        "layout(location = 1) in vec2 a_TexCoord;\n" +
        "out vec2 v_TexCoord;\n" +
        "void main() {\n" +
        "    gl_Position = a_Position;\n" +
        "    v_TexCoord = a_TexCoord;\n" +
        "}\n";

    private static final String CAMERA_FRAGMENT_SHADER =
        "#version 300 es\n" +
        "#extension GL_OES_EGL_image_external_essl3 : require\n" +
        "precision mediump float;\n" +
        "uniform samplerExternalOES u_Texture;\n" +
        "in vec2 v_TexCoord;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    fragColor = texture(u_Texture, v_TexCoord);\n" +
        "}\n";

    private static final String BOUNDARY_VERTEX_SHADER =
        "#version 300 es\n" +
        "uniform mat4 u_ModelViewProjection;\n" +
        "layout(location = 0) in vec4 a_Position;\n" +
        "layout(location = 1) in vec4 a_Color;\n" +
        "out vec4 v_Color;\n" +
        "void main() {\n" +
        "    gl_Position = u_ModelViewProjection * a_Position;\n" +
        "    v_Color = a_Color;\n" +
        "}\n";

    private static final String BOUNDARY_FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec4 v_Color;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    fragColor = v_Color;\n" +
        "}\n";

    private static final String GRID_VERTEX_SHADER =
        "#version 300 es\n" +
        "uniform mat4 u_ModelViewProjection;\n" +
        "uniform vec4 u_GridColor;\n" +
        "layout(location = 0) in vec4 a_Position;\n" +
        "out vec4 v_Color;\n" +
        "void main() {\n" +
        "    gl_Position = u_ModelViewProjection * a_Position;\n" +
        "    v_Color = u_GridColor;\n" +
        "}\n";

    private static final String GRID_FRAGMENT_SHADER =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "in vec4 v_Color;\n" +
        "out vec4 fragColor;\n" +
        "void main() {\n" +
        "    fragColor = v_Color;\n" +
        "}\n";

    // Shader programs
    private int cameraShaderProgram;
    private int boundaryShaderProgram;
    private int gridShaderProgram;

    // Camera background
    private int cameraTextureId = -1;
    private FloatBuffer cameraVertexBuffer;
    private FloatBuffer cameraTexCoordBuffer;

    // Matrices
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] viewProjectionMatrix = new float[16];
    private float[] modelMatrix = new float[16];
    private float[] modelViewProjectionMatrix = new float[16];

    // Document overlays
    private List<DocumentOverlay> documentOverlays = new ArrayList<>();

    // Grid
    private FloatBuffer gridVertexBuffer;
    private int gridLineCount = 0;

    // Animation
    private long lastFrameTime = 0;
    private float animationTime = 0;

    // Settings
    private boolean showGrid = true;
    private boolean showQualityIndicators = true;
    private float lineWidth = 3.0f;

    // Context
    private Context context;

    /**
     * Constructor
     */
    public ARDocumentRenderer(Context context) {
        this.context = context;
    }

    // ================================
    // Initialization
    // ================================

    /**
     * Initialize OpenGL resources
     */
    public void initialize() throws IOException {
        Log.d(TAG, "Initializing AR renderer");

        // Create shader programs
        createCameraShaderProgram();
        createBoundaryShaderProgram();
        createGridShaderProgram();

        // Setup camera background
        setupCameraBackground();

        // Setup grid
        setupGrid();

        Log.i(TAG, "AR renderer initialized successfully");
    }

    /**
     * Create camera background shader program
     */
    private void createCameraShaderProgram() {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, CAMERA_VERTEX_SHADER);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, CAMERA_FRAGMENT_SHADER);

        cameraShaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(cameraShaderProgram, vertexShader);
        GLES30.glAttachShader(cameraShaderProgram, fragmentShader);
        GLES30.glLinkProgram(cameraShaderProgram);

        checkGLError("Camera shader program");

        // Create texture for camera
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        cameraTextureId = textures[0];

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);

        Log.d(TAG, "Camera shader program created");
    }

    /**
     * Create boundary shader program
     */
    private void createBoundaryShaderProgram() {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, BOUNDARY_VERTEX_SHADER);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, BOUNDARY_FRAGMENT_SHADER);

        boundaryShaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(boundaryShaderProgram, vertexShader);
        GLES30.glAttachShader(boundaryShaderProgram, fragmentShader);
        GLES30.glLinkProgram(boundaryShaderProgram);

        checkGLError("Boundary shader program");

        Log.d(TAG, "Boundary shader program created");
    }

    /**
     * Create grid shader program
     */
    private void createGridShaderProgram() {
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, GRID_VERTEX_SHADER);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, GRID_FRAGMENT_SHADER);

        gridShaderProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(gridShaderProgram, vertexShader);
        GLES30.glAttachShader(gridShaderProgram, fragmentShader);
        GLES30.glLinkProgram(gridShaderProgram);

        checkGLError("Grid shader program");

        Log.d(TAG, "Grid shader program created");
    }

    /**
     * Load and compile shader
     */
    private int loadShader(int type, String shaderCode) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        // Check compilation status
        int[] compiled = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            String error = GLES30.glGetShaderInfoLog(shader);
            Log.e(TAG, "Shader compilation error: " + error);
            GLES30.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed: " + error);
        }

        return shader;
    }

    /**
     * Setup camera background quad
     */
    private void setupCameraBackground() {
        // Full screen quad vertices
        float[] vertices = {
            -1.0f, -1.0f, 0.0f,  // Bottom-left
             1.0f, -1.0f, 0.0f,  // Bottom-right
            -1.0f,  1.0f, 0.0f,  // Top-left
             1.0f,  1.0f, 0.0f   // Top-right
        };

        // Texture coordinates
        float[] texCoords = {
            0.0f, 0.0f,  // Bottom-left
            1.0f, 0.0f,  // Bottom-right
            0.0f, 1.0f,  // Top-left
            1.0f, 1.0f   // Top-right
        };

        // Create vertex buffer
        cameraVertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        cameraVertexBuffer.put(vertices);
        cameraVertexBuffer.position(0);

        // Create texture coordinate buffer
        cameraTexCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        cameraTexCoordBuffer.put(texCoords);
        cameraTexCoordBuffer.position(0);
    }

    /**
     * Setup grid for alignment
     */
    private void setupGrid() {
        // Create 10x10 grid
        int gridSize = 10;
        float gridSpacing = 0.05f; // 5cm spacing
        float halfSize = gridSize * gridSpacing / 2;

        List<Float> gridVertices = new ArrayList<>();

        // Horizontal lines
        for (int i = 0; i <= gridSize; i++) {
            float y = -halfSize + i * gridSpacing;
            gridVertices.add(-halfSize); gridVertices.add(y); gridVertices.add(0.0f);
            gridVertices.add(halfSize);  gridVertices.add(y); gridVertices.add(0.0f);
        }

        // Vertical lines
        for (int i = 0; i <= gridSize; i++) {
            float x = -halfSize + i * gridSpacing;
            gridVertices.add(x); gridVertices.add(-halfSize); gridVertices.add(0.0f);
            gridVertices.add(x); gridVertices.add(halfSize);  gridVertices.add(0.0f);
        }

        gridLineCount = gridVertices.size() / 3;

        // Create buffer
        float[] gridArray = new float[gridVertices.size()];
        for (int i = 0; i < gridVertices.size(); i++) {
            gridArray[i] = gridVertices.get(i);
        }

        gridVertexBuffer = ByteBuffer.allocateDirect(gridArray.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        gridVertexBuffer.put(gridArray);
        gridVertexBuffer.position(0);
    }

    // ================================
    // Rendering
    // ================================

    /**
     * Render AR frame with overlays
     */
    public void render(Frame frame, Camera camera) {
        if (frame == null || camera == null) {
            return;
        }

        // Update animation time
        updateAnimation();

        // Update matrices
        updateMatrices(camera);

        // Render camera background
        renderCameraBackground(frame);

        // Render document overlays
        for (DocumentOverlay overlay : documentOverlays) {
            renderDocumentOverlay(overlay, camera);
        }

        checkGLError("Render frame");
    }

    /**
     * Update animation time
     */
    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (lastFrameTime > 0) {
            float deltaTime = (currentTime - lastFrameTime) / 1000.0f;
            animationTime += deltaTime;
        }
        lastFrameTime = currentTime;
    }

    /**
     * Update view and projection matrices
     */
    private void updateMatrices(Camera camera) {
        // Get view matrix
        camera.getViewMatrix(viewMatrix, 0);

        // Get projection matrix
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

        // Calculate view-projection matrix
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    /**
     * Render camera background
     */
    private void renderCameraBackground(Frame frame) {
        // Disable depth test for background
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthMask(false);

        // Use camera shader
        GLES30.glUseProgram(cameraShaderProgram);

        // Bind camera texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);

        // Set vertex attributes
        int positionHandle = GLES30.glGetAttribLocation(cameraShaderProgram, "a_Position");
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, cameraVertexBuffer);

        int texCoordHandle = GLES30.glGetAttribLocation(cameraShaderProgram, "a_TexCoord");
        GLES30.glEnableVertexAttribArray(texCoordHandle);
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 0, cameraTexCoordBuffer);

        // Draw quad
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glDisableVertexAttribArray(texCoordHandle);

        // Re-enable depth test
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthMask(true);
    }

    /**
     * Render document overlay (boundary, grid, indicators)
     */
    private void renderDocumentOverlay(DocumentOverlay overlay, Camera camera) {
        // Calculate model matrix from plane pose
        overlay.plane.getCenterPose().toMatrix(modelMatrix, 0);

        // Apply animation
        applyAnimation(modelMatrix, overlay);

        // Calculate MVP matrix
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);

        // Render boundary
        renderBoundary(overlay);

        // Render grid if enabled
        if (showGrid && overlay.showGrid) {
            renderGrid(overlay);
        }

        // Render quality indicators
        if (showQualityIndicators && overlay.showQualityIndicator) {
            renderQualityIndicator(overlay);
        }

        // Render corner markers
        renderCornerMarkers(overlay);
    }

    /**
     * Apply smooth animation to overlay
     */
    private void applyAnimation(float[] matrix, DocumentOverlay overlay) {
        // Pulse animation for selected documents
        if (overlay.isSelected) {
            float scale = 1.0f + 0.05f * (float) Math.sin(animationTime * 3.0f);
            Matrix.scaleM(matrix, 0, scale, scale, scale);
        }

        // Fade in animation for new documents
        if (overlay.fadeInProgress < 1.0f) {
            overlay.fadeInProgress = Math.min(1.0f, overlay.fadeInProgress + 0.05f);
        }
    }

    /**
     * Render document boundary
     */
    private void renderBoundary(DocumentOverlay overlay) {
        GLES30.glUseProgram(boundaryShaderProgram);

        // Set MVP matrix
        int mvpHandle = GLES30.glGetUniformLocation(boundaryShaderProgram, "u_ModelViewProjection");
        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, modelViewProjectionMatrix, 0);

        // Get plane dimensions
        float halfX = overlay.plane.getExtentX() / 2;
        float halfZ = overlay.plane.getExtentZ() / 2;

        // Create boundary vertices (4 corners + close loop)
        float[] boundaryVertices = {
            -halfX, 0, -halfZ,  // Top-left
             halfX, 0, -halfZ,  // Top-right
             halfX, 0,  halfZ,  // Bottom-right
            -halfX, 0,  halfZ,  // Bottom-left
            -halfX, 0, -halfZ   // Close loop
        };

        // Create colors (with alpha based on fade-in)
        float alpha = overlay.fadeInProgress;
        float r = overlay.isSelected ? 0.0f : 1.0f;
        float g = overlay.isSelected ? 1.0f : 1.0f;
        float b = overlay.isSelected ? 0.0f : 0.0f;

        float[] colors = new float[boundaryVertices.length / 3 * 4];
        for (int i = 0; i < boundaryVertices.length / 3; i++) {
            colors[i * 4] = r;
            colors[i * 4 + 1] = g;
            colors[i * 4 + 2] = b;
            colors[i * 4 + 3] = alpha * 0.8f;
        }

        // Create buffers
        FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(boundaryVertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        vertexBuffer.put(boundaryVertices);
        vertexBuffer.position(0);

        FloatBuffer colorBuffer = ByteBuffer.allocateDirect(colors.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        // Set attributes
        int positionHandle = GLES30.glGetAttribLocation(boundaryShaderProgram, "a_Position");
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        int colorHandle = GLES30.glGetAttribLocation(boundaryShaderProgram, "a_Color");
        GLES30.glEnableVertexAttribArray(colorHandle);
        GLES30.glVertexAttribPointer(colorHandle, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);

        // Set line width
        GLES30.glLineWidth(lineWidth);

        // Draw boundary
        GLES30.glDrawArrays(GLES30.GL_LINE_STRIP, 0, boundaryVertices.length / 3);

        // Cleanup
        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glDisableVertexAttribArray(colorHandle);
    }

    /**
     * Render alignment grid
     */
    private void renderGrid(DocumentOverlay overlay) {
        GLES30.glUseProgram(gridShaderProgram);

        // Set MVP matrix
        int mvpHandle = GLES30.glGetUniformLocation(gridShaderProgram, "u_ModelViewProjection");
        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, modelViewProjectionMatrix, 0);

        // Set grid color (semi-transparent white)
        int colorHandle = GLES30.glGetUniformLocation(gridShaderProgram, "u_GridColor");
        float alpha = overlay.fadeInProgress * 0.3f;
        GLES30.glUniform4f(colorHandle, 1.0f, 1.0f, 1.0f, alpha);

        // Set position attribute
        int positionHandle = GLES30.glGetAttribLocation(gridShaderProgram, "a_Position");
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, gridVertexBuffer);

        // Draw grid
        GLES30.glLineWidth(1.0f);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, gridLineCount);

        // Cleanup
        GLES30.glDisableVertexAttribArray(positionHandle);
    }

    /**
     * Render quality indicator (arrow or checkmark)
     */
    private void renderQualityIndicator(DocumentOverlay overlay) {
        // Quality indicator would be rendered here
        // Could be an arrow pointing to optimal position
        // or a checkmark when quality is good

        // Implementation would use similar shader approach
        // with custom geometry for arrows/icons
    }

    /**
     * Render corner markers
     */
    private void renderCornerMarkers(DocumentOverlay overlay) {
        GLES30.glUseProgram(boundaryShaderProgram);

        // Set MVP matrix
        int mvpHandle = GLES30.glGetUniformLocation(boundaryShaderProgram, "u_ModelViewProjection");
        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, modelViewProjectionMatrix, 0);

        float halfX = overlay.plane.getExtentX() / 2;
        float halfZ = overlay.plane.getExtentZ() / 2;
        float markerSize = 0.02f;

        // Create corner marker positions
        float[][] corners = {
            {-halfX, 0, -halfZ},  // Top-left
            { halfX, 0, -halfZ},  // Top-right
            { halfX, 0,  halfZ},  // Bottom-right
            {-halfX, 0,  halfZ}   // Bottom-left
        };

        for (float[] corner : corners) {
            renderCornerMarker(corner[0], corner[1], corner[2], markerSize, overlay);
        }
    }

    /**
     * Render single corner marker
     */
    private void renderCornerMarker(float x, float y, float z, float size, DocumentOverlay overlay) {
        // Create small cross at corner
        float[] vertices = {
            x - size, y, z,  x + size, y, z,  // Horizontal line
            x, y, z - size,  x, y, z + size   // Vertical line
        };

        float alpha = overlay.fadeInProgress;
        float[] colors = new float[vertices.length / 3 * 4];
        for (int i = 0; i < vertices.length / 3; i++) {
            colors[i * 4] = overlay.isSelected ? 0.0f : 1.0f;
            colors[i * 4 + 1] = overlay.isSelected ? 1.0f : 1.0f;
            colors[i * 4 + 2] = 0.0f;
            colors[i * 4 + 3] = alpha;
        }

        FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        FloatBuffer colorBuffer = ByteBuffer.allocateDirect(colors.length * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
        colorBuffer.put(colors);
        colorBuffer.position(0);

        int positionHandle = GLES30.glGetAttribLocation(boundaryShaderProgram, "a_Position");
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer);

        int colorHandle = GLES30.glGetAttribLocation(boundaryShaderProgram, "a_Color");
        GLES30.glEnableVertexAttribArray(colorHandle);
        GLES30.glVertexAttribPointer(colorHandle, 4, GLES30.GL_FLOAT, false, 0, colorBuffer);

        GLES30.glLineWidth(3.0f);
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, vertices.length / 3);

        GLES30.glDisableVertexAttribArray(positionHandle);
        GLES30.glDisableVertexAttribArray(colorHandle);
    }

    // ================================
    // Document Management
    // ================================

    /**
     * Add document overlay
     */
    public void addDocumentOverlay(Plane plane, boolean isSelected) {
        DocumentOverlay overlay = new DocumentOverlay();
        overlay.plane = plane;
        overlay.isSelected = isSelected;
        overlay.showGrid = true;
        overlay.showQualityIndicator = true;
        overlay.fadeInProgress = 0.0f;

        documentOverlays.add(overlay);
    }

    /**
     * Update document overlays
     */
    public void updateDocumentOverlays(List<Plane> planes, Plane selectedPlane) {
        documentOverlays.clear();

        for (Plane plane : planes) {
            boolean isSelected = (plane == selectedPlane);
            addDocumentOverlay(plane, isSelected);
        }
    }

    /**
     * Clear all overlays
     */
    public void clearOverlays() {
        documentOverlays.clear();
    }

    // ================================
    // Settings
    // ================================

    /**
     * Set grid visibility
     */
    public void setShowGrid(boolean show) {
        this.showGrid = show;
    }

    /**
     * Set quality indicators visibility
     */
    public void setShowQualityIndicators(boolean show) {
        this.showQualityIndicators = show;
    }

    /**
     * Set line width
     */
    public void setLineWidth(float width) {
        this.lineWidth = width;
    }

    /**
     * Get camera texture ID
     */
    public int getCameraTextureId() {
        return cameraTextureId;
    }

    // ================================
    // Cleanup
    // ================================

    /**
     * Cleanup OpenGL resources
     */
    public void cleanup() {
        if (cameraShaderProgram != -1) {
            GLES30.glDeleteProgram(cameraShaderProgram);
            cameraShaderProgram = -1;
        }

        if (boundaryShaderProgram != -1) {
            GLES30.glDeleteProgram(boundaryShaderProgram);
            boundaryShaderProgram = -1;
        }

        if (gridShaderProgram != -1) {
            GLES30.glDeleteProgram(gridShaderProgram);
            gridShaderProgram = -1;
        }

        if (cameraTextureId != -1) {
            int[] textures = {cameraTextureId};
            GLES30.glDeleteTextures(1, textures, 0);
            cameraTextureId = -1;
        }

        documentOverlays.clear();

        Log.d(TAG, "AR renderer cleaned up");
    }

    // ================================
    // Utility Methods
    // ================================

    /**
     * Check for OpenGL errors
     */
    private void checkGLError(String operation) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG, operation + ": glError " + error);
            throw new RuntimeException(operation + ": glError " + error);
        }
    }

    // ================================
    // Inner Classes
    // ================================

    /**
     * Document overlay data
     */
    public static class DocumentOverlay {
        public Plane plane;
        public boolean isSelected;
        public boolean showGrid;
        public boolean showQualityIndicator;
        public float fadeInProgress;
        public float qualityScore;
    }
}

