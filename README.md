# 📄 Document Scanner Android App

A professional Android document scanning application with advanced features including camera capture, image enhancement, multi-page PDF generation, and more.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Build](https://img.shields.io/badge/Build-Passing-success.svg)](https://github.com/sri8080/doument-scanner)

## ✨ Features

### 📸 Camera & Capture
- **CameraX Integration** - Modern camera API with preview
- **Touch to Focus** - Tap anywhere to focus
- **Flash Control** - Toggle flash for low-light conditions
- **Real-time Preview** - See captured images instantly
- **Auto-save** - Automatic image saving with timestamps

### 🎨 Image Enhancement
- **6 Filter Options:**
  - Original (no filter)
  - Auto Enhance (histogram equalization)
  - Black & White (adaptive threshold)
  - Grayscale (desaturation)
  - Sharpen (detail enhancement)
  - Brightness (lighting adjustment)
- **Real-time Preview** - See filters before applying
- **High Quality** - Maintains image quality

### ✂️ Manual Cropping
- **Drag Corner Points** - Adjust crop area precisely
- **Visual Overlay** - See crop area in real-time
- **Perspective Correction** - Ready for OpenCV integration
- **Reset Option** - Return to original corners

### 📚 Multi-Page Documents
- **Grid View** - 2-column thumbnail layout
- **Drag & Drop Reordering** - Long press to drag pages
- **Swipe to Remove** - Quick page deletion
- **Add Pages** - Add new pages anytime
- **Page Numbering** - Auto-numbered badges
- **Batch Processing** - Combine multiple pages

### 📄 PDF Generation
- **Professional PDFs** - High-quality output
- **Custom Page Sizes** - A4, Letter, Legal, A3, A5
- **Compression Options** - 4 levels (None to Best)
- **Image Quality Control** - Adjustable 0-100%
- **Metadata Support** - Title, Author, Subject, Keywords
- **File Size Optimization** - Up to 40% reduction

### 🔗 Sharing & Export
- **PDF Preview** - View before sharing
- **Share Intent** - Email, Drive, WhatsApp, etc.
- **FileProvider** - Secure file sharing (Android 7.0+)
- **Multiple Formats** - JPEG images and PDFs

## 🎯 Screenshots

> Add your app screenshots here

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24 (Android 7.0) or higher
- Gradle 8.0+
- Java 11

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/sri8080/doument-scanner.git
cd doument-scanner
```

2. **Open in Android Studio**
- Open Android Studio
- Select "Open an existing project"
- Navigate to the cloned directory
- Click OK

3. **Sync Gradle**
```bash
./gradlew clean build
```

4. **Run the app**
- Connect your Android device or start an emulator
- Click Run button in Android Studio
- Or use command: `./gradlew installDebug`

## 📦 Project Structure

```
app/
├── src/main/
│   ├── java/com/example/myapplication/
│   │   ├── MainActivity.java              # Main entry point
│   │   ├── CameraActivity.java            # Camera capture
│   │   ├── PreviewActivity.java           # Image enhancement
│   │   ├── ImageCropActivity.java         # Manual cropping
│   │   ├── MultiPageActivity.java         # Multi-page management
│   │   ├── PdfGenerator.java              # PDF generation
│   │   ├── FilterAdapter.java             # Filter selection
│   │   ├── PageAdapter.java               # Page grid adapter
│   │   └── CropOverlayView.java           # Custom crop view
│   ├── res/
│   │   ├── layout/                        # XML layouts
│   │   ├── drawable/                      # Icons & graphics
│   │   ├── values/                        # Strings, colors, themes
│   │   └── xml/                           # FileProvider config
│   └── AndroidManifest.xml                # App configuration
├── build.gradle.kts                       # App-level Gradle
└── proguard-rules.pro                     # ProGuard rules
```

## 🔧 Dependencies

### Core
- **AndroidX AppCompat** - Backward compatibility
- **Material Components** - Material Design 3
- **ConstraintLayout** - Flexible layouts
- **RecyclerView** - Efficient list displays
- **CardView** - Card-based UI

### Camera
- **CameraX Core** - Camera functionality
- **CameraX Camera2** - Camera2 API support
- **CameraX Lifecycle** - Lifecycle management
- **CameraX View** - PreviewView widget

### PDF Generation
- **iText 7 Core** - PDF creation
- **iText Layout** - Advanced layouts
- **iText Kernel** - Low-level operations
- **iText IO** - Image & font handling

### Optional (Future)
- **OpenCV Android** - Image processing
- **MLKit** - Document detection
- **TensorFlow Lite** - AI enhancements

## 🎨 Technologies Used

- **Language:** Java
- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 36 (Android 14)
- **Build System:** Gradle 8.0
- **Architecture:** Activity-based
- **UI:** Material Design 3
- **Camera:** CameraX
- **PDF:** iText 7.2.5
- **Storage:** FileProvider

## 📱 Supported Devices

- **Phones:** Android 7.0+ (API 24+)
- **Tablets:** Full support
- **Chromebooks:** Native Android app support
- **Foldables:** Adaptive layouts

## 🔐 Permissions

Required permissions:
- `CAMERA` - Capture documents
- `WRITE_EXTERNAL_STORAGE` - Save images (Android < 10)
- `READ_EXTERNAL_STORAGE` - Read images (Android < 10)

Runtime permissions are requested when needed.

## 🏗️ Build Variants

### Debug
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## 🧪 Testing

### On Emulator
```bash
./gradlew installDebug
adb shell am start -n com.example.myapplication/.MainActivity
```

### On Chromebook
```bash
# Enable ADB debugging on Chromebook
# Get Chromebook IP address
./deploy-to-chromebook.ps1 -ChromebookIP "192.168.1.100"
```

### Unit Tests (TODO)
```bash
./gradlew test
```

### Instrumentation Tests (TODO)
```bash
./gradlew connectedAndroidTest
```

## 📖 Documentation

Comprehensive guides available in the project:
- **CHROMEBOOK_TESTING_GUIDE.md** - Testing on Chromebook
- **ENHANCED_PDF_GENERATOR_GUIDE.md** - PDF generation features
- **IMAGE_CROP_GUIDE.md** - Cropping functionality
- **IMAGE_ENHANCER_GUIDE.md** - Filter system
- **MULTI_PAGE_ACTIVITY_GUIDE.md** - Multi-page management
- **PREVIEW_ACTIVITY_GUIDE.md** - Enhancement features
- **HOW_TO_ACCESS_PDFS.md** - Accessing generated PDFs

## 🗺️ Roadmap

### Version 1.0 (Current)
- ✅ Camera capture
- ✅ Image enhancement
- ✅ Manual cropping
- ✅ Multi-page documents
- ✅ PDF generation
- ✅ PDF sharing

### Version 1.1 (Planned)
- [ ] OpenCV integration
- [ ] Auto edge detection
- [ ] Perspective correction
- [ ] OCR (text recognition)
- [ ] Cloud storage integration
- [ ] Batch processing

### Version 2.0 (Future)
- [ ] AI-powered enhancements
- [ ] Document templates
- [ ] Digital signatures
- [ ] Password protection
- [ ] QR code scanning
- [ ] Multi-language support

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is open source. Feel free to use it for learning and development.

## 👤 Author

**sri8080**
- GitHub: [@sri8080](https://github.com/sri8080)

## 🙏 Acknowledgments

- [CameraX](https://developer.android.com/training/camerax) - Modern camera API
- [iText](https://itextpdf.com/) - PDF generation library
- [Material Design](https://material.io/) - Design system
- [Android Developers](https://developer.android.com/) - Documentation & guides

## 📞 Support

For support, please open an issue in the GitHub repository.

## 🌟 Star History

If you like this project, please give it a ⭐ on GitHub!

---

**Made with ❤️ for Android developers**

**Last Updated:** November 15, 2025

