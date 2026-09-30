# 🎓 OpenPrep - Bring Your Own Server (BYOS) EdTech Platform

OpenPrep is a 100% Free and Open Source Software (FOSS) alternative to proprietary, DRM-locked test preparation applications. 

Instead of paying for subscriptions or being tracked by analytics, OpenPrep acts as a **smart, lightweight shell**. You host your own educational content (Videos, PDFs, and Quizzes) on your own server, and OpenPrep streams and tracks your progress locally.

## ✨ Features
* 🔒 **Absolute Privacy:** Zero tracking, zero analytics, zero ads.
* ☁️ **BYOS Architecture:** Host your JSON manifest on Nextcloud, GitHub Gist, AWS S3, or any basic web server.
* 🎥 **Native Video Streaming:** Powered by AndroidX Media3 (ExoPlayer).
* 📝 **Interactive Mock Tests:** Built-in JSON-powered Quiz Engine with instant grading.
* 💾 **Offline Downloads:** Download videos and PDFs directly to your device via Android's native Download Manager.
* 🔖 **Progress & Bookmarks:** Tracks your watched videos, best quiz scores, and bookmarks locally using Jetpack DataStore.
* 📱 **Modern UI:** Built entirely with Jetpack Compose (Material 3).

## 🚀 How to Use (For Students)
1. Download the latest `app-debug.apk` from the **Releases** tab.
2. Install the app on your Android device.
3. On the setup screen, paste the URL to your content server (the folder containing your `course_manifest.json`).
4. Start learning!

## 📂 Content Creators: How to format your Server
To use OpenPrep, host a file named `course_manifest.json` on your server.

**Example `course_manifest.json`:**
```json
{
  "courseName": "OpenSource CA Prep",
  "version": "1.0",
  "subjects": [
    {
      "id": "s1",
      "title": "Advanced Accounting",
      "modules": [
        { 
          "id": "m1", 
          "title": "Chapter 1 Lecture", 
          "type": "video", 
          "url": "videos/chap1.mp4" 
        },
        { 
          "id": "m2", 
          "title": "Chapter 1 Notes", 
          "type": "pdf", 
          "url": "notes/chap1.pdf" 
        },
        { 
          "id": "m3", 
          "title": "Chapter 1 Mock Test", 
          "type": "qbank", 
          "url": "quizzes/chap1.json" 
        }
      ]
    }
  ]
}
```

**Example Quiz JSON (`quizzes/chap1.json`):**
```json
{
  "title": "Chapter 1 Mock Test",
  "questions": [
    {
      "id": "q1",
      "text": "What is the primary goal of OpenPrep?",
      "options": ["Collect data", "Provide FOSS education", "Show ads"],
      "correctOptionIndex": 1,
      "explanation": "OpenPrep is designed to be free, open-source, and private."
    }
  ]
}
```

## 🛠 Building the App
This app is designed to be built automatically using GitHub Actions. Simply fork this repository, and push to the `main` branch to trigger a new APK build!
