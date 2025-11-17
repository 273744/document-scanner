# SIMPLIFIED Multi-Page Workflow - No More Confusion!

## The Problem
The current workflow is TOO COMPLEX and CONFUSING for users:
- Too many buttons and dialogs
- Users don't know which button to press
- "Add More Pages" vs "Done" - confusing!
- Results not being passed back properly

## The NEW Simplified Solution

### Automatic Multi-Page Mode Detection
When PreviewActivity is launched from CameraActivity which was launched from MultiPageActivity, it should:
1. **Automatically save** the image
2. **Automatically return** to MultiPageActivity
3. **NO dialogs or confusing buttons**
4. **Just works!**

### User Flow (SIMPLIFIED):
```
User in MultiPageActivity
  ↓ Taps FAB (+)
Camera opens
  ↓ Captures image
Preview opens → Applies filter → AUTO-SAVES
  ↓ Automatically returns
MultiPageActivity
  ↓ Page automatically added to grid!
  ↓ User sees page count update
  ↓ Taps FAB (+) again to add more
Repeat!
```

### Implementation Plan:

1. **Pass a flag** from MultiPageActivity → CameraActivity → PreviewActivity indicating "multi_page_mode"

2. **In PreviewActivity**: When in multi-page mode:
   - Show simpler UI: "Adding Page X..." 
   - Auto-save after filter selection (or original)
   - Auto-return to MultiPageActivity (no dialog!)
   
3. **In MultiPageActivity**: When page is added:
   - Show toast: "Page 2 added! Tap + for more"
   - Update grid immediately
   - Keep FAB prominent

## Benefits:
- ✅ **NO confusing buttons**
- ✅ **NO dialogs to understand**
- ✅ **Automatic workflow**
- ✅ **Just capture, it adds!**
- ✅ **Visual feedback in grid**

## Alternative Simpler Approach:
Make MultiPageActivity THE DEFAULT for capturing documents, not a special mode!

When user taps "Capture Document" from Main:
- Opens MultiPageActivity (empty, with camera preview overlay)
- User captures pages one by one
- Each page appears in a bottom strip/carousel
- User taps "Generate PDF" when done

This is how professional scanning apps work - continuous capture mode is the default!

