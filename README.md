
DeepaMehta 4
============

DeepaMehta 4 is a platform for collaboration and knowledge management. The vision of DeepaMehta is a Post-Desktop Metaphor user interface that abolishes applications, windows, files, and folders in favor of stable personal views of contextual content. The goal of DeepaMehta is to provide knowledge workers of all kind a cognitive adequate work environment, right after booting.

Technically DeepaMehta 4 is made of  
Java, Neo4j, Apache Lucene, Apache Felix, Jetty, Jersey (server-side),  
Javascript/AJAX, jQuery, jQuery-UI, HTML5 Canvas, and TinyMCE (client-side).

DeepaMehta 4 is a rewrite of DeepaMehta 2.  
(DeepaMehta 3 was a research & development effort.)

Project website:  
<http://www.deepamehta.de/>

Download, source code:  
<https://github.com/jri/deepamehta>

Wiki, issue tracker:  
<https://trac.deepamehta.de/>

Mailing lists:  
<https://lists.berlios.de/mailman/listinfo/deepamehta-users>  
<https://lists.berlios.de/mailman/listinfo/deepamehta-devel>

Licensed under GNU General Public License, version 3.


**+++ DeepaMehta is still under heavy development! +++**  
Do not use it in any productive and/or professional environment!  
Currently we can not provide an update mechanism for your data. Data you put into the current version of DeepaMehta might not be usable in a later version of DeepaMehta. Please keep this in mind when using this software.  
Thank you for your understanding!


Requirements
------------

**Java 1.6** and a "modern" **webbrowser**.

Works fine with Firefox 3.6 (or newer) and Safari 5. Works mostly fine with Chrome 5 (or newer).  
Doesn't work with IE8. Potentially works with IE9.


Install
-------

Download the latest release and unzip it.  
<https://github.com/jri/deepamehta/downloads/>

A folder `deepamehta-4.0.6` is created.


Start
-----

Open the `deepamehta-4.0.6` folder and use the respective starter script for your platform:

    deepamehta-linux.sh         # choose "Run in terminal"
    deepamehta-macosx.command   # double-click it
    deepamehta-windows.bat      # double-click it

While DeepaMehta starts a terminal window opens and you see some information logged.  
Then a browser window opens and DeepaMehta is ready to use.

If no browser window appears open it manually:  
<http://localhost:8080/>


Stop
----

Go to the terminal window that opened while startup and press:

    Ctrl-C

This puts the database in a consistent state and shuts down the webserver.  
You can close the terminal window now.


Install plugins
---------------

You can extend DeepaMehta's functionality by installing plugins.
See the list of available plugins:  
<https://trac.deepamehta.de/wiki/Plugins>


Update
------

Updating from DeepaMehta 2 or 3 to DeepaMehta 4 is currently not supported.  
Even updating from 4.0.x to 4.0.6 is currently not supported.  
Please see the note at the top.


Reset the database
------------------

Sometimes you might required to restart DeepaMehta with a fresh database.  
To do so a) stop DeepaMehta, b) delete the `deepamehta-db` folder, and c) start again.


Uninstall
---------

Stop DeepaMehta and delete the `deepamehta-4.0.6` folder.  
This removes DeepaMehta completely from your computer, including the database.


Build from Source
-----------------

<https://trac.deepamehta.de/wiki/BuildFromSource>


Version History
---------------

**4.0.6** -- Nov 27, 2011

* New feature for plugin developers:
    * Map type extension architecture: plugins can provide additional map types, e.g. a geo map or a time map.
* Improvements/Fixes:
    * Topicmaps remember their translation.
    * Menus opened within dialog boxes are not constrained to the dialog box dimension.
* Compatible with 2 new plugins (optional install):
    * DM4 Geomaps: displays topics on a geographical map.
    * DM4 Facets: introduces multi-typing to the DM data model.
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.5** -- Oct 20, 2011

* New features:
	* Custom topicmap backgrounds.
	* Functional browser back/forward buttons.
	* Permalinks.
* Fixes:
	* The file related features work on Windows.
	* The file related features work if DeepaMehta runs behind an Apache reverse proxy.
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.4** -- Sep 27, 2011

* New features:
    * File management: View/play your text/image/audio/video/PDF files.
    * GUI customization (user-defined icons and topic labeling rules).
    * Daemon support.
* Improvements:
    * First support for larger topic amounts
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.3** -- Aug 22, 2011

* New features:
    * Webbrowsing: render webpages directly in the content panel or in a separate window/tab.
    * Type search: query all topics of a given type
* Improvements:
    * More intuitive Create menu
    * Create topics via keyboard
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

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
Nov 27, 2011
