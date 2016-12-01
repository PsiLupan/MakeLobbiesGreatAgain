# MakeLobbiesGreatAgain
MLGA is a tool for Dead By Daylight which checks the provided network interface for packets coming from Dead By Daylight's lobby joining. 
Once detected, the tool will lookup the killer's IP and return the country code in a transparent overlay.

To run, download the JAR, then double-click MLGA.jar.
* If UAC is enabled, try running as Administrator if overlay doesn't display after clicking Start.

* Draggable overlay. Enabled/Disabled by double-clicking. Changes color to make it obvious if active.
* Change between country code and country name by right-clicking. Name is not truncated, so long countries can overflow from the box.
* To exit, simply right-click Jake's face in the system tray (near the clock) and select Exit.

## Dependencies
* pcap4j requires WinPCap to be installed and on the PATH environment variable

## Known Issues
* None currently