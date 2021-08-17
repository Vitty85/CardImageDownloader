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

![Screenshot1](https://user-images.githubusercontent.com/53129080/129806547-b01d50ca-e13b-4e4e-b614-ef5cf2cbc61f.jpg)
![Screenshot2](https://user-images.githubusercontent.com/53129080/129806551-c5b9b263-940f-4331-9ca8-d9aeeea173e7.jpg)
![Screenshot3](https://user-images.githubusercontent.com/53129080/129806554-f56b386a-eda2-4b50-a453-0743cdd3de64.jpg)
![Screenshot4](https://user-images.githubusercontent.com/53129080/129806555-14ab7246-0628-4ad0-877e-83acdda21a7d.jpg)
![Screenshot5](https://user-images.githubusercontent.com/53129080/129806557-cbf7e470-9780-4796-aaf4-e8ce4c9e1aa1.jpg)
![Screenshot6](https://user-images.githubusercontent.com/53129080/129806558-a955fc49-6ac7-44a1-a530-99bae25aa71a.jpg)
![Screenshot7](https://user-images.githubusercontent.com/53129080/129806561-a4965dc0-968f-458e-a831-e5831c34d9d2.jpg)
