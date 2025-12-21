# Google Play Store Deployment Guide
## App Usage Tracker - Complete Deployment Instructions

---

## ✅ PRE-DEPLOYMENT CHECKLIST (COMPLETED)

### 1. App Icon Setup ✓
- Your app logo (app_logo.png) is now configured as the app icon
- Adaptive icons configured for all Android versions
- Dark purple background (#1a0b2e) matching your logo theme

### 2. Build Configuration ✓
- ProGuard/R8 enabled for code optimization and obfuscation
- Resource shrinking enabled to reduce APK size
- ProGuard rules added for Room, Compose, Glance, and WorkManager

---

## 📋 STEP-BY-STEP DEPLOYMENT PROCESS

### STEP 1: Create a Keystore (Signing Key)

This is required to sign your app. You only need to do this once.

**Option A: Using Android Studio (Recommended)**
1. In Android Studio, go to: `Build` → `Generate Signed Bundle / APK`
2. Select `Android App Bundle`
3. Click `Next`
4. Click `Create new...` under "Key store path"
5. Fill in the details:
   - **Key store path**: Choose a secure location (e.g., `C:\Users\abhin\Desktop\keystores\appusagetracker-keystore.jks`)
   - **Password**: Create a strong password (SAVE THIS!)
   - **Alias**: `appusagetracker-key`
   - **Alias Password**: Create a password (can be same as above)
   - **Validity**: 25 years (or 10000 days)
   - **Certificate info**:
     - First and Last Name: Abhinav Vaidya
     - Organizational Unit: Optional
     - Organization: Optional
     - City or Locality: Your city
     - State or Province: Your state
     - Country Code: Your country (e.g., IN)
6. Click `OK`

**Option B: Using Command Line**
```powershell
# Navigate to Java bin directory (adjust path if needed)
cd "C:\Program Files\Android\Android Studio\jbr\bin"

# Generate keystore
.\keytool.exe -genkey -v -keystore C:\Users\abhin\Desktop\keystores\appusagetracker-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias appusagetracker-key
```

**⚠️ IMPORTANT: Backup Your Keystore**
- Store the keystore file in a SECURE location
- Keep the passwords SAFE (use a password manager)
- If you lose this, you CANNOT update your app on Play Store!

---

### STEP 2: Configure Signing in Gradle

Create a file: `keystore.properties` in your project root:

```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=appusagetracker-key
storeFile=C:/Users/abhin/Desktop/keystores/appusagetracker-keystore.jks
```

**⚠️ Add to .gitignore:**
```
keystore.properties
*.jks
*.keystore
```

Then update your `app/build.gradle.kts` to use it (see code below).

---

### STEP 3: Build the Release Bundle

**Using Android Studio:**
1. Go to: `Build` → `Generate Signed Bundle / APK`
2. Select `Android App Bundle`
3. Select your keystore (created in Step 1)
4. Choose `release` build variant
5. Click `Finish`
6. The AAB file will be created in: `app/release/app-release.aab`

**Using Command Line:**
```powershell
cd C:\Users\abhin\Desktop\Projects\AppUsageTracker
.\gradlew.bat bundleRelease
```

The bundle will be at: `app\build\outputs\bundle\release\app-release.aab`

---

### STEP 4: Create Google Play Console Account

1. Go to: https://play.google.com/console
2. Sign in with your Google account
3. Pay the one-time $25 registration fee
4. Complete your account information

---

### STEP 5: Create a New App in Play Console

1. Click `Create app`
2. Fill in the details:
   - **App name**: App Usage Tracker (or your preferred name)
   - **Default language**: English (United States)
   - **App or game**: App
   - **Free or paid**: Free
3. Accept declarations and click `Create app`

---

### STEP 6: Set Up Store Listing

Navigate to: `Dashboard` → `Store presence` → `Main store listing`

**Required Information:**

1. **App name**: App Usage Tracker
2. **Short description** (80 characters max):
   ```
   Track your screen time, monitor app usage, and build better digital habits.
   ```

3. **Full description** (4000 characters max):
   ```
   📱 Take Control of Your Screen Time

   App Usage Tracker helps you understand and manage your smartphone usage with beautiful, insightful analytics. Monitor which apps consume most of your time and build healthier digital habits.

   ⏱️ KEY FEATURES

   • Real-time Usage Tracking
     Track how much time you spend on each app with detailed statistics

   • Beautiful Dashboard
     View your usage patterns at a glance with an intuitive, modern interface

   • Daily & Weekly Analytics
     Understand your usage trends over time with comprehensive charts

   • Home Screen Widget
     Monitor your top apps without opening the app

   • Set Usage Goals
     Define daily limits and track your progress

   • App Categories
     Organize apps by category to see where your time goes

   • Dark Theme Support
     Easy on the eyes with a beautiful dark purple gradient theme

   🎯 WHY APP USAGE TRACKER?

   • Privacy First: All data stays on your device
   • Battery Efficient: Optimized background processing
   • No Ads: Clean, distraction-free experience
   • Lightweight: Small app size, minimal storage

   📊 INSIGHTS YOU'LL GET

   • Total screen time per day/week
   • Most used apps ranking
   • Usage time for each app
   • Usage trends and patterns
   • Time spent in different categories

   🔒 PRIVACY & PERMISSIONS

   App Usage Tracker requires the "Usage Access" permission to track app usage statistics. This permission is strictly used only to display your app usage data to you. We never collect, share, or upload your personal data to any server.

   ⚡ BATTERY OPTIMIZED

   Our app uses intelligent caching and efficient background processing to ensure minimal battery impact while providing accurate tracking.

   🎨 MODERN DESIGN

   Built with the latest Material Design 3 guidelines and Jetpack Compose, providing a smooth, responsive, and beautiful user experience.

   💬 FEEDBACK & SUPPORT

   We're constantly improving! If you have suggestions or need help, please contact us.

   Download App Usage Tracker today and take the first step towards healthier digital habits! 🚀
   ```

4. **App icon**: Already configured (your app_logo.png will be used)

5. **Feature graphic** (1024 x 500 px):
   - Create a banner with your logo and app name
   - You'll need to create this in a design tool

6. **Phone screenshots** (2-8 required, JPEG or PNG, 16:9 or 9:16 aspect ratio):
   - Take screenshots of your app showing:
     - Dashboard/Home screen
     - App list with usage stats
     - Analytics/Charts screen
     - Widget preview
     - Settings screen
   - Minimum 2 screenshots required

7. **Category**: 
   - Choose: `Productivity` or `Tools`

8. **Contact details**:
   - Email: Your email address
   - Phone: Optional
   - Website: Optional

9. **Privacy Policy**: 
   - Required for apps with Usage Access permission
   - See template below

---

### STEP 7: Privacy Policy

You MUST have a privacy policy for your app. Create a simple webpage or use a free hosting service.

**Privacy Policy Template:**

```markdown
# Privacy Policy for App Usage Tracker

Last updated: December 20, 2025

## Overview
App Usage Tracker ("we", "our", or "us") is committed to protecting your privacy. This policy explains how our app handles your information.

## Information Collection
App Usage Tracker accesses your device's app usage statistics solely to display them to you within the app. We do NOT:
- Collect any personal information
- Upload data to any server
- Share your information with third parties
- Track your location
- Access your contacts, messages, or files

## Usage Access Permission
The app requires the "Usage Access" permission to read app usage statistics from your device's system. This data:
- Stays entirely on your device
- Is never transmitted to any server
- Is used only to show you your app usage patterns

## Data Storage
All app usage data is stored locally on your device using Android's Room database. You can clear this data at any time by uninstalling the app.

## Third-Party Services
App Usage Tracker does not use any third-party analytics, advertising, or tracking services.

## Children's Privacy
Our app does not knowingly collect information from children under 13.

## Changes to This Policy
We may update this policy from time to time. Any changes will be posted on this page.

## Contact Us
If you have questions about this privacy policy, please contact us at: [your-email@example.com]
```

**How to host your Privacy Policy:**
- Option 1: Create a GitHub Page (free)
- Option 2: Use Google Sites (free)
- Option 3: Use https://www.privacypolicygenerator.info/
- Option 4: Host on your own website

---

### STEP 8: Content Rating

1. Navigate to: `Dashboard` → `Store presence` → `App content` → `Content rating`
2. Click `Start questionnaire`
3. Select category: `Utility, Productivity, Communication`
4. Answer the questions honestly:
   - Does your app contain violence? **No**
   - Does your app contain sexual content? **No**
   - Does your app contain bad language? **No**
   - Does your app allow user interaction? **No**
   - Does your app share user location? **No**
   - Does your app allow purchases? **No**
5. Submit and get your rating

---

### STEP 9: Target Audience and Content

1. **Target age group**: 
   - Select: `13+` or `Everyone`

2. **App access**:
   - If free with no restrictions, select: "All functionality is available without restrictions"

3. **Ads**:
   - Select: "No, my app does not contain ads"

4. **Data safety**:
   - Navigate to `App content` → `Data safety`
   - Answer questions:
     - Does your app collect or share user data? **No** (since everything stays on device)
     - Click "Next" and complete the form
     - Submit

---

### STEP 10: Upload Your App Bundle

1. Navigate to: `Dashboard` → `Release` → `Production`
2. Click `Create new release`
3. Upload your AAB file (`app-release.aab`)
4. Set release name: `1.0` (or current version)
5. Add release notes:
   ```
   🎉 Initial Release - Version 1.0
   
   ✨ Features:
   • Track app usage and screen time
   • View detailed statistics and analytics
   • Home screen widget for quick access
   • Beautiful dark theme interface
   • Set daily usage goals
   • Privacy-first: All data stays on your device
   
   Welcome to App Usage Tracker! Take control of your digital habits today.
   ```
6. Click `Next`
7. Review and click `Start rollout to Production`

---

### STEP 11: Pre-Launch Testing (Optional but Recommended)

Google Play will automatically test your app on real devices:
1. Navigate to: `Release` → `Testing` → `Internal testing`
2. Create an internal test track
3. Upload your AAB
4. Add test users (your email)
5. Test the app thoroughly before production release

---

### STEP 12: Submit for Review

1. Complete all required sections (marked with red exclamation marks)
2. Review the summary
3. Click `Send for review`
4. Google typically reviews apps within 2-7 days
5. You'll receive an email when approved or if changes are needed

---

## 📝 POST-LAUNCH CHECKLIST

### Monitor Your App
- Check crash reports: Play Console → `Quality` → `Android vitals`
- Read user reviews: Play Console → `Ratings and reviews`
- Track installs: Play Console → `Statistics`

### Future Updates
1. Update version code and version name in `build.gradle.kts`
2. Build new signed bundle
3. Upload to Play Console → `Production` → `Create new release`
4. Add release notes describing changes
5. Submit for review

---

## 🛠️ GRADLE CONFIGURATION FOR SIGNING

Add this to your `app/build.gradle.kts`:

```kotlin
import java.util.Properties

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    // ... existing config ...
    
    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile") ?: "")
            storePassword = keystoreProperties.getProperty("storePassword") ?: ""
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: ""
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: ""
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## 📸 CREATING SCREENSHOTS

**Tools to create screenshots:**
1. Run your app on an emulator or physical device
2. Take screenshots using:
   - Android Studio: Click camera icon in logcat
   - Device: Power + Volume Down
3. Or use Android Studio's screenshot tool

**Recommended screenshot content:**
1. Dashboard with usage statistics
2. App list sorted by usage time
3. Analytics/charts view
4. Home screen widget
5. App details screen
6. Settings screen

**Screenshot specifications:**
- Format: PNG or JPEG
- Minimum dimension: 320px
- Maximum dimension: 3840px
- Recommended: 1080 x 1920 (9:16 portrait)

---

## 🎨 CREATING FEATURE GRAPHIC

Create a 1024 x 500 px banner using:
- Canva (free)
- Figma (free)
- Adobe Photoshop
- GIMP (free)

**Content suggestions:**
- Your app logo in the center
- App name
- Tagline: "Track Your Screen Time"
- Gradient background matching your theme

---

## ❓ COMMON ISSUES & SOLUTIONS

### Issue: "Upload failed - Version code already used"
**Solution**: Increment `versionCode` in `build.gradle.kts`

### Issue: "Missing privacy policy"
**Solution**: Create and host a privacy policy, add URL in app content

### Issue: "App not compliant with target API level"
**Solution**: Ensure `targetSdk = 36` in your `build.gradle.kts`

### Issue: "Signing key mismatch"
**Solution**: Use the same keystore for all updates

### Issue: "App crashes on some devices"
**Solution**: Check crash reports in Play Console → Android vitals

---

## 📊 METRICS TO TRACK

After launch, monitor:
- **Installs**: Total downloads
- **Uninstalls**: Users who removed your app
- **Crashes**: Critical to fix ASAP
- **ANRs** (App Not Responding): Performance issues
- **Ratings**: User satisfaction
- **Reviews**: User feedback

---

## 🚀 MARKETING YOUR APP

1. **App Store Optimization (ASO)**:
   - Use relevant keywords in title and description
   - High-quality screenshots
   - Respond to user reviews

2. **Social Media**:
   - Share on Twitter, LinkedIn, Reddit
   - Create demo videos

3. **Product Hunt**:
   - Launch on Product Hunt for visibility

4. **Tech Forums**:
   - Share on XDA Developers, Android Police forums

---

## 📞 SUPPORT & RESOURCES

- **Google Play Console Help**: https://support.google.com/googleplay/android-developer
- **Android Developer Docs**: https://developer.android.com/distribute
- **Play Console**: https://play.google.com/console

---

## ✅ FINAL CHECKLIST BEFORE SUBMISSION

- [ ] Keystore created and backed up
- [ ] App signed with release key
- [ ] ProGuard enabled and tested
- [ ] All screens tested thoroughly
- [ ] No crashes or critical bugs
- [ ] Privacy policy created and hosted
- [ ] Store listing completed (title, description, screenshots)
- [ ] Content rating obtained
- [ ] Data safety section completed
- [ ] Icon and graphics added
- [ ] Version code and name set correctly
- [ ] AAB file uploaded
- [ ] Release notes written

---

**Good luck with your app launch! 🎉**

If you encounter any issues during deployment, refer to this guide or reach out to Google Play Console support.

