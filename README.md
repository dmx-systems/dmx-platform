
DeepaMehta 4
============

DeepaMehta 4 is a platform for collaboration and knowledge management.

Technically DeepaMehta 4 is made of  
Java, Neo4j, Apache Lucene, Apache Felix, Jetty, Jersey (server-side),  
Javascript/AJAX, jQuery, jQuery-UI, HTML5 Canvas, and TinyMCE (client-side).

DeepaMehta 4 is a rewrite of DeepaMehta 2.  
(DeepaMehta 3 was a research & development effort.)

Project website:  
<http://www.deepamehta.de/>

Downloads, source code:  
<https://github.com/jri/deepamehta>

Wiki, issue tracker:  
<https://trac.deepamehta.de/>

User and developer discussion:  
<https://groups.google.com/group/deepamehta3>

Licensed under GNU General Public License Version 3.


Requirements
------------

**Java 1.6** and a "modern" **webbrowser**.

Works fine with Firefox 3.6 (or newer) and Safari 5. Works mostly fine with Chrome 5 (or newer).  
Doesn't work with IE8. Potentially works with IE9.


Install
-------

Download the latest release and unzip it.  
<https://github.com/jri/deepamehta/downloads/>

A folder `deepamehta-4.0.2` is created.


Start
-----

Open the `deepamehta-4.0.2` folder and use the respective starter script for your platform:

    deepamehta-linux.sh         # choose "Run in terminal"
    deepamehta-macosx.command   # double-click it
    deepamehta-windows.bat      # double-click it

While DeepaMehta starts a terminal window opens and you see some information logged.  
Then a browser window opens and DeepaMehta is ready to use.

Hint: if no browser window appears open it manually:  
<http://localhost:8080/de.deepamehta.webclient/index.html>


Stop
----

Go to the terminal window that opened while startup and type:

    stop 0

This puts the database in a consistent state.  
You can close the terminal window now.


Update
------

To update from DeepaMehta 4.0 or 4.0.1 to 4.0.2 while keeping your content: before starting DeepaMehta copy your `deepamehta-db` folder into the `deepamehta-4.0.2` folder. (If you've started DeepaMehta 4.0.2 already, stop it, and replace its `deepamehta-db` folder with your one, and start again.)

Updating from DeepaMehta 2 or 3 to DeepaMehta 4 is currently not supported.


Start with a fresh database
---------------------------

Stop DeepaMehta, remove the `deepamehta-db` folder, and start again.


Troubleshooting
---------------

If the browser shows 404 or 503 while an exception appears in the terminal: refresh the `Apache Felix Http Jetty` bundle by typing `refresh 1` in the terminal (checkout the proper bundle ID before by using the `lb` command).


Uninstall
---------

Stop DeepaMehta and delete the `deepamehta-4.0.2` folder.  
This removes DeepaMehta completely from your computer, including the database.


Build from Source
-----------------

<https://trac.deepamehta.de/wiki/BuildFromSource>


Version History
---------------

**4.0.2** -- Aug 2, 2011

* Bug fix: proper character encoding (when using the binary distribution in a client/server setup)

**4.0.1** -- Jul 28, 2011

* Bug fix: retyping an association does not compromise role types

**4.0** -- Jul 24, 2011

* Complete new property-less data model. All values are represented as reusable semantics-attached topics.  
  <https://groups.google.com/group/deepamehta3/browse_thread/thread/a77704d35e7af539>
* Improvements for users:
    * new type editor
    * new topic type "Resource" for collecting web resources
    * cleaner display, less clutter
    * new icons
* Improvements for developers:
    * New core service API. All the domain objects (Topic, Association, ...) are attached to the database.
      So, the developer must not care about DB-updates.
    * Domain models (type definitions) provided by a plugin can be build upon the domain models
      provided by other plugins. The dependencies are handled by the framework.

  The version planned as "DeepaMehta 3 v0.5" eventually become "DeepaMehta 4.0".  
  From now on DeepaMehta version numbers follow the classic *major*.*minor*.*bugfix* schema.

---

**DeepaMehta 3**:

**v0.4.5** -- May 1, 2011

* Under the hood changes to support (new) developers:
    * Complete new build system based on pure Maven and Pax Runner (no shellscripts anymore).
    * Easy from-scratch setup of development environment. No manual Felix installation required.
    * Example plugin project included.
    * Testing support: Prepared test environments for your own tests. Core tests included.
    * Hot deployment: Changes are deployed immediately. No Felix shell interaction required (in most cases).
* Under the hood core implementation changes:
    * Plugins publish their public API as OSGi service, consumable by other plugins.
    * Plugin and Core services are published at 2 endpoints (HTTP/REST and OSGi) solely by annotation.

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

---

Version history of **DeepaMehta 1** and **DeepaMehta 2**:  
<http://www.deepamehta.de/docs/milestones.html>



------------
Jörg Richter  
Aug 2, 2011
