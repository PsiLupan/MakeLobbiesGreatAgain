[![Build Status](https://travis-ci.org/PsiLupan/MakeLobbiesGreatAgain.svg)](https://travis-ci.org/PsiLupan/MakeLobbiesGreatAgain/)

## HOW DOES IT WORK?
MakeLobbiesGreatAgain uses a packet capture library to detect STUN packets from the client and server of Dead By Daylight, in order to determine who you're connected to and get ping from.
It is not detected as a hack, since it does not and will not interact with the game ever. 

In an official statement by the developers, MLGA is the the only application they have whitelisted and verified with EAC. 
Source: http://steamcommunity.com/app/381210/discussions/0/1319962683448307108/

## APPLICATION SUPPORTS BOTH KILLER AND SURVIVOR

**NOTE**: If you were linked here from another source, such as Reddit, be sure to check for the latest versions for the best quality. You can find all versions here: [MLGA Releases](https://github.com/PsiLupan/MakeLobbiesGreatAgain/releases)

Primary Feature:
* Determining Ping

Optional Features: 
* Double-Click to lock/unlock the overlay for dragging
* Shift + Left Click on a player, highlighted in a darker color for current selection, to toggle to BLOCKED, LOVED, or back to the normal display
* To exit, simply look for Jake's face in your system tray near the clock, right-click, and select Exit.

## HOW TO INSTALL AND USE:
**System Requirements:**
* Latest Java Runtime https://java.com/en/download/
* Npcap from https://nmap.org/npcap/ and tick "Install Npcap in WinPcap API-compatible Mode" during installation (For advanced users: Add %SystemRoot%\System32\Npcap\ to PATH instead.)

Simply double double click on the MLGA.jar file to run

**NOTE:** You may need to right-click the JAR file, select Properties, and choose Unblock if it appears below Attributes.

**If UAC is enabled:** 
You may need to run the application via Command Prompt (this is due to the PCap4J library being unable to find devices).
* Copy the folder path that MLGA is in, for example: C:\Users\Dwight\Desktop\MLGA\
* Right-click in the same directory as MLGA and create a new text document
* Open it with Notepad and type, cd C:\The\Path\You\Copied\Earlier
* Start a new line with Enter and type, javaw -jar MLGA.jar
* Choose Save As and name it MLGA.bat with the option All Files selected
* Right-click the new batch file and Run as Administrator

## HOW TO SUBMIT A DEBUG LOG
* Right-click in the same directory as MLGA and create a new text document
* Open it with Notepad and type, java -jar MLGA.jar
* Choose Save As and name it MLGADebug.bat with the option All Files selected
* Right-click the new batch file and Run as Administrator
* Submit a picture or copy of the text to an Issue

## ALTERNATIVE WAYS TO CONTACT ME
* My GMail is wcarter312@gmail.com
* My Discord is psiLupan#0316
* My Steam account is http://steamcommunity.com/profiles/76561197995173996