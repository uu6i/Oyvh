# VAMPS MUSIC (Android)

Native Kotlin + Jetpack Compose + Media3 music player for audio stored on the device.

## Build

Requirements: JDK 17, Android SDK platform 36 + build-tools 36.0.0.

```bash
./gradlew :app:assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

The release APK is signed with the auto-generated debug key so it can be sideloaded.
Add your own `signingConfig` before distributing it.

GitHub: push this project and the workflow in `.github/workflows/android-apk.yml`
builds both APKs and uploads them as the `VAMPS-MUSIC-apk` artifact.

## What works (real, on-device)

- MediaStore scan (background thread), permission handling, rescan
- Media3 playback service with media notification, lock-screen controls, audio focus,
  headphone-unplug pause, background playback
- Queue: tap to jump, remove, clear. Shuffle, repeat (off/all/one), seek, previous/next
- Favorites, recently played (persisted locally), library views by songs/favorites/albums/artists
- Search by all fields / title / artist / album
- Sleep timer (minutes or end of current track), runs in the playback service
- Equalizer using the Android audio-effect attached to VAMPS' own playback session
- Edge-to-edge layout, adaptive launcher icon with monochrome layer, splash background

## Not implemented (and the UI says so)

Provider sign-in (Spotify/YouTube/YouTube Music/SoundCloud), lyrics, crossfade, casting,
cross-service search, light theme, queue reordering and playlists. The Connect cards open the
official apps and never claim an account is connected.

## Notes

- `INTERNET` permission was removed because this build makes no network requests. Add it back
  when provider integrations are added.
- Dependency versions: Compose BOM 2025.09.00, Media3 1.8.0, AGP 8.11.1, Kotlin 2.2.20, Gradle 8.13.
