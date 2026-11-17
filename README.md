# Land of the Dead — Native Android port (unofficial)

Work-in-progress Android ARM64 adaptation of *Land of the Dead: Road to Fiddler's Green*, tested on HONOR X6c (NIC-LX2) and BlueStacks 5.

## Native Android controls

- Touch movement joystick (WASD), swipe-to-aim, FIRE and ALT
- USE, RELOAD, JUMP, RUN, DUCK
- ESC / Pause and profile-name keyboard input

## What's in this repository

The `native-android/` directory contains the Android launcher and original touchscreen-control integration for the native ARM64 port. This upload does **not** include any original game content, an APK, or third-party statically recompiled game-engine sources. The CMake configuration may need adjustments to remove Windows-specific paths and locate required engine sources on another developer's machine.

The project is a technical prototype, not a standalone reimplementation or a complete buildable game distribution. A legitimate copy of the original PC game is required for personal testing.

## Dependencies and credits

- [lotd-webport by un4ckn0wl3z](https://github.com/un4ckn0wl3z/lotd-webport) (upstream project; check its redistribution permissions before reusing source in public releases)
- [SDL2](https://www.libsdl.org/)

## Legal note

Unofficial fan project. Not endorsed by the original game's publisher or rights holders. Original game binaries, levels, textures, audio and other assets are not distributed here. Redistribution of upstream-derived/recompiled code and APKs requires separate rights review.

## Build status

This is a source snapshot for review and integration, **not a self-contained build**. The CMake file currently points to a local `E:/LOTDAndroid` layout and references `wsock_android_offline.c`, which is not included in this package. The native engine and generated/recompiled game modules are also not included. Do not expect a clean checkout to build or run without separately obtaining permitted dependencies and making path fixes.
