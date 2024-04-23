# Minecraft Server File Specification

## What is this?
This is the specification for a File that is supposed to be distributed together or seperated from the modpack.
It is supposed to be used by server launchers (_like this one_) to know what it is supposed to do.

## Why?
You might ask, why not just throw the client files next to a forge installer and then call it a day?
You are correct, you can do this if you set it up on your local server, but that is a lot of manual labor.

But it allows for more:
* Reduced size of the server files when download and uploading to a server.
* As it is not launching the server directly but a subprocess it allows for specifying java args easily, 
    which, might not always be possible on some hosting providers.
* This file format is not bound to any program, modpack, or even programming language!  
    A parser could be written for any other utility program to take care of the special problems specified in the file.      
* With the use of wildcard options and regex selectors it could be made to even work across modpack versions.

## Format
See `server-setup-config.yaml` for a example file how this file should be layouted.

## How is this fork different than the original?
This fork has been modified to use the official CurseForge API. This has become necesarry due to the recent shutdown of the cursemeta API (a third-pary CurseForge API). With this change, an API key is needed. There are two ways to get an API key which are layed out below.

## 1. Official Way
The official way is to create a CFCore account and generate an API key. This is the easiest and most straight forward way, but CurseForge allows mod/modpack creators to disable third-parties from being able to access the download URL through the API, so some mods may not be able to be downloaded automatically.

~~## 2. The Absolutly-Not-Official Way~~ Broken Now. Use the Sequel!
~~The Absolutly-Not-Official way is to extract the key that the official CurseForge uses to access the CFCore API. Credit for this goes to this repo: https://git.sakamoto.pl/domi/curseme/-/tree/meow. If you are on linux, there is a script in that repo called getToken.sh that can be run to automate that process. From what I can tell, it is legit, but use your judgement and check out the code. For those on other OSes or who don't want to run that script:~~
~~1. Download the Linux verson of the CurseForge client from https://curseforge.overwolf.com/. (Direct download: https://curseforge.overwolf.com/downloads/curseforge-latest-linux.zip)~~
~~2. Open the zip using 7-zip or a simmilar program.~~
~~3. Double click on the .AppImage to open it.~~
~~4. Navigate to resources/app/dist/desktop and open desktop.js in an editor other than notepad (the file is too large and it crashes notepad)~~
~~5. Search the document for `"cfCoreApiKey":` (quotes and semicolons included). This will return a single result. Copy the value in the quotes folowing the semicolon. 
    `"cfCoreApiKey": "#COPY THE VALUE THAT IS HERE#"`~~
~~6. Take that value and paste it into your `server-setup-config.yaml` in the `curseForgeAPIKey` field~~

## 3. The Absolutly-Not-Official Way (The Sequel) 
This is similar, but updated for the modern CurseForge app. The CurseForge app now packages the resources in an asar archive, so you need to extract the archive first, then retrieve the file.
1. Download the Linux verson of the CurseForge client from https://curseforge.overwolf.com/. (Direct download: https://curseforge.overwolf.com/downloads/curseforge-latest-linux.zip)
2. Install NodeJS from nodejs.org or your package manager.
3. Open the zip using 7-zip or a simmilar program.
4. Double click on the .AppImage to open it.
5. Navigate to the resources folder and copy the app.asar file to a working directory
6. Open your terminal and navigate to the working directory with the app.asar
7. Run `npx @electron/asar extract app.asar app`. This will extract the contents of app.asar to a folder called app.
8.  Navigate to app/dist/desktop and open desktop.js in an editor other than notepad (the file is too large and it crashes notepad)
9. Search the document for `"cfCoreApiKey":` (quotes and semicolons included). This will return a single result. Copy the value in the quotes folowing the semicolon. 
    `"cfCoreApiKey": "#COPY THE VALUE THAT IS HERE#"`
10. Take that value and paste it into your `server-setup-config.yaml` in the `curseForgeAPIKey` field
