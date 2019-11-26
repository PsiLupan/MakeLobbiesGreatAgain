[![Build Status](https://travis-ci.org/PsiLupan/MakeLobbiesGreatAgain.svg)](https://travis-ci.org/PsiLupan/MakeLobbiesGreatAgain/)

# WHAT IS MLGA?

MLGA stands for "Make Lobbies Great Again". In any peer to peer lobby, MLGA can be run to view the people you are connected to, see their ping, and toggle a blocked/loved setting for each of them.

*This is a continuation of the original project, adapted to work for any - and every - peer-hosted game.
It supports loading any lists of users you've previously created, but no longer automatically backs up or interacts with any game data.*

## HOW DOES IT WORK?
MakeLobbiesGreatAgain uses a packet capture library to detect STUN packets from any peer-to-peer connection, in order to determine who you're connected to and get ping from. This should work for any Steam API-based game, as the Jingle library is used for the STUN, etc. functions of the Steam API.

It is not detected as a hack, since it does not and will not interact directly with a game ever. 

## APPLICATION SUPPORTS BOTH CLIENTS AND HOSTS

**NOTE**: If you were linked here from another source, such as Reddit, be sure to check for the latest versions for the best quality. You can find all versions here: [MLGA Releases](https://github.com/PsiLupan/MakeLobbiesGreatAgain/releases)

Primary Feature:
* Determining Ping

Optional Features: 
* Double-Click to lock/unlock the overlay for dragging
* Shift + Left Click on a player, highlighted in a darker color for current selection, to toggle to BLOCKED, LOVED, or back to the normal display
* To exit, simply look for the icon in your system tray near the clock, right-click, and select Exit.

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
