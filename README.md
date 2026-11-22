<div align="center">

<br/>
<br/>

<img src="Logo.png" alt="BitChord+ app icon" width="200" />

# BitChord+

### YouTube Music, Apple Music-style, with your own music server built in

<br/>

[![Latest release](https://img.shields.io/github/v/release/ILostXD/BitChord_Plus?style=for-the-badge&labelColor=0d1117&color=fa233b)](https://github.com/ILostXD/BitChord_Plus/releases)
[![Based on BitChord](https://img.shields.io/badge/based_on-BitChord_v1.10-fa233b?style=for-the-badge&labelColor=0d1117)](https://github.com/kushagrasinghx/BitChord/releases/tag/v1.10)
[![License](https://img.shields.io/github/license/ILostXD/BitChord_Plus?style=for-the-badge&labelColor=0d1117)](LICENSE)
[![Downloads](https://img.shields.io/github/downloads/ILostXD/BitChord_Plus/total?style=for-the-badge&labelColor=0d1117)](https://github.com/ILostXD/BitChord_Plus/releases)

<br/>

[**Download**](#download) · [**What's new**](#whats-new) · [**Companion**](#companion) · [**Features**](#features) · [**Original BitChord**](#original-bitchord) · [**Disclaimer**](#disclaimer)

</div>

> [!IMPORTANT]
> BitChord+ is not affiliated with, endorsed by, or connected to YouTube or Google in any way. Use it at your own discretion.

---

<div align="center">

<img src="Banner.png" alt="BitChord+: Requests, an artist's upcoming album, Pre-Save and an album from your server" width="100%" />

<h1><a id="whats-new"></a>What's new in BitChord+</h1>

Everything [BitChord](https://github.com/kushagrasinghx/BitChord) does, plus your own music server as a first-class part of the app.

<br/>

<table>
  <tr>
    <td width="33%" align="center" valign="top">
      <img src="docs/screenshots/requests.jpg" alt="Requests" width="240" /><br/><br/>
      <b>Requests</b><br/>
      <sub>Ask for any song or album and it lands in your library, lossless only. Artwork, status, retry, swipe to delete with undo, and the companion's settings in one place.</sub>
    </td>
    <td width="33%" align="center" valign="top">
      <img src="docs/screenshots/presave.jpg" alt="Pre-Save" width="240" /><br/><br/>
      <b>Pre-Save</b><br/>
      <sub>Upcoming albums with a live countdown. Pre-Save one and it is downloaded on release day; songs already out play right away.</sub>
    </td>
    <td width="33%" align="center" valign="top">
      <img src="docs/screenshots/album.jpg" alt="Album page" width="240" /><br/><br/>
      <b>Apple Music-style pages</b><br/>
      <sub>Genre, year and the real quality your server has (Lossless, Hi-Res Lossless), disc sections and full-screen covers. Your server plays up to 24-bit FLAC next to YouTube Music.</sub>
    </td>
  </tr>
  <tr>
    <td width="33%" align="center" valign="top">
      <img src="docs/screenshots/artist.jpg" alt="Artist page" width="240" /><br/><br/>
      <b>What's next, on every artist</b><br/>
      <sub>An announced album takes the release card, with a one-tap Pre-Save. Otherwise it shows the newest release of any kind, dated to the day.</sub>
    </td>
    <td width="33%" align="center" valign="top">
      <img src="docs/screenshots/icons.jpg" alt="App icons" width="240" /><br/><br/>
      <b>Its own look</b><br/>
      <sub>A new icon in glass, clear and neon styles for Samsung and Pixel launchers, a glass tab bar and size options.</sub>
    </td>
    <td width="33%" valign="top">

#### Also new
- **Upcoming** on Home, under Top Picks.
- **Playlist sync** to your server, with missing songs requested automatically.
- **Singles to albums**: replace singles when the album arrives, playlists included.
- **Listen Together** on your own companion.

    </td>
  </tr>
</table>

</div>

---

<div align="center">

<h1><a id="companion"></a>BitChord+ Companion</h1>

</div>

Requests, Pre-Save, playlist sync and Listen Together parties run on a small service next to your Navidrome: **[BitChord+ Companion](https://github.com/ILostXD/BitChord_Plus_Companion)**. Set it up there, then add its address under **Sources → your Navidrome**. Only the source with a companion address is used for requests, so any other servers you add stay plain sources.

- **Lossless only.** It searches Soulseek (and Qobuz, if set up) several ways: by album artist, inside the album a song is from, by the album named in "(from …)", and spells out titles like "?" that can't be folder names. If nothing lossless exists, you can let the app add JioSaavn's copy instead; that is off unless you switch it on.
- **Knows your library.** Before downloading, it checks what you already have: another edition, a different credit line, the same album under an artist's other name (Kanye West / Ye). When it is unsure, it asks.
- **Singles to albums.** Optionally, when an album arrives, its songs you have as singles are moved aside and your playlists point at the album versions. It can also ask for an album once you have one, half or all of its singles.
- **Listen Together.** The companion runs BitChord's party server, and the app uses it automatically.

---

<div align="center">

<h1><a id="download"></a>Download</h1>

<a href="https://github.com/ILostXD/BitChord_Plus/releases/latest" target="_blank" rel="noopener noreferrer"><img src="https://cdn.jsdelivr.net/gh/JMcrafter26/badges@main/src/assets/supported/android/cozy.svg" alt="Download for Android" height="55"/></a>

Grab the latest signed APK from [Releases](https://github.com/ILostXD/BitChord_Plus/releases). Most phones want **arm64-v8a**; *universal* runs anywhere. Sideloading needs "Install unknown apps" for the app you download it with.

BitChord+ has its own package name, so it installs **beside** the original BitChord instead of replacing it, and updates itself from this repository. Every release says which BitChord version it is based on.

</div>

---

<div align="center">

<h1><a id="features"></a>Features</h1>

<table>
  <tr>
    <td width="50%" valign="top">

#### Playback
- **Search, browse and play** anything available on YouTube Music.
- **Hi-Res lossless audio** from your server or a module source, with YouTube Music as fallback.
- **Gapless playback with true crossfade**, adjustable 0–12s.
- **Automix [Beta]**: DJ-style transitions with beat-matching and tempo-stretching.
- **Offline downloads** with embedded metadata.
- **Local music library** integration.
- **Apple-like lyrics animation**, credit to [binimum](https://github.com/binimum/am-lyrics).

#### Your library
- **Navidrome and Subsonic servers** as a source, with their library, playlists and favourites in the app.
- **Import Playlist** from your server or Spotify into YouTube Music.
- **Requests, Pre-Save and playlist sync** through the companion.

#### Connectivity & Accounts
- **Sign in with your Google account** for personalized content.
- **Spotify integration**: your playlists and Liked Songs.
- **Discord Rich Presence** and **scrobbling** to Last.fm and ListenBrainz.
- **Listen Together**: share a code and play the same music, in time.

    </td>
    <td width="50%" valign="top">

#### Experience
- **Animated album canvas**: motion artwork on the now-playing screen.
- **Word-synced lyrics** from many providers (full credits in the [original README](https://github.com/kushagrasinghx/BitChord#features)).
- **Dynamic, artwork-driven theming** and a **frosted-glass UI**.
- **Album page backgrounds**: blurred, Apple Music or classic.
- **Navigation options**: tab bar and mini player sizes, Search in the bar or on its own.
- **Replay Year**, Google Drive backup, Radio, Best Part and more from BitChord.

#### Controls & Tweaks
- **Per-network audio quality** for Wi-Fi and mobile data.
- **Playback speed** (0.5×–2.0×) and **skip silence**.
- **Sleep timer**: presets or "stop after this track".
- **System equalizer** integration.
- **Bit-perfect USB exclusive output** to a USB DAC.
- **Stats for nerds** and a live **Audio Pipeline** view: source, codec, bit depth, sample rate.

    </td>
  </tr>
</table>

</div>

---

<div align="center">

<h1><a id="original-bitchord"></a>Original BitChord</h1>

</div>

BitChord+ stands on **[BitChord](https://github.com/kushagrasinghx/BitChord)** by Kushagra Singh. Everything it does that isn't listed above is their work: thank you to Kushagra and every BitChord contributor. Each BitChord release is merged into BitChord+, and the app shows in Settings which one it is based on and whether a newer one is out. If you don't need the server features, use the original: [BitChord releases](https://github.com/kushagrasinghx/BitChord/releases).

The Automix model is downloaded from the original BitChord's releases the first time you turn Automix on. Credit for the lyrics, Automix and USB DAC work is in the original README.

---

<div align="center">

<h1><a id="disclaimer"></a>Disclaimer</h1>

</div>

BitChord+ is an independent, community-driven third-party audio player and client. It is **not** associated with Google LLC, YouTube Music, or any of their parent companies.

* **No media hosting:** BitChord+ does not host, upload, or store copyrighted music files. It is an interface to local storage, public or user-authenticated APIs, and servers you run yourself.
* **Your server, your files:** the companion downloads only what you ask it to, into your own library. You are responsible for making sure that is allowed where you live.
* **Fair use:** this software is made for personal, educational and fair-use purposes. You are responsible for using it in line with your local copyright laws and YouTube's Terms of Service.
* **Copyleft:** BitChord+ is free software under the GPLv3, like BitChord. Any distribution must come with the Corresponding Source under the same license.

## License

Licensed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE).
