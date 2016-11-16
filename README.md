# MakeLobbiesGreatAgain
MLGA is a tool for Dead By Daylight which checks the provided network interface for packets coming from Dead By Daylight's lobby joining. 
Once detected, the tool will lookup the killer's IP and return the country code in a transparent overlay.

## Dependencies
* pcap4j requires WinPCap to be installed and on the PATH environment variable

## Known Issues
* The overlay will cause the game rendering in Fullscreen (technically Borderless Windowed mode) to not hide the task bar.
	- Workaround: In Windows 10, you can right-click the Task Bar, go to Settings, and enable "Automatically hide the taskbar in desktop mode."
				  This causes the task bar to only be displayed if moused over.
				  Alternatively, "Show taskbar on all displays" can be disabled if using multiple monitors.