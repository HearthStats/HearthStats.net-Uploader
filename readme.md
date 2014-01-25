HearthStats.net Uploader
========================

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

* Make sure you have Java installed (use windows (http://java.com/en/download/manual.jsp) builds)
* Download the __[latest release](https://github.com/JeromeDane/HearthStats.net-Uploader/releases)__ of the HearthStats.net Uploader
* Extract the downloaded zip file to any directory
* Find your __userkey__ from your [HearthStats.net profile page](http://hearthstats.net/profiles) (not currently implemented)
* Edit __config.ini__, replace **your_userkey_here** with the string you found in the previous step, and save the file   
* Run the HearthStatsUploader.jar
* A window should open called "HearthStats.net Uploader"
* Start your Hearthstone client and put it in __windowed mode__ (see [issue #17](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/17))
* Look for notifications in the bottom right of your screen that indicate event detection
* [Report any issues you find](https://github.com/JeromeDane/HearthStats.net-Uploader/issues)

Known Issues
-------------

* This alpha build does not yet submit data to HearthStats.net
* Only supports Windows at the moment
* Game must be running in __windowed mode__ at the moment (see [issue #17](https://github.com/JeromeDane/HearthStats.net-Uploader/issues/17))
* If you don't see the image in the uploader window update to match your game, make sure you have the latest drivers installed for your graphics card.
 
Please see the full [list of known issues](https://github.com/JeromeDane/HearthStats.net-Uploader/issues)
as well.