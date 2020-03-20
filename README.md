## Player for Monstercat

![Icon](https://raw.githubusercontent.com/lucaspape/catplayer/master/playstore_res/icon-round-full.png)

https://pixabay.com/illustrations/black-panther-figure-3704552/ - Black Panther Figure by Victoria Borodinova

This android app aims to implement the Monstercat API.

## Download
<a width="40%" href='https://play.google.com/store/apps/details?id=de.lucaspape.monstercat&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>

## Features

 - Manage and play your playlist from monstercat.com
 - Shuffle and repeat playback
 - Download songs (with Monstercat Gold)
 - Stream songs early (with Monstercat Gold)
 - Browse in catalog and in album view
 - Search songs
 - Search for artist by tapping on text in fullscreen-view
 - Dark Mode (Android 9 or higher)
 - Live Radio from Twitch
 - FLAC download support
 - Crossfade

<img src="https://github.com/lucaspape/catplayer/raw/master/playstore_res/screenshots/20200316/Screenshot_20200316-154827_Catplayer.png" width="40%">  <img src="https://github.com/lucaspape/catplayer/raw/master/playstore_res/screenshots/20200316/Screenshot_20200316-154918_Catplayer.png" width="40%">

All album images seen in these screenshots are Copyright Monstercat.

## API

This app uses the official Monstercat Connect API for most functions but for some features a custom API is used.
This custom API is open source and available [here](https://github.com/lucaspape/catplayer-helper-api.git).

## Todo

 - Retain position of list
 - Favorites
 - More song info
 - Browse by artist
 - Mirror playlists from spotify / public playlists

## Beta

If you want to participate in the beta test you can download it from the google play store [here](https://play.google.com/apps/testing/de.lucaspape.monstercat "Test Android-App").
The beta is open but currently limited to 1000 participants.

## Build from source

To build this app from source you will need Android Studio and the ExoPlayer V2 sourcecode as well as the ExoPlayer FLAC extension. Please refer to [here](https://github.com/google/ExoPlayer/blob/release-v2/README.md) and [here](https://github.com/google/ExoPlayer/tree/release-v2/extensions/flac) for more information on how to get the ExoPlayer V2 and the FLAC extension source code. 

You can also build this project without FLAC support by using this patch: [no_flac_support.patch ](https://gist.github.com/lucaspape/6b9f537cb3e3b3c337bab5e09eb6ebff)(you won't need the ExoPlayer sourcecode)

Please also read the developers guide ([DEVELOPERS_GUIDE.txt](https://github.com/lucaspape/catplayer/blob/master/DEVELOPERS_GUIDE.txt)) in this project.

## Additional information
Disclamer: this is not an official app from monstercat.com!
I am not responsible in any way for the content displayed in this app!
Monstercat is a trademark from Monstercat inc.
Google Play and the Google Play logo are trademarks of Google LLC.

