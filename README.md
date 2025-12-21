# 📱 App Usage Tracker

A modern Android application built with Jetpack Compose that helps users track and analyze their daily and weekly app usage patterns. Monitor screen time, set usage limits, and gain insights into your digital habits.

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)
![Min API](https://img.shields.io/badge/Min%20API-26-orange.svg)
![Target API](https://img.shields.io/badge/Target%20API-36-brightgreen.svg)

## ✨ Features

### 📊 Core Functionality
- **Daily Usage Tracking**: Monitor app usage statistics for the current day
- **Weekly Analytics**: View comprehensive weekly usage patterns with interactive charts
- **Home Screen Widget**: Quick access to your screen time via Jetpack Glance widget
- **Usage Stats Integration**: Leverages Android's UsageStatsManager API for accurate tracking
- **Smart Caching**: Efficient Room database caching for optimal performance

### 🎨 User Experience
- **Material 3 Design**: Modern UI following Material Design 3 guidelines
- **Dark Theme Support**: Beautiful dark purple theme (#1a0b2e)
- **Animated Splash Screen**: Smooth splash screen with branding (Android 12+)
- **Responsive Layout**: Adapts to different screen sizes and orientations
- **Intuitive Navigation**: Bottom navigation bar for seamless screen transitions

### ⚙️ Advanced Features
- **Widget Customization**: Configure widget update intervals and appearance
- **Battery Optimization**: Smart WorkManager-based updates (30-60 min intervals)
- **Efficient Updates**: Widget reads from cached data, not live queries
- **Boot Persistence**: Automatically reschedules widget updates after device reboot
- **ProGuard/R8**: Code obfuscation and optimization for release builds

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Manual DI with repository pattern
- **Database**: Room (SQLite)
- **Background Tasks**: WorkManager
- **Widget Framework**: Jetpack Glance

### Key Components
```
app/
├── data/
│   ├── local/              # Room database, DAOs, entities
│   └── UsageRepository     # Data layer abstraction
├── system/
│   └── usage/              # UsageStatsManager wrapper
├── ui/
│   ├── components/         # Reusable UI components
│   ├── home/               # Daily usage screen
│   ├── weekly/             # Weekly analytics screen
│   ├── settings/           # Widget configuration
│   └── about/              # About screen
├── viewmodel/              # ViewModels for state management
└── widget/                 # Glance widget implementation
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK API 26-36
- Gradle 8.7+

### Installation

1. **Clone the repository**
```bash
git clone https://github.com/abhi9vaidya/AppUsageTracker.git
cd AppUsageTracker
```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

3. **Run the app**
   - Connect an Android device or start an emulator (API 26+)
   - Click the "Run" button (▶️) in Android Studio
   - Grant "Usage Access" permission when prompted

### Required Permissions
The app requires the following permission:
- **PACKAGE_USAGE_STATS**: Access app usage statistics (user must grant via Settings)
- **RECEIVE_BOOT_COMPLETED**: Reschedule widget updates after device reboot

## 📦 Building for Release

### Quick Start
For detailed deployment instructions, see:
- **[START_HERE.md](START_HERE.md)** - Simple step-by-step guide
- **[QUICK_DEPLOYMENT_CHECKLIST.md](QUICK_DEPLOYMENT_CHECKLIST.md)** - Quick reference checklist
- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** - Comprehensive deployment documentation

### Build Commands

**Generate Debug APK:**
```bash
./gradlew assembleDebug
```

**Generate Release APK:**
```bash
./gradlew assembleRelease
```

**Generate Release Bundle (for Play Store):**
```bash
./gradlew bundleRelease
```

### Signing Configuration
1. Copy `keystore.properties.template` to `keystore.properties`
2. Update with your keystore credentials:
```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=your-key-alias
storeFile=path/to/your/keystore.jks
```

**⚠️ Important**: Never commit `keystore.properties` to version control!

## 📱 App Screens

### Home Screen
- Current date display
- Total screen time for today
- List of apps with usage duration
- Pull-to-refresh functionality
- Navigation to other screens

### Weekly Screen
- 7-day usage overview
- Interactive bar chart visualization
- Daily breakdown with total hours
- Average daily usage calculation
- Color-coded usage levels

### Settings Screen
- Widget update interval configuration
- App theme preferences
- About section with version info
- Privacy policy link

### Widget
- Compact home screen widget
- Shows top 3 most used apps
- Total daily screen time
- Customizable update frequency
- Battery-efficient background updates

## 🔒 Privacy & Security

- **Local Storage Only**: All data stored locally in Room database
- **No Network Access**: App does not connect to the internet
- **No Analytics**: Zero third-party tracking or analytics
- **User Control**: Users can clear data anytime via Android Settings
- **See**: [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for details

## 🛠️ Development

### Dependencies
Key libraries used in this project:
- **Jetpack Compose**: Modern declarative UI
- **Room**: Local database with SQLite
- **WorkManager**: Background task scheduling
- **Glance**: Widget framework
- **Material 3**: UI components and theming
- **Lifecycle & ViewModel**: State management
- **KSP**: Kotlin Symbol Processing for Room

See [gradle/libs.versions.toml](gradle/libs.versions.toml) for complete dependency list.

### Code Structure
- **MVVM Pattern**: Separation of concerns with ViewModels
- **Repository Pattern**: Abstracts data sources
- **Dependency Injection**: Constructor injection for testability
- **Kotlin Coroutines**: Asynchronous programming
- **StateFlow**: Reactive state management

### Build Variants
- **Debug**: Development build with debugging enabled
- **Release**: Production build with ProGuard optimization

## 📝 Version History

### Version 1.0 (Current)
- Initial release
- Daily and weekly usage tracking
- Home screen widget
- Material 3 design
- Battery-optimized background updates
- ProGuard/R8 optimization

## 🤝 Contributing

This is a personal project, but suggestions and feedback are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is available for personal and educational use. Please provide attribution if you use significant portions of the code.

## 👨‍💻 Author

**Abhinav Vaidya**
- GitHub: [@abhi9vaidya](https://github.com/abhi9vaidya)

## 🙏 Acknowledgments

- Built with Android Jetpack libraries
- Inspired by Digital Wellbeing apps
- Material Design 3 guidelines
- Android developer community

## 📞 Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Check existing documentation in the repo
- Review the deployment guides for common problems

## 🗺️ Roadmap

Future enhancements being considered:
- [ ] Custom time range selection
- [ ] App category grouping
- [ ] Usage goals and alerts
- [ ] Export data to CSV
- [ ] Comparison with previous weeks
- [ ] Focus mode integration
- [ ] Dark/Light theme toggle

