# 📊 Quality Score - Where to Find It

## 🎯 Quick Answer

The **Quality Score** is displayed in **TWO places**:

### 1. 🔔 Success Dialog (Primary Display)
**When**: Immediately after auto-detection completes successfully
**Where**: Pop-up dialog on screen

```
┌─────────────────────────────────────┐
│   ✨ Document Enhanced!              │
├─────────────────────────────────────┤
│                                     │
│  Quality Score: 8.5/10              │  ← HERE!
│                                     │
│  Document automatically detected,   │
│  cropped, and enhanced!             │
│                                     │
│  Would you like to generate a PDF?  │
│                                     │
├─────────────────────────────────────┤
│  [Generate PDF] [Add More] [Done]   │
└─────────────────────────────────────┘
```

**When you'll see this**:
- After clicking "🤖 Auto-Detect & Enhance"
- When edge detection succeeds
- In single-document mode (not multi-page batch)

---

### 2. 📝 Document Description (Secondary Display)
**When**: Viewing document details in Gallery
**Where**: Document properties/description field

The quality score is permanently saved in the document's metadata as:
```
"Auto-detected and enhanced (Quality: 8.5/10)"
```

**How to view**:
1. Go to **Gallery** in the app
2. Find the auto-detected document (filename starts with `AUTO_...`)
3. Long-press or tap for details
4. Check the **Description** field

---

## 📱 Step-by-Step to See Quality Score

### Method 1: During Auto-Detection (Recommended)
```
1. Open app
2. Tap "Capture Document"
3. Take a photo
4. Tap "🤖 Auto-Detect & Enhance" button
5. Wait 1-2 seconds
6. ✨ Success dialog appears with Quality Score!
```

### Method 2: In Gallery (After Processing)
```
1. Open app
2. Tap "View Gallery"
3. Find document with "AUTO_" prefix
4. Tap/Long-press document
5. View document details
6. See description: "Auto-detected and enhanced (Quality: X.X/10)"
```

---

## 🎨 Visual Examples

### High Quality Score (9.0/10)
```
✨ Document Enhanced!
Quality Score: 9.0/10 ← Excellent!

Perfect edge detection, well-lit,
clear document boundaries.
```

### Good Quality Score (7.5/10)
```
✨ Document Enhanced!
Quality Score: 7.5/10 ← Good

Minor variations in edges,
overall detection successful.
```

### Fair Quality Score (5.0/10)
```
✨ Document Enhanced!
Quality Score: 5.0/10 ← Fair

Edges detected but you may want
to manually adjust corners.
```

---

## 📊 What Does the Quality Score Mean?

| Score | Rating | What It Means | Action Recommended |
|-------|--------|---------------|-------------------|
| **9-10** | ⭐⭐⭐⭐⭐ Excellent | Perfect detection! Document perfectly centered, clear edges | None - Use as is |
| **7-8** | ⭐⭐⭐⭐ Good | Very good detection, minor edge variations | None - Quality sufficient |
| **5-6** | ⭐⭐⭐ Fair | Acceptable detection but could be better | Consider retaking or manual adjust |
| **3-4** | ⭐⭐ Poor | Rough detection, edges not ideal | Manual adjustment recommended |
| **0-2** | ⭐ Failed | Detection failed or very poor | Retake photo with better conditions |

---

## 🚨 If You Don't See Quality Score

### Multi-Page Mode:
In **multi-page mode**, the quality score dialog is **skipped** for faster workflow. Instead:
- You'll see: "✓ Page auto-processed and added!"
- Quality score is still saved in document description
- View it later in Gallery

### Detection Failure:
If auto-detection **fails**, you'll see:
```
⚠️ No Document Detected
No document detected, using full image

You can:
• Manually adjust corners
• Try with better lighting
• Ensure document is flat and visible
```
- No quality score shown (detection failed)
- You can still use manual cropping

---

## 💡 Tips for Better Quality Scores

### To Get 9-10/10 Scores:

1. **Lighting** ☀️
   - Use bright, even lighting
   - Avoid shadows across document
   - No glare or reflections

2. **Background** 🎨
   - Dark desk/surface for white paper
   - High contrast between document and surface
   - Avoid patterned backgrounds

3. **Positioning** 📐
   - Document flat (not curved/bent)
   - Fully visible in frame
   - Not too close to edges
   - Straight, not angled

4. **Document Type** 📄
   - Clean, crisp edges
   - Rectangular shape
   - No torn or damaged corners

---

## 🔍 Finding Your Quality Score Later

### In App Gallery:

1. **Open Gallery**
   ```
   Main Screen → "View Gallery"
   ```

2. **Filter by Type**
   ```
   - Look for files starting with "AUTO_"
   - These are auto-detected documents
   ```

3. **View Details**
   ```
   - Long-press document
   - Or tap for options
   - Select "Details" or "Properties"
   - Description shows: "Quality: X.X/10"
   ```

### In File Manager:

1. **Navigate to folder**
   ```
   Internal Storage/Android/media/com.srikanth.docscanner/DocumentScanner/
   ```

2. **Find AUTO_ files**
   ```
   AUTO_2025-11-22-14-30-45-123.jpg
   ```

3. **Open in app**
   - Quality stored in database entry
   - View through app Gallery

---

## 📸 Screenshot Locations

When you see the quality score:

### Screenshot 1: Success Dialog
![Success Dialog with Quality Score]
- Appears immediately after auto-detect
- Center of screen, modal dialog
- Can't be missed!

### Screenshot 2: Gallery Details
![Gallery Document Details]
- Gallery → Select document → Details
- Shows all metadata including quality

---

## 🎯 Pro Tips

### Want to Track Quality Over Time?
- The quality score is **saved permanently**
- You can review all your auto-detected documents
- Compare quality scores to improve your technique
- Learn what lighting/positioning works best

### Quality Score in Multi-Page PDFs?
- Each page has its own quality score
- View individual page details in Gallery
- Combined PDF shows all source images

### Export Quality Data?
- Quality scores are in the database
- Can be viewed in document properties
- Consider adding export feature for analysis

---

## ✅ Summary

**Primary View**: 
- **Success Dialog** after auto-detection
- Shows immediately on screen
- Format: "Quality Score: X.X/10"

**Secondary View**:
- **Document Description** in Gallery
- Saved permanently
- Format: "Auto-detected and enhanced (Quality: X.X/10)"

**To Test Right Now**:
1. Capture a document
2. Click "🤖 Auto-Detect & Enhance"
3. Wait 1-2 seconds
4. **Look at the dialog** → Quality Score is right there! 🎯

---

Need help improving your quality scores? Check the tips above! 📈✨

