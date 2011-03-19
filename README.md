
DeepaMehta 3
============

DeepaMehta 3 is a platform for collaboration and knowledge management.

Technically DeepaMehta 3 is made of  
Java, Neo4j, Apache Lucene, Apache Felix, Jetty, Jersey (server-side),  
Javascript/AJAX, jQuery, jQuery-UI, HTML5 Canvas, and TinyMCE (client-side).

DeepaMehta 3 is a rewrite of DeepaMehta 2.

Project website:  
<http://www.deepamehta.de/>

Downloads, wiki, issue tracker, source code:  
<http://github.com/jri/deepamehta3>

User and developer discussion:  
<http://groups.google.com/group/deepamehta3>

Licensed under GNU General Public License Version 3.


Requirements
------------

* Java 1.6

* A "modern" webbrowser.

  Works fine with Firefox 3.6 and Safari 5. Works mostly fine with Chrome 5 or higher.  
  Doesn't work with IE8. Potentially works with IE9.


Installing
----------

1. Download latest release from here:  
   <http://github.com/jri/deepamehta3/downloads/>

2. Unpack zip archive.  
   A folder *deepamehta3-v0.4.4* is created.


Starting
--------

1. Open the *deepamehta3-v0.4.4* folder and use the respective starter script for your platform:

        deepamehta-linux.sh         # choose "Run in terminal"
        deepamehta-macosx.command   # double-click it
        deepamehta-windows.bat      # double-click it

   While the server starts a terminal window opens and you see some information logged.  
   Then a browser window opens and DeepaMehta is ready to use.

   Note: to open the DeepaMehta browser window manually use this address:  
   <http://localhost:8080/de.deepamehta.3-webclient/index.html>


Stopping
--------

1. Go to the terminal window that opened while startup and type:

        exit 0

   This puts the database in a consistent state.  
   You can close the terminal window now.


Updating
--------

To update a previous DeepaMehta installation and keep your database:

1. Stop DeepaMehta (if running).

2. Download and unpack the new DeepaMehta version.

3. Replace its *deepamehta-db* folder with a copy of your current one.

4. Start the new DeepaMehta (as described above).  
   Your database automatically migrates to the new format.  
   Wait until the logging has finished.

5. **IMPORTANT:** stop DeepaMehta and start it again (as described above).  
   (Now the memory cache is up-to-date.) Sorry for this extra step!

6. You're done. The DeepaMehta browser window opens automatically.

Please Note:

* **You can only update a stable DeepaMehta release.** Updating a snapshot release is not supported.  
  The first updatable version is DeepaMehta 3 v0.4. Updating DeepaMehta v0.3 is not suppported.

* Keep your old *deepamehta-db* folder at a safe place.  
  You'll need it as a backup when something went wrong while migration.


Uninstalling
------------

Stop DeepaMehta and delete the *deepamehta3-v0.4.4* folder.  
This removes DeepaMehta completely from your computer, including your database.


Version History
---------------

**v0.4.4** -- Jan 3, 2011

* Internal changes only (required to run [DM3 Freifunk Geomap](http://github.com/jri/dm3-freifunk-geomap) 0.3)

**v0.4.3** -- Nov 25, 2010

* Access Control (early state): configurable access privilegs for users and groups.
* Client starts automatically.
* GUI improvement: Create topics on-the-spot via canvas context menu.
* Better search result rendering: Result topics are shown in a dedicated "Search Result" field.
* Better form layout: Text input/editor fields use the entire detail panel's width.

**v0.4.2** -- Oct 26, 2010

* Bug fix: The fulltext search works.

**v0.4.1** -- Oct 16, 2010

* Main features:
    * File handling: Representing local files as topics.
      View/play file contents (text, image, audio, video, PDF) right inside DeepaMehta.
    * *Folder Canvas* plugin (optional installation): Representing a local folder as a synchronizable topicmap.
    * *Nautilus* plugin (optional installation): Put a folder under DeepaMehta control right from the GNOME desktop.
* GUI: canvas size is adjustable by the means of a split pane.
* Bug fixes, e.g. menus are closed when clicked elsewhere.

**v0.4** -- Aug 4, 2010

* Completely new backend architecture:
    * CouchDB is replaced by Neo4j. Storage layer abstraction.
    * Application server in the middle-tier (Java), accessable via REST API.
    * All DeepaMehta modules are OSGi bundles.
    * Plugins can contain both, application logic (server-side Java) and presentation logic (client-side Javascript).
    * DB migration facility.
* Bulk creation tool.
* Hot deployment: install and update plugins on a running system.
* More browsers supported: Firefox, Chrome, and Safari.
* Easy local installation, updating, and deinstallation.

**v0.3** -- Mar 6, 2010

* Persistent topicmaps (plugin *DM3 Topicmaps*)
* Type editor (plugin *DM3 Type Editor*)
* Icon picker (plugin *DM3 Icons*)
* New topic type *To Do* (plugin *DM3 Project*)
* More flexible plugin developer framework

**v0.2** -- Dec 1, 2009

* Framework for plugin developers
* Autocompletion facility
* Topics have icons
* jQuery UI based GUI
* 7 general purpose plugins (*DM3 Time*, *DM3 Workspaces*, *DM3 Contacts*, *DM3 Email*, *DM3 Import*, *DM3 Accounts*, *DM3 Typing*) and 1 custom application (*Poemspace*) available

**v0.1** -- Sep 15, 2009

* Basic functionality (Creating notes, edit, delete. Relate notes to other notes, navigate alongside relations. Attach files to notes. Fulltext searching in notes, also in attachments. Graphical network display of related notes.)


------------
Jörg Richter  
Jan 5, 2011
