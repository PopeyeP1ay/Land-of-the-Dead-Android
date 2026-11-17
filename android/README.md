# LOTD Offline Android — personal WebView test shell

This is a **test wrapper**, not a native Android/ARM64 port. It runs your **existing**
Emscripten/WebAssembly build in Android WebView using WebViewAssetLoader on an app-local
virtual HTTPS origin, with no dependency on your PC server. This project deliberately
includes no game files and no game executables.

## Before building

1. On Windows, confirm your web game works and rebuild any mobile-control changes:
   `python port.py --game ../game web` in `E:\LOTDAndroid\lotd-webport`.
2. Extract this project to `E:\LOTDAndroid\LOTDOfflineAndroid`.
3. Double-click `copy_web_build.bat` in the extracted project (or run it in CMD).
   It copies **only** `index.html`, `index.js`, `index.wasm`, `game.js`, `game.data`,
   and `vnet.js` from your personal web build to `app/src/main/assets/www`.
   Confirm `game.data` appears (roughly 482 MB).
4. Install Android Studio, SDK Platform 36 and JDK 17 (the bundled JDK is fine).
5. Open the project folder in Android Studio, allow Gradle sync, and choose Gradle
   **8.13** if it asks. The source archive has no Gradle wrapper JAR. To generate a
   wrapper from an installed `gradle` CLI, execute `gradle wrapper --gradle-version 8.13`.
   Android Gradle Plugin is pinned to **8.13.2**.
6. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**. Find the debug APK at
   `app/build/outputs/apk/debug/app-debug.apk`.
7. Install that debug APK on your own Android phone for testing (USB debugging or
   transfer the APK and approve installation from that source).

## Design / limitations

- Offline: no INTERNET permission and no remote game server. Online multiplayer
  is deliberately unavailable. Leave the game lobby's **Online room** empty.
- Landscape and immersive; JavaScript, WebGL 2 and IndexedDB/localStorage enabled.
  The existing mobile controls from your generated `index.html` are retained.
- The `game.data` asset is **large (~482 MB)** and WebAssembly starts with **1 GB**
  of allocated memory before file-loading overhead. Chrome working on your phone
  does NOT guarantee Android WebView will run it. It may crash/close with OOM.
- Native WebView's fullscreen API behavior may differ from mobile Chrome. Prefer
  the app's immersive landscape mode without pressing the game's HTML fullscreen button.
- Debug builds can exceed half a gigabyte. You need disk space and a device with
  ample free RAM/storage. APK signing with a debug key is only for testing.
- WebView may have different graphics and WebGL quirks than browser Chrome.
  Check Android Studio Logcat for `LOTD-Web` and `LOTD-Offline` messages if it fails.
- Your browser game progress and the APK's WebView progress use **different origins**;
  saves/profiles generally do not transfer automatically.
- Only use and share this project with game data if you have appropriate rights.
  Do not distribute copyrighted `game.data`, recompiled binaries or derived assets
  without authorization. Source-code licensing of the upstream port also needs review.

## Troubleshooting

- Blank screen: check Logcat and confirm the six assets exist and JavaScript is enabled.
- Crash while loading `game.data`: likely device/browser renderer memory pressure.
  A small downloadable test bundle or staged loading would be the next engineering task.
- Touch inputs fail: rebuild the web app and recopy all six files; this Android
  shell doesn't change the touch bindings.
- App goes blank on HTML fullscreen: exit fullscreen and rely on Android immersive
  mode; upstream web UI may need a change to fullscreen the root page instead
  of only the canvas.
- Audio blocked: check WebView autoplay and Android device media volume.

This source template was prepared without an Android SDK in the authoring environment,
so the resulting APK has not been compiled or device-tested here.
