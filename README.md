## HOW DOES IT WORK?
MakeLobbiesGreatAgain uses a packet capture library to detect STUN packets from the client and server of Dead By Daylight, in order to determine who you're connected to and get ping from, followed by a website API which provides host information.
It is not detected as a hack, since it does not and will not interact with the game ever.

## APPLICATION SUPPORTS BOTH KILLER AND SURVIVOR
**AS A RESULT, VPN AND PROXY DETECTION HAS BEEN TEMPORARILY REMOVED**

**NOTE**: If you were linked here from another source, such as Reddit, be sure to check for the latest versions for the best quality. You can find all versions here: [MLGA Releases](https://github.com/PsiLupan/MakeLobbiesGreatAgain/releases)

Primary Feature:
* Determining Ping

Optional Features: 
* Double-Click to lock/unlock the overlay for dragging
* Right-click to change from Survivor to Killer mode
* Shift + Right-Click during Survivor mode to have a killer show "BLOCKED"
* Shift + Ctrl + Right-Click during Survivor Mode to show a killer as "LOVED"
* Shift + Alt + Right-Click during Survivor Mode to remove a killer from your list, returning to the normal "Killer Ping"
* To exit, simply look for Jake's face in your system tray near the clock, right-click, and select Exit.

## HOW TO INSTALL AND USE:
**System Requirements:**
* Latest Java Runtime https://java.com/en/download/
* WinPCap from https://winpcap.org

**There is a common error when installing WinPCap, look below for fixes to common errors**

Simply double double click on the MLGA.jar file to run

**If UAC is enabled:** 
You may need to run the application via Command Prompt (this is due to the PCap4J library being unable to find devices).
* Right-click in the same directory as MLGA and create a new text document
* Open it with Notepad and type, javaw -jar MLGA.jar
* Choose Save As and name it MLGA.bat with the option All Files selected
* Right-click the new batch file and Run as Administrator

#### Common Errrors:
While installing WinPCap you may receive an error stating that there is already a previous version installed, and to close all WinPCap programs.
**The Fix:**
* Boot up your system in safe mode
* While in safe mode, re-launch the WinPCap installer
* This time it will give you the option to remove the old version, do so
* Proceed through the rest of the installation
* Reboot your computer normally. Now the installation is done, and you can proceed with Making Lobbies Great Again