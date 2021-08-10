# CardImageDownloader
A software to download all card images for Wagic.

Please keep in mind the following notes:

1) Zip is no longer based on 7zip so it should work in all OS.
2) To download all sets of Wagic use the wildcard *.* (as default), otherwise select one or more specific Sets (the combobox will be automatically filled after you choose the Wagic install path).
3) The progress bar will show the progress of the current set download (if you select *.* it will reset and start for each set in your Wagic Res folder).
4) The Res folder of Wagic can now contain both compressed and uncompressed core files in order to allow the Downloader to read the Card Id to download.
5) The Wagic Path has to be readable and writable from the downloader application.
6) The High res images should work just on PC and new Android Devices (for older Android and PSP it's better to use Medium, Low or Tiny).
7) The Downloader automatically detects the Tokens connected to each card and it tries to download them from same set or from any other set with a matching token.
8) During each download cycle a log file will be saved in same folder of Downloader application with all the download process details (warnings, errors and general infos).
9) During each download cycle the user can pause and resume or stop and restart the download process.
10) For each download cycle the user can select to download borderless version of card images.
11) Application has been tested on Windows using JDK 8.
12) To connect https url (such as Gatherer and ScryFall) In some JDK you may need to change the security policy jars (you can find them in lib folder) in the following folder: %JDK_HOME%\jre\lib\security.
13) Before to use the downloader be sure to start Wagic at least one time to create the User folder.
14) This software is distributed as-is, i'm not responsibile for its usage and/or damages it can cause to your PC.

Enjoy ;)
