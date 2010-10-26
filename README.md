
DeepaMehta 3
============

DeepaMehta 3 is a platform for collaboration and knowledge management.

Technologically DeepaMehta is made of Java, Neo4j, Apache Lucene, Apache Felix, Jersey, Javascript/AJAX, jQuery, jQuery-UI, TinyMCE, and HTML5 Canvas.

DeepaMehta 3 is a complete rewrite of DeepaMehta 2.

Project website:  
<http://www.deepamehta.de/>

User and developer discussion:  
<http://groups.google.com/group/deepamehta3>


Requirements
------------

* Java 1.6

* A "modern" webbrowser.  
  Works fine with Firefox 3.6 and Safari 5. Works mostly fine with Chrome 5 or higher.  
  Doesn't work with IE8. Potentially works with IE9.


Install
-------

1. Download latest release from here:  
   <http://github.com/jri/deepamehta3/downloads/>

2. Unpack zip archive.  
   A folder *deepamehta3* is created.


Start
-----

1. Open the *deepamehta3* folder and use the respective starter script for your platform:

        deepamehta-linux.sh         # choose "Run in terminal"
        deepamehta-mac.command      # double-click it
        deepamehta-windows.bat      # double-click it

   A terminal window opens and you see some information logged.  
   Wait until the logging has finished.

2. Visit DeepaMehta in your webbrowser:  
   <http://localhost:8080/de.deepamehta.3-client/index.html>


Stop
----

Go to the terminal window that opened while startup and type:

    exit 0

This puts the database in a consistent state.  
You can close the terminal window now.


Update
------

To update a previous DeepaMehta installation and keep all your data:

1. Stop old DeepaMehta (if running).

2. Download and unpack new DeepaMehta at another location (or rename the old DeepaMehta folder before).

3. Replace the new *deepamehta-db* folder with a copy of your old one.

4. Start the new DeepaMehta (by using the respective starter script).  
   DeepaMehta now automatically migrates your old data to the new format.  
   Wait until the logging has finished.

5. **IMPORTANT:** now stop DeepaMehta and start it again (only now the memory cache is up-to-date).  
   Sorry for this extra step!

6. You're done. Visit DeepaMehta in your webbrowser:  
   <http://localhost:8080/de.deepamehta.3-client/index.html>

Please Note:

* **You can only update a stable DeepaMehta release.** Updating a snapshot release is not supported.  
  The first updatable version is DeepaMehta 3 v0.4. Updating DeepaMehta v0.3 is not suppported.

* Keep your old *deepamehta-db* folder at a safe place.  
  You'll need it as a backup when something went wrong while migration.


Uninstall
---------

Stop DeepaMehta and delete the *deepamehta3* folder.  
This removes DeepaMehta completely from your computer, including the database with all your data.


Version History
---------------

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
Oct 26, 2010
