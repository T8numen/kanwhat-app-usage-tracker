# 🚀 Quick Deployment Checklist

## Pre-Deployment (COMPLETED ✓)
- [x] App logo configured as app icon
- [x] Adaptive icons set up
- [x] ProGuard/R8 enabled
- [x] Resource shrinking enabled
- [x] ProGuard rules configured
- [x] Build configuration optimized

## Your Next Steps

### 1️⃣ CREATE KEYSTORE (First Time Only)
```
Build → Generate Signed Bundle/APK → Create new keystore
Save location: C:\Users\abhin\Desktop\keystores\appusagetracker-keystore.jks
⚠️ BACKUP THIS FILE AND SAVE PASSWORDS!
```

### 2️⃣ CONFIGURE SIGNING
1. Copy `keystore.properties.template` to `keystore.properties`
2. Fill in your passwords and keystore path
3. DO NOT commit keystore.properties to git (already in .gitignore)

### 3️⃣ BUILD RELEASE BUNDLE
```
Build → Generate Signed Bundle/APK → Select keystore → Build
OR
.\gradlew.bat bundleRelease
```
Output: `app\build\outputs\bundle\release\app-release.aab`

### 4️⃣ CREATE PLAY CONSOLE ACCOUNT
- Go to: https://play.google.com/console
- Pay $25 one-time fee
- Complete registration

### 5️⃣ CREATE APP LISTING
**Required Assets:**
- [x] App icon (already configured)
- [ ] Feature graphic (1024 x 500 px)
- [ ] Screenshots (minimum 2, recommended 4-8)
- [ ] Privacy policy URL (use PRIVACY_POLICY.md)

**App Details:**
```
Name: App Usage Tracker
Category: Productivity / Tools
Short description: Track your screen time, monitor app usage, and build better digital habits.
```

### 6️⃣ UPLOAD & SUBMIT
1. Upload AAB file
2. Complete all required sections
3. Submit for review
4. Wait 2-7 days for approval

---

## 📁 Important Files Created

### Deployment Documentation
- `DEPLOYMENT_GUIDE.md` - Complete step-by-step guide
- `PRIVACY_POLICY.md` - Privacy policy template
- `keystore.properties.template` - Keystore configuration template

### What You Need to Create
1. **Keystore file** (Step 1 in guide)
2. **Feature graphic** (1024x500px banner)
3. **Screenshots** (run app and capture screens)
4. **Privacy policy hosted URL** (can use GitHub Pages)

---

## ⚡ Quick Commands

### Build Release Bundle
```powershell
cd C:\Users\abhin\Desktop\Projects\AppUsageTracker
.\gradlew.bat bundleRelease
```

### Build and Install Debug
```powershell
.\gradlew.bat installDebug
```

### Clean Build
```powershell
.\gradlew.bat clean
.\gradlew.bat bundleRelease
```

### Check Version
Check `app/build.gradle.kts`:
- `versionCode = 1` (increment for each release)
- `versionName = "1.0"` (user-facing version)

---

## 📸 Screenshot Checklist

Capture these screens for Play Store:
- [ ] Home/Dashboard screen with usage stats
- [ ] App list with usage times
- [ ] Analytics/Charts view
- [ ] Home screen showing widget
- [ ] App details screen
- [ ] Settings screen (optional)

---

## 🎨 Feature Graphic Ideas

Create 1024x500px graphic with:
- Your app logo (centered or left)
- App name: "App Usage Tracker"
- Tagline: "Take Control of Your Screen Time"
- Background: Gradient (purple/pink matching theme)

Tools: Canva, Figma, Photoshop, GIMP

---

## ⚠️ CRITICAL REMINDERS

1. **BACKUP YOUR KEYSTORE**
   - Store in multiple secure locations
   - Save passwords in password manager
   - Loss = cannot update app EVER

2. **Privacy Policy Required**
   - Must be publicly accessible URL
   - Required for Usage Access permission
   - Can use GitHub Pages or Google Sites

3. **Test Before Release**
   - Install release APK on test device
   - Test all features thoroughly
   - Ensure no crashes

4. **Version Management**
   - Increment `versionCode` for each update
   - Update `versionName` for user clarity
   - Document changes in release notes

---

## 📚 Resources

- Full Guide: `DEPLOYMENT_GUIDE.md`
- Privacy Policy: `PRIVACY_POLICY.md`
- Play Console: https://play.google.com/console
- Android Docs: https://developer.android.com/distribute

---

## 🆘 Common Issues

**"Upload failed - Version already exists"**
→ Increment `versionCode` in `build.gradle.kts`

**"App not signed"**
→ Create keystore and configure `keystore.properties`

**"Missing privacy policy"**
→ Host `PRIVACY_POLICY.md` online and add URL

**Build fails**
→ Run `.\gradlew.bat clean` then rebuild

---

## ✅ Final Pre-Submit Checklist

Before clicking "Submit for Review":
- [ ] All store listing sections complete
- [ ] Screenshots uploaded (min 2)
- [ ] Feature graphic uploaded
- [ ] Privacy policy URL added
- [ ] Content rating completed
- [ ] Data safety form filled
- [ ] AAB uploaded successfully
- [ ] Release notes written
- [ ] App tested on device
- [ ] No critical bugs

---

**Ready to deploy? Follow DEPLOYMENT_GUIDE.md for detailed instructions!**

**Questions? Check the full guide or contact Play Console support.**

🎉 Good luck with your launch!

