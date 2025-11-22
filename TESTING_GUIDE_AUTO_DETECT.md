# 🧪 Testing Guide - Auto-Detect & Enhance Feature

## Quick Test Steps

### 1. Launch the App
```bash
adb shell am start -n com.srikanth.docscanner/.MainActivity
```

### 2. Navigate to Camera
- Click on **"Capture Document"** button from main screen

### 3. Capture a Document
- Point camera at any document (paper, receipt, book, etc.)
- Click the **capture button** (camera icon)
- Wait for the image to be captured

### 4. Test Auto-Detect
- You should now see the **ImageCropActivity** with:
  - Preview of captured image
  - 4 draggable corner handles
  - **🤖 Auto-Detect & Enhance** button (purple)
  - Manual control buttons (Cancel, Reset, Crop)

### 5. Click Auto-Detect Button
- Click the **"🤖 Auto-Detect & Enhance"** button
- Watch the progress messages:
  1. "🤖 Auto-detecting document edges..."
  2. "✓ Edges detected! Enhancing image..."

### 📊 WHERE TO SEE QUALITY SCORE:
**The quality score appears in the SUCCESS DIALOG** that pops up after auto-detection completes!
- Look for: **"Quality Score: 8.5/10"** at the top of the dialog
- Also saved in document description (view in Gallery later)

### Expected Results:

#### ✅ Success Case (Good Document Detection):
- Corners automatically adjust to document edges
- Image is cropped with perspective correction
- Enhancement is applied
- Success dialog shows:
  ```
  ✨ Document Enhanced!
  Quality Score: 8.5/10
  
  Document automatically detected, cropped, and enhanced!
  
  Would you like to generate a PDF?
  ```
- Options:
  - **Generate PDF**: Creates single-page PDF
  - **Add More Pages**: Start multi-page workflow
  - **Done**: Save to gallery

#### ⚠️ Failure Case (No Document Detected):
- Dialog shows:
  ```
  ⚠️ No Document Detected
  No document detected, using full image
  
  You can:
  • Manually adjust corners
  • Try with better lighting
  • Ensure document is flat and visible
  ```
- You can still manually adjust corners

## 🎯 Test Scenarios

### Scenario 1: White Paper Document
**Setup**: Place white A4 paper on dark desk
**Expected**: ✅ High quality score (8-10/10)
**Test**: Edges should be detected perfectly

### Scenario 2: Receipt
**Setup**: Small receipt on any surface
**Expected**: ✅ Good quality score (7-9/10)
**Test**: Should detect smaller rectangular areas

### Scenario 3: Book Page
**Setup**: Open book on table
**Expected**: ✅ Moderate quality score (6-8/10)
**Test**: Should detect page boundary

### Scenario 4: Poor Lighting
**Setup**: Document in dim lighting
**Expected**: ⚠️ May fail or low score (3-6/10)
**Test**: Verify fallback to manual adjustment

### Scenario 5: No Document
**Setup**: Point at empty surface
**Expected**: ⚠️ Detection fails
**Test**: Should show helpful error message

### Scenario 6: Complex Background
**Setup**: Document on patterned surface
**Expected**: ⚠️ May have lower accuracy (5-7/10)
**Test**: Edge detection might be imperfect

## 🔍 What to Check

### Visual Quality:
- [ ] Document edges are correctly detected
- [ ] Perspective correction makes document flat
- [ ] Text is sharp and readable
- [ ] No distortion or warping
- [ ] Colors look natural (or B&W if preferred)

### Performance:
- [ ] Processing completes in < 2 seconds
- [ ] No app crashes or freezes
- [ ] Smooth UI transitions
- [ ] Progress indicators work correctly

### User Experience:
- [ ] Button is easy to find and understand
- [ ] Success/error messages are clear
- [ ] Can fallback to manual adjustment
- [ ] Multi-page workflow still works

## 📊 Quality Score Interpretation

| Score | Quality | Description |
|-------|---------|-------------|
| 9-10 | Excellent | Perfect detection, document centered |
| 7-8  | Good | Minor edge variations, good overall |
| 5-6  | Fair | Edges detected but needs adjustment |
| 3-4  | Poor | Rough detection, manual adjustment recommended |
| 0-2  | Failed | No valid document detected |

## 🐛 Known Issues to Watch For

1. **White document on white surface**: May fail (no contrast)
2. **Curved/bent documents**: Perspective correction has limits
3. **Multiple documents**: Only detects the largest one
4. **Glare/reflections**: Can interfere with edge detection
5. **Very low resolution**: May not detect fine edges

## 🎬 Video Test Sequence

If recording demo video:
1. Open app
2. Navigate to camera
3. Point at document
4. Capture photo
5. **Highlight the Auto-Detect button**
6. Click it
7. Show corner adjustment animation
8. Show success dialog with quality score
9. Generate PDF
10. Show final result in gallery

## 📝 Testing Checklist

### Basic Functionality:
- [ ] Auto-detect button appears
- [ ] Button is clickable
- [ ] Progress indicators show
- [ ] Corners update automatically
- [ ] Image is cropped correctly
- [ ] Enhancement is applied
- [ ] File is saved to gallery
- [ ] Database entry is created

### Edge Cases:
- [ ] Works with very small documents
- [ ] Works with large documents
- [ ] Handles portrait/landscape orientation
- [ ] Works in multi-page mode
- [ ] Manual adjustment still works after auto-detect
- [ ] Can reset corners after auto-detect
- [ ] Can cancel during processing

### Integration:
- [ ] PDF generation works with auto-enhanced images
- [ ] Gallery displays auto-enhanced documents
- [ ] Multi-page combines auto-enhanced pages
- [ ] OCR works with enhanced images

## 🚀 Performance Benchmarks

Target metrics:
- **Edge Detection**: < 500ms
- **Perspective Transform**: < 200ms
- **Enhancement**: < 300ms
- **Total Processing**: < 1000ms (1 second)

## 💾 File Naming

Auto-processed files are saved as:
```
AUTO_2025-11-22-14-30-45-123.jpg
```

Manual cropped files:
```
CROPPED_2025-11-22-14-30-45-123.jpg
```

## 🎨 Visual Comparison

Take screenshots of:
1. **Before**: Original captured image
2. **After Auto-Detect**: Cropped and enhanced
3. **Manual Crop**: For comparison

## ✅ Final Validation

The feature is working correctly if:
1. ✅ Auto-detect button is visible and functional
2. ✅ Edge detection works on standard documents
3. ✅ Perspective correction produces flat images
4. ✅ Enhancement improves readability
5. ✅ Quality score is reasonable (> 5 for good photos)
6. ✅ Manual fallback works when detection fails
7. ✅ Multi-page workflow integrates seamlessly
8. ✅ No crashes or memory leaks

## 📱 Test Devices

Recommended to test on:
- [ ] Emulator (API 34)
- [ ] Physical device (if available)
- [ ] Different screen sizes
- [ ] Various Android versions

## 🎯 Success Criteria

Feature is **READY FOR RELEASE** when:
- ✅ 80%+ success rate on standard documents
- ✅ < 2 second processing time
- ✅ No crashes in 50+ consecutive uses
- ✅ User feedback is positive
- ✅ All test scenarios pass

---

**Happy Testing! 🧪✨**

Need help? Check the main documentation: `AUTO_DETECT_FEATURE.md`

