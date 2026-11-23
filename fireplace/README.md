# Fireplace

A small Android TV app for the Google TV Streamer. Each scene, like *Fireplace Jazz* or *Snowy Cabin*, is its own tile in Projectivy, and its own name for "Hey Google, open …". Opening a tile hands the scene's YouTube link to **SmartTube**, falling back to the official YouTube app. The **Fireplace** tile opens a menu with every scene.

## Install on the TV

1. Install SmartTube on the Streamer if you haven't already.
2. Get the APK from the fixed link
   `https://github.com/stijntratsaertit/stijntratsaertit/releases/download/fireplace-latest/fireplace.apk`
   - **On the TV:** install the *Downloader* app (by AFTVnews) from the Play Store, allow it to install unknown apps (Settings → Apps → Security & restrictions), and enter the link above.
   - **Or from a computer:** download the APK, turn on developer mode and network debugging on the TV, then run `adb connect <tv-ip>` and `adb install -r fireplace.apk`.
3. The tiles appear in Projectivy. Add the ones you want to your favourites row.

Updates install over the old version, because every build is signed with the same key (`sideload.keystore`).

## Change your scenes

Edit [`config/scenes.json`](config/scenes.json) (on github.com works fine) and push. GitHub Actions builds a new APK at the same link; install it again the same way.

- `url`: any YouTube link. The defaults are **search links** so they work out of the box. Once you find a video or playlist you like, paste its link instead. Playlists give the most variety.
- `tile`: `true` gives the scene its own launcher tile; `false` keeps it only in the Fireplace menu.
- `id`: lowercase, stays fixed once installed. Changing it creates a new tile.
- `color`: the tile banner's colour. Banners are drawn automatically from the title and subtitle.
- `players`: the apps to try, in order. The first entries are SmartTube's stable and beta package names.

## Google Home

- "Hey Google, open Fireplace Jazz on the living room TV" should launch that tile. Test it once; sideloaded apps aren't always recognised by voice.
- Routines: add the same "open …" command as a custom action, e.g. a "cozy time" routine that also dims the lights.
- Pause and volume from Google Home depend on SmartTube, since SmartTube is doing the playing.
