HearthStats.net Uploader
==========================

This is a Java based utility designed to run in the background and automatically
upload your win ratios and other statistics to [HearthStats.net](http://HearthStats.net)
while you play Hearthstone. This program uses screen grab analysis of your Hearthstone window
and does not do any packet sniffing, monitoring, or network modification of any kind.

This project is and always will be open source so that you can do your own builds 
and see exactly what's happening within the program. Feel free to fork this repo if you can hack.
Create a pull request if you make any modifications and they will be merged back into this official
release.

Running Alpha Builds
--------------------

This project is under HEAVY construction, but you can still run alpha builds
to help test things out or just see how things are going. Check out the project's
[milestones](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/milestones) 
to see how things are progressing.

* Make sure you have Java installed (use the [windows 32 bit version](http://java.com/en/download/manual.jsp) builds)
* __[Download latest build](https://github.com/JeromeDane/HearthStats.net-Uploader/raw/master/hss-uploader.0.1.20130122.2.jar)__ of the HearthStats.net Uploader
* Start your Hearthstone client and put it in __windowed mode__ (see [issue #17](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/17))
* Run the .jar file you downloaded above
* A window should open called "HearthStats.net Uploader"
* Return to Hearthstone and play as normal
* Look for notifications in the bottom right of your screen that indicate event detection
* [Report any issues you find](https://github.com/JeromeDane/HearthStats.net-Uploader/issues)

Known Issues
-------------

* Only supports Windows at the moment
* Game must be running in __windowed mode__ at the moment (see [issue #17](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/17))
* Fails to correctly grab screenshot on some windows systems - see [issue #12](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/12)
 
Please see the full [list of known issues](https://github.com/JeromeDane/HearthStats.net-Uploader/issues)
as well.