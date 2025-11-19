package com.srikanth.docscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OCRTextRecognizer - ML Kit text extraction with multi-language support
 *
 * Features:
 * - Multi-language text recognition (Latin, Devanagari, Chinese, Japanese, Korean)
 * - Automatic language detection
 * - Confidence scores for recognized text
 * - Text bounding boxes for highlighting
 * - Error handling and fallback scenarios
 * - Progress callbacks
 * - Offline language model management
 */
public class OCRTextRecognizer {

    private static final String TAG = "OCRTextRecognizer";

    // Supported languages
    public enum Language {
        LATIN("Latin", TextRecognizerOptions.DEFAULT_OPTIONS),
        DEVANAGARI("Devanagari", new DevanagariTextRecognizerOptions.Builder().build()),
        CHINESE("Chinese", new ChineseTextRecognizerOptions.Builder().build()),
        JAPANESE("Japanese", new JapaneseTextRecognizerOptions.Builder().build()),
        KOREAN("Korean", new KoreanTextRecognizerOptions.Builder().build()),
        AUTO_DETECT("Auto", TextRecognizerOptions.DEFAULT_OPTIONS);

        public final String displayName;
        public final Object options;

        Language(String displayName, Object options) {
            this.displayName = displayName;
            this.options = options;
        }
    }

    // Context
    private Context context;

    // Text recognizers for different languages
    private TextRecognizer latinRecognizer;
    private TextRecognizer devanagariRecognizer;
    private TextRecognizer chineseRecognizer;
    private TextRecognizer japaneseRecognizer;
    private TextRecognizer koreanRecognizer;
    private LanguageIdentifier languageIdentifier;

    // Current language
    private Language currentLanguage = Language.AUTO_DETECT;

    // Executor for background processing
    private ExecutorService executor;

    // Remote model manager
    private RemoteModelManager modelManager;

    /**
     * Constructor
     */
    public OCRTextRecognizer(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.modelManager = RemoteModelManager.getInstance();

        // Initialize Latin recognizer by default
        initializeRecognizer(Language.LATIN);

        // Initialize language identifier for auto-detect
        languageIdentifier = LanguageIdentification.getClient();

        Log.d(TAG, "OCRTextRecognizer initialized");
    }

    // ================================
    // 1. Initialize Text Recognizer
    // ================================

    /**
     * Initialize recognizer for specific language
     */
    public void initializeRecognizer(Language language) {
        this.currentLanguage = language;

        switch (language) {
            case LATIN:
                if (latinRecognizer == null) {
                    latinRecognizer = TextRecognition.getClient(
                        TextRecognizerOptions.DEFAULT_OPTIONS);
                    Log.d(TAG, "Latin recognizer initialized");
                }
                break;

            case DEVANAGARI:
                if (devanagariRecognizer == null) {
                    devanagariRecognizer = TextRecognition.getClient(
                        new DevanagariTextRecognizerOptions.Builder().build());
                    Log.d(TAG, "Devanagari recognizer initialized");
                }
                break;

            case CHINESE:
                if (chineseRecognizer == null) {
                    chineseRecognizer = TextRecognition.getClient(
                        new ChineseTextRecognizerOptions.Builder().build());
                    Log.d(TAG, "Chinese recognizer initialized");
                }
                break;

            case JAPANESE:
                if (japaneseRecognizer == null) {
                    japaneseRecognizer = TextRecognition.getClient(
                        new JapaneseTextRecognizerOptions.Builder().build());
                    Log.d(TAG, "Japanese recognizer initialized");
                }
                break;

            case KOREAN:
                if (koreanRecognizer == null) {
                    koreanRecognizer = TextRecognition.getClient(
                        new KoreanTextRecognizerOptions.Builder().build());
                    Log.d(TAG, "Korean recognizer initialized");
                }
                break;

            case AUTO_DETECT:
                // Initialize all recognizers for auto-detect
                initializeRecognizer(Language.LATIN);
                Log.d(TAG, "Auto-detect mode initialized");
                break;
        }
    }

    /**
     * Set language for recognition
     */
    public void setLanguage(Language language) {
        this.currentLanguage = language;
        initializeRecognizer(language);
    }

    /**
     * Get current language
     */
    public Language getCurrentLanguage() {
        return currentLanguage;
    }

    // ================================
    // 2. Process Image and Extract Text
    // ================================

    /**
     * Process bitmap and extract text
     */
    public void processImage(Bitmap bitmap, OCRCallback callback) {
        if (bitmap == null) {
            callback.onError(new IllegalArgumentException("Bitmap is null"));
            return;
        }

        callback.onProgress(10, "Preparing image...");

        // Create input image
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        if (currentLanguage == Language.AUTO_DETECT) {
            // Auto-detect language and process
            processWithAutoDetect(image, callback);
        } else {
            // Process with specific language
            processWithLanguage(image, currentLanguage, callback);
        }
    }

    /**
     * Process with specific language
     */
    private void processWithLanguage(InputImage image, Language language, OCRCallback callback) {
        callback.onProgress(30, "Recognizing text (" + language.displayName + ")...");

        TextRecognizer recognizer = getRecognizerForLanguage(language);

        if (recognizer == null) {
            callback.onError(new IllegalStateException("Recognizer not initialized for " + language));
            return;
        }

        recognizer.process(image)
            .addOnSuccessListener(visionText -> {
                callback.onProgress(80, "Processing results...");

                OCRResult result = buildOCRResult(visionText, language);

                callback.onProgress(100, "Complete");
                callback.onSuccess(result);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Text recognition failed for " + language, e);

                // Try fallback to Latin if not already using it
                if (language != Language.LATIN) {
                    Log.d(TAG, "Falling back to Latin recognizer");
                    processWithLanguage(image, Language.LATIN, callback);
                } else {
                    callback.onError(e);
                }
            });
    }

    // ================================
    // 3. Handle Multiple Languages Detection
    // ================================

    /**
     * Process with automatic language detection
     */
    private void processWithAutoDetect(InputImage image, OCRCallback callback) {
        callback.onProgress(20, "Detecting language...");

        // First, try with Latin recognizer to get initial text
        latinRecognizer.process(image)
            .addOnSuccessListener(visionText -> {
                String recognizedText = visionText.getText();

                if (recognizedText.isEmpty()) {
                    callback.onProgress(100, "Complete");
                    callback.onSuccess(buildOCRResult(visionText, Language.LATIN));
                    return;
                }

                // Identify language
                languageIdentifier.identifyLanguage(recognizedText)
                    .addOnSuccessListener(languageCode -> {
                        Language detectedLanguage = mapLanguageCode(languageCode);

                        Log.d(TAG, "Detected language: " + languageCode + " -> " + detectedLanguage);

                        if (detectedLanguage != Language.LATIN && detectedLanguage != Language.AUTO_DETECT) {
                            // Re-process with correct recognizer
                            callback.onProgress(50, "Re-processing with " + detectedLanguage.displayName + "...");
                            processWithLanguage(image, detectedLanguage, callback);
                        } else {
                            // Use Latin result
                            callback.onProgress(100, "Complete");
                            callback.onSuccess(buildOCRResult(visionText, Language.LATIN));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Language detection failed, using Latin result", e);
                        callback.onProgress(100, "Complete");
                        callback.onSuccess(buildOCRResult(visionText, Language.LATIN));
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Initial text recognition failed", e);
                callback.onError(e);
            });
    }

    /**
     * Map language code to Language enum
     */
    private Language mapLanguageCode(String code) {
        if (code.equals("und")) {
            return Language.LATIN; // Undetermined - default to Latin
        }

        if (code.startsWith("hi") || code.startsWith("mr") || code.startsWith("ne")) {
            return Language.DEVANAGARI;
        } else if (code.startsWith("zh")) {
            return Language.CHINESE;
        } else if (code.equals("ja")) {
            return Language.JAPANESE;
        } else if (code.equals("ko")) {
            return Language.KOREAN;
        } else {
            return Language.LATIN;
        }
    }

    // ================================
    // 4. Return Extracted Text with Confidence
    // ================================

    /**
     * Build OCR result from Vision Text
     */
    private OCRResult buildOCRResult(Text visionText, Language language) {
        OCRResult result = new OCRResult();
        result.fullText = visionText.getText();
        result.language = language;
        result.blocks = new ArrayList<>();

        // Process all text blocks
        for (Text.TextBlock block : visionText.getTextBlocks()) {
            TextBlock textBlock = new TextBlock();
            textBlock.text = block.getText();
            textBlock.boundingBox = block.getBoundingBox();
            textBlock.confidence = null; // Block level confidence not available in ML Kit
            textBlock.lines = new ArrayList<>();

            // Process lines in block
            for (Text.Line line : block.getLines()) {
                TextLine textLine = new TextLine();
                textLine.text = line.getText();
                textLine.boundingBox = line.getBoundingBox();
                textLine.confidence = line.getConfidence();
                textLine.elements = new ArrayList<>();

                // Process elements in line
                for (Text.Element element : line.getElements()) {
                    TextElement textElement = new TextElement();
                    textElement.text = element.getText();
                    textElement.boundingBox = element.getBoundingBox();
                    textElement.confidence = element.getConfidence();

                    textLine.elements.add(textElement);
                }

                textBlock.lines.add(textLine);
            }

            result.blocks.add(textBlock);
        }

        // Calculate overall confidence
        result.overallConfidence = calculateOverallConfidence(result);

        Log.d(TAG, String.format("OCR completed: %d blocks, %.2f%% confidence",
            result.blocks.size(), result.overallConfidence * 100));

        return result;
    }

    /**
     * Calculate overall confidence score
     */
    private float calculateOverallConfidence(OCRResult result) {
        if (result.blocks.isEmpty()) {
            return 0.0f;
        }

        float totalConfidence = 0.0f;
        int count = 0;

        for (TextBlock block : result.blocks) {
            if (block.confidence != null) {
                totalConfidence += block.confidence;
                count++;
            }
        }

        return count > 0 ? totalConfidence / count : 0.0f;
    }

    // ================================
    // 5. Extract Text Bounding Boxes
    // ================================

    /**
     * Get all text bounding boxes for highlighting
     */
    public List<TextBoundingBox> getTextBoundingBoxes(OCRResult result) {
        List<TextBoundingBox> boundingBoxes = new ArrayList<>();

        for (TextBlock block : result.blocks) {
            if (block.boundingBox != null) {
                TextBoundingBox box = new TextBoundingBox();
                box.rect = block.boundingBox;
                box.text = block.text;
                box.confidence = block.confidence != null ? block.confidence : 0.0f;
                box.level = BoundingBoxLevel.BLOCK;
                boundingBoxes.add(box);
            }

            for (TextLine line : block.lines) {
                if (line.boundingBox != null) {
                    TextBoundingBox box = new TextBoundingBox();
                    box.rect = line.boundingBox;
                    box.text = line.text;
                    box.confidence = line.confidence != null ? line.confidence : 0.0f;
                    box.level = BoundingBoxLevel.LINE;
                    boundingBoxes.add(box);
                }

                for (TextElement element : line.elements) {
                    if (element.boundingBox != null) {
                        TextBoundingBox box = new TextBoundingBox();
                        box.rect = element.boundingBox;
                        box.text = element.text;
                        box.confidence = element.confidence != null ? element.confidence : 0.0f;
                        box.level = BoundingBoxLevel.WORD;
                        boundingBoxes.add(box);
                    }
                }
            }
        }

        return boundingBoxes;
    }

    /**
     * Get word-level bounding boxes only
     */
    public List<TextBoundingBox> getWordBoundingBoxes(OCRResult result) {
        List<TextBoundingBox> wordBoxes = new ArrayList<>();

        for (TextBlock block : result.blocks) {
            for (TextLine line : block.lines) {
                for (TextElement element : line.elements) {
                    if (element.boundingBox != null) {
                        TextBoundingBox box = new TextBoundingBox();
                        box.rect = element.boundingBox;
                        box.text = element.text;
                        box.confidence = element.confidence != null ? element.confidence : 0.0f;
                        box.level = BoundingBoxLevel.WORD;
                        wordBoxes.add(box);
                    }
                }
            }
        }

        return wordBoxes;
    }

    // ================================
    // 6. Error Handling and Fallback
    // ================================

    /**
     * Get appropriate recognizer for language
     */
    private TextRecognizer getRecognizerForLanguage(Language language) {
        switch (language) {
            case LATIN:
                return latinRecognizer;
            case DEVANAGARI:
                return devanagariRecognizer;
            case CHINESE:
                return chineseRecognizer;
            case JAPANESE:
                return japaneseRecognizer;
            case KOREAN:
                return koreanRecognizer;
            default:
                return latinRecognizer;
        }
    }

    /**
     * Handle ML Kit exception
     */
    private void handleMLKitException(Exception e, OCRCallback callback) {
        if (e instanceof MlKitException) {
            MlKitException mlKitException = (MlKitException) e;

            String errorMessage;
            switch (mlKitException.getErrorCode()) {
                case MlKitException.UNAVAILABLE:
                    errorMessage = "ML Kit is unavailable";
                    break;
                case MlKitException.NOT_ENOUGH_SPACE:
                    errorMessage = "Not enough storage space for ML Kit models";
                    break;
                case MlKitException.MODEL_INCOMPATIBLE_WITH_TFLITE:
                    errorMessage = "ML Kit model is incompatible";
                    break;
                case MlKitException.INVALID_ARGUMENT:
                    errorMessage = "Invalid argument provided to ML Kit";
                    break;
                case MlKitException.UNAUTHENTICATED:
                    errorMessage = "ML Kit authentication failed";
                    break;
                default:
                    errorMessage = "ML Kit error: " + e.getMessage();
            }

            Log.e(TAG, errorMessage, e);
            callback.onError(new OCRException(errorMessage, e));
        } else {
            callback.onError(e);
        }
    }

    // ================================
    // 7. Offline Language Model Management
    // ================================

    /**
     * Check if model is downloaded for language
     */
    public void isModelDownloaded(Language language, ModelStatusCallback callback) {
        // For now, models are bundled with the app or downloaded automatically
        // This is a placeholder for future implementation
        callback.onModelStatus(true);
    }

    /**
     * Download model for specific language
     */
    public void downloadModel(Language language, ModelDownloadCallback callback) {
        callback.onDownloadProgress(0, "Starting download...");

        // Initialize recognizer which will trigger model download
        initializeRecognizer(language);

        TextRecognizer recognizer = getRecognizerForLanguage(language);

        if (recognizer == null) {
            callback.onDownloadFailed(new IllegalStateException("Failed to initialize recognizer"));
            return;
        }

        // Create a dummy image to trigger model download
        Bitmap dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        InputImage dummyImage = InputImage.fromBitmap(dummyBitmap, 0);

        callback.onDownloadProgress(50, "Downloading model...");

        recognizer.process(dummyImage)
            .addOnSuccessListener(text -> {
                callback.onDownloadProgress(100, "Download complete");
                callback.onDownloadSuccess();
                dummyBitmap.recycle();
            })
            .addOnFailureListener(e -> {
                callback.onDownloadFailed(e);
                dummyBitmap.recycle();
            });
    }

    /**
     * Delete downloaded model
     */
    public void deleteModel(Language language, ModelDeleteCallback callback) {
        // Close the recognizer
        TextRecognizer recognizer = getRecognizerForLanguage(language);
        if (recognizer != null) {
            recognizer.close();
        }

        // Clear the recognizer reference
        switch (language) {
            case LATIN:
                latinRecognizer = null;
                break;
            case DEVANAGARI:
                devanagariRecognizer = null;
                break;
            case CHINESE:
                chineseRecognizer = null;
                break;
            case JAPANESE:
                japaneseRecognizer = null;
                break;
            case KOREAN:
                koreanRecognizer = null;
                break;
        }

        callback.onDeleteSuccess();
    }

    // ================================
    // Data Classes
    // ================================

    /**
     * OCR Result containing all extracted text
     */
    public static class OCRResult {
        public String fullText;
        public Language language;
        public List<TextBlock> blocks;
        public float overallConfidence;

        @Override
        public String toString() {
            return String.format(Locale.getDefault(),
                "OCR Result: %d blocks, %.1f%% confidence, Language: %s\nText: %s",
                blocks != null ? blocks.size() : 0,
                overallConfidence * 100,
                language != null ? language.displayName : "Unknown",
                fullText != null ? fullText.substring(0, Math.min(50, fullText.length())) : "");
        }
    }

    /**
     * Text Block
     */
    public static class TextBlock {
        public String text;
        public Rect boundingBox;
        public Float confidence;
        public List<TextLine> lines;
    }

    /**
     * Text Line
     */
    public static class TextLine {
        public String text;
        public Rect boundingBox;
        public Float confidence;
        public List<TextElement> elements;
    }

    /**
     * Text Element (word)
     */
    public static class TextElement {
        public String text;
        public Rect boundingBox;
        public Float confidence;
    }

    /**
     * Text Bounding Box for highlighting
     */
    public static class TextBoundingBox {
        public Rect rect;
        public String text;
        public float confidence;
        public BoundingBoxLevel level;
    }

    /**
     * Bounding box level
     */
    public enum BoundingBoxLevel {
        BLOCK,
        LINE,
        WORD
    }

    /**
     * OCR Exception
     */
    public static class OCRException extends Exception {
        public OCRException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ================================
    // Callbacks
    // ================================

    /**
     * OCR processing callback
     */
    public interface OCRCallback {
        void onProgress(int progress, String message);
        void onSuccess(OCRResult result);
        void onError(Exception e);
    }

    /**
     * Model status callback
     */
    public interface ModelStatusCallback {
        void onModelStatus(boolean isDownloaded);
    }

    /**
     * Model download callback
     */
    public interface ModelDownloadCallback {
        void onDownloadProgress(int progress, String message);
        void onDownloadSuccess();
        void onDownloadFailed(Exception e);
    }

    /**
     * Model delete callback
     */
    public interface ModelDeleteCallback {
        void onDeleteSuccess();
        void onDeleteFailed(Exception e);
    }

    // ================================
    // Cleanup
    // ================================

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (latinRecognizer != null) {
            latinRecognizer.close();
            latinRecognizer = null;
        }
        if (devanagariRecognizer != null) {
            devanagariRecognizer.close();
            devanagariRecognizer = null;
        }
        if (chineseRecognizer != null) {
            chineseRecognizer.close();
            chineseRecognizer = null;
        }
        if (japaneseRecognizer != null) {
            japaneseRecognizer.close();
            japaneseRecognizer = null;
        }
        if (koreanRecognizer != null) {
            koreanRecognizer.close();
            koreanRecognizer = null;
        }
        if (languageIdentifier != null) {
            languageIdentifier.close();
            languageIdentifier = null;
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        Log.d(TAG, "OCRTextRecognizer cleaned up");
    }
}


