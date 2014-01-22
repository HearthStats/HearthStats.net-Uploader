HearthStats.net Uploader
==========================

This is a Java based utility designed to run in the background and automatically
upload your win ratios and other statistics to [HearthStats.net](http://HearthStats.net)
while you play Hearthstone. This program uses screen grab analysis of your Hearthstone window
and does not do any packet sniffing, monitoring, or network modification of any kind.

This project is and will always remain open source so that you can do your own builds if you want
and see exactly what's happening within the program. Feel free to fork this repo if you can hack.
Create a pull request if you make any modifications and they will be merged back into this official
release.

Running Alpha Builds
--------------------

This project is under HEAVY construction, but you can still run alpha builds
to help test things out or just see how things are going. Check outrthe project's
[milestones](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/milestones) 
to see how things are progressing.

* Make sure you have Java installed - install the [windows 32 bit version](http://java.com/en/download/manual.jsp)
* Download the [latest build](https://github.com/JeromeDane/HearthStats.net-Uploader/raw/master/hss-uploader.0.1.20130122.2.jar)
* Start your Hearthstone client and make sure it is running in 1024x768 windowed mode - see [issue #13](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/13)
* Run the .jar file you downloaded above
* You should see a window open up called "HearthStats.net Uploader", and it should mirror your game
* Return to Hearthstone and play as normal
* Look for notifications in the bottom right of your screen that indicate event detection
* [Report any issues you find](https://github.com/JeromeDane/HearthStats.net-Uploader/issues)

Known Issues
-------------

* Only supports Windows at the moment
* Game must be running in 1024x768 windowed mode at the moment - see [issue #13](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/13)
* Fails to correctly grab screenshot on some windows systems - see [issue #12](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/12)
 
Please see the full [list of known issues](https://github.com/JeromeDane/HearthStats.net-Uploader/issues)
as well.