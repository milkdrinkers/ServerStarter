<h1 align="center">ServerStarter (Updated Fork)</h1>
<p align="center">
    <img src="https://img.shields.io/github/license/milkdrinkers/ServerStarter?color=blue&style=for-the-badge" alt="license"/>
    <img alt="GitHub release" src="https://img.shields.io/github/downloads-pre/milkdrinkers/ServerStarter/latest?style=for-the-badge">
    <img alt="GitHub Workflow Status (with event)" src="https://img.shields.io/github/actions/workflow/status/milkdrinkers/ServerStarter/release.yml?style=for-the-badge">
    <img alt="GitHub issues" src="https://img.shields.io/github/issues/milkdrinkers/ServerStarter?style=for-the-badge">
</p>

---

## Description

ServerStarter allows users to install the Minecraft server, mod loader and mods for Minecraft modpacks. It is both an installer and launcher used for operating modded Minecraft servers.  

**Why the fork?** I had multiple issues trying to run the original and found multiple improvements to it spread out on different forks. This fork serves as a cleaned up collection of patches made to the original.

--- 

## Features

- Install Modpacks (*Includes server binaries, mods and configs from modpack files*)
- Handle server launching (*Automatic restarts, java args and more!*)

---

## Usage

Modpack creators should bundle a pre-configured `server-setup-config.yaml`, `startserver.bat` and `startserver.sh`. (*In some environments the script will not run. In those cases users can manually download the latest `ServerLauncher.jar`.*)

Users should execute either of the two scripts (*or the .jar file*) in the same directory as the config to install the server modpack.

---

## Configuration
See `server-setup-config.yaml` for an example config. Both `startserver.bat` and `startserver.sh` can be found included in every release [here](https://github.com/milkdrinkers/ServerStarter/releases).

### Curse API Key
#### 1. Official Method
- Create or login to your CurseForge developer account [here](https://console.curseforge.com/#/login).
- Go to the [developer console](https://console.curseforge.com/#/api-keys) and create a new API key.

#### 2. Unofficial Method (*Unsupported*)
> [!CAUTION]
> This is not endorsed or supported by either Milkdrinkers or Curse.

You may be able to retrieve a key [here](https://git.sakamoto.pl/domi/curseme). 

---

## Credits

- **BloodyMods** - *Creators and contributors to the original project fround [here](https://github.com/BloodyMods/ServerStarter).*
- **EdenLeaf** - *For their improvements and implementation of Modrith support [here](https://github.com/EdenLeaf/ServerStarter).*
- **EnigmaticaModpacks** - *For their improvements made [here](https://github.com/EnigmaticaModpacks/ServerStarter).*
- **Ocraftyone** - *For their improvements made [here](https://github.com/Ocraftyone/ServerStarter-CFCorePatch).*
