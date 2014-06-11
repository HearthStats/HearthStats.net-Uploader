[![Build Status](https://api.travis-ci.org/HearthStats/HearthStats.net-Uploader.png)](https://travis-ci.org/HearthStats/HearthStats.net-Uploader)
[![Stories in Ready](https://badge.waffle.io/HearthStats/HearthStats.net-Uploader.png?label=Ready)](https://waffle.io/HearthStats/HearthStats.net-Uploader)

HearthStats.net Uploader
========================

This is a Java based utility designed to run in the background and automatically
upload your win ratios and other statistics to [HearthStats.net](http://HearthStats.net)
while you play Hearthstone. This program uses screen grab analysis of your Hearthstone window
and does not do any packet sniffing, monitoring, or network modification of any kind.

This project is and always will be open source so that you can do your own builds 
and see exactly what's happening within the program. 


Features
--------------------

* Support for both Windows and OS X
* Automatically tracks constructed and arena matches
* Tracks your class and your opponent's class
* Tracks your rank level for ranked matches
* Tracks your opponent's name
* Tracks number of rounds played and match duration
* Add notes to your matches directly from the uploader

See the [Development Status](https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Development-Status) wiki page for more detail about what is currently supported.


Running Beta Builds
--------------------

This project is under HEAVY construction, but you can still run beta builds
to help test things out or just see how things are going. Check out the project's
[milestones](https://github.com/HearthStats/HearthStats.net-Uploader/issues/milestones) 
to see how things are progressing.

* Make sure you have [Java 7](http://java.com/en/download/manual.jsp) installed
* Download the __[latest release](https://hearthstats.net/uploader)__ of the HearthStats.net Uploader
* Extract the downloaded zip file to any folder
  * On **Windows**, run HearthStats.exe in the folder where you extracted the zip file
  * On **Mac OS X**, run HearthStats in that folder, or from the Applications folder if you prefer
* A window should open called "HearthStats.net Uploader"
* Start your Hearthstone client 
  * On **Windows**, put Hearthstone in **windowed mode** (see [issue #17](https://github.com/HearthStats/HearthStats.net-Uploader/issues/17))
  * On **Mac OS X**, you can run Hearthstone in **windowed mode** or **full-screen**
* Look for notifications in the corner of your screen that indicate event detection
  * Notifications will only appear in windowed mode, not in full-screen mode
* [Report any issues you find](https://github.com/HearthStats/HearthStats.net-Uploader/issues)



Known Issues
-------------

* On Windows, Hearthstone must be running in __windowed mode__ for now (see [issue #17](https://github.com/HearthStats/HearthStats.net-Uploader/issues/17))
* On 64-bit Windows you need to install [Visual C++ Redistributable for Visual Studio 2012](http://www.microsoft.com/en-us/download/details.aspx?id=30679) if you don't have it already
 

Please see the full [list of known issues](https://github.com/HearthStats/HearthStats.net-Uploader/issues)
as well.

Contributing to this Project
----------------------------

There are several things you can do to help this project out:

* Check out and participate in [the reddit thread](http://www.reddit.com/r/hearthstone/comments/1wa4rc/auto_uploader_for_hearthstatsnet_need_help_testing/)
* Download and test early builds, making sure to [report any issues you find](https://github.com/HearthStats/HearthStats.net-Uploader/issues)
* Fork this repository if you can hack, and create a pull request if you come up with things to contribute.
* [Buy Jerome a cup of coffee via PayPal](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=F9XNSXLZNP9QQ) for his work on this uploader
* Donate to HearthStats.net's founder Jeff through the site's [about us page](http://hearthstats.net/aboutus) 
