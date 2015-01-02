# Introduction

Shelves is an Android application that manages your collection of apparel, board games, books, comics, gadgets, movies, music, software, tools, toys, and video games.

With Shelves, you can add items in a number of ways, such as:

* By scanning barcodes
* From the Internet
* Entering details manually

You can also import and export data from a variety of sources, including Google Books, Google Drive, BoardGameGeeks.com, or Shelfari. There's also full support for software such as Dropbox, Delicious Library, and MediaMan.

Shelves lets you organize your collection by:

* Providing sorting rules on title, price, author, or format (CD, DVD, Vinyl, e.t.c.), and more
* Searching across all available item attributes
* Tagging and filtering your collection
* Allowing for the selection of multiple items

You can loan items to friends in your contact list, or rate items for your personal use. You can also opt to place items in a separate wishlist category.

[Grab the app on Google Play](https://play.google.com/store/apps/details?id=com.miadzin.shelves&hl=en)!

## Differences Between This App and the Published Version

Although the code is open-source, there are a few differences between it and the version distributed through Google Play:

* All item lookups are not supported.  
In order to perform a lookup for item information, Shelves pings a server (that I own) that does most of the "hard work." The server then returns the results in an XML format for Shelves to parse and represent. I can't provide access to this server publicaly, since it'll make my costs skyrocket. However, you can run your own server, and implement your own lookups.
This doesn't affect board games or comic books.

* License checks are not enforced in the open source version. It'll simply always show the ads.
This code automatically assumes that the app is not paid for. However, you can easily set a flag to just enable all the features and remove ads. You'll need to find references in `LicenseCheck` and just have the result set to `true`. License checking is currently only used in two places: `TabSelector` and `UIUtilities`.

## Dummy Data

If you wish to play around with some Shelves data, the _databases_ folder contains some simple databases for each item type. You'll want to place these in your phone's _data/data/miadzin.shelves/databases_ folder. This is impossible for real devices that are not rooted, but emulators and rooted phones can access the folder through DDMS. 

## Wishlist

Changes that I'd like to implement--in no particular order--include:

* Removing deprecated code; this mostly depends on converting over to Fragments
* Updating a lot of the UI
* Finding a good online Wine or Beer API
* Adding more translations
* Integrating a barcode scanner directly into the app 

## A Warning About How Terrible the Code is

When I started this project, I knew very little Java, and had no Android knowledge.

As such, there are probably a bunch of things that could be abstracted and improved upon. All of the code was written in my spare time--I made no glamorous life off of this project. I also only own one Android device, so I did my best writing code that covered as many use cases as possible.

## History

Shelves began as [a sample project by Romain Guy](http://www.curious-creature.org/2009/01/19/shelves-an-open-source-android-application/) in January 2009. It was used to demonstrate various capabilities of the Android OS, way back on Cupcake (1.5). It only managed book data.

After it became apparent that the project would not be distributed through Google Play, I brought out my own fork in April 2010. Over the years, I added much more functionality, and gained a lot of insight into dealing with the quirks of various Android devices, versions, form factors, and manufacturers. 

Unfortunately, it's gotten to the point where Shelves is simply too difficult for one person to maintain. Rather than abandon the project, I opted to make most of my code open-source. And here we are. The only closed-source implementation is the actual server-side lookup, as described above--but it shouldn't be too hard to figure out what to do.

My infinite gratitude to the people who downloaded and supported this app,  as well as everyone who offered words of praise or encouragement. It has 32,000 active users (more than the population of the town I grew up in) and over 100,000 downloads (a figure much higher than I can count). It was for you all that I kept this app going for so long

## Donation

If this code or app helped you out in any way, you might want to consider [buying the unlocker on Google Play](https://play.google.com/store/apps/details?id=com.miadzin.shelves.unlocker&hl=en). It'd be a nice gesture! :grinning:

## License

```
Copyright (C) 2010 Garen J. Torikian

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
