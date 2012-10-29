
DeepaMehta 4
============

DeepaMehta 4 is a platform for collaboration and knowledge management. The vision of DeepaMehta is a Post-Desktop Metaphor user interface that abolishes applications, windows, files, and folders in favor of stable personal views of contextual content. The goal of DeepaMehta is to provide knowledge workers of all kind a cognitive adequate work environment, right after booting.

Technically DeepaMehta 4 is made of  
Java, Neo4j, Apache Lucene, Apache Felix, Jetty, Jersey (server-side),  
Javascript/AJAX, jQuery, jQuery-UI, HTML5 Canvas, and CKEditor (client-side).

DeepaMehta 4 is a rewrite of DeepaMehta 2.  
(DeepaMehta 3 was a research & development effort.)

Project website:  
<https://www.deepamehta.de/>

Download, source code:  
<https://github.com/jri/deepamehta>

Documentation, release notes, issue tracker:  
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

Works fine with Firefox 3.6 (or newer) and Safari 5. Works mostly fine with Google Chrome.  
Doesn't work with IE8. Potentially works with IE9 or IE10.


Install
-------

1. Download the latest release from here:  
   <https://github.com/jri/deepamehta/downloads/>
2. Unzip the file.  
   A folder `deepamehta-4.0.12` is created.


Update
------

Updating from DeepaMehta 2 or 3 to DeepaMehta 4 is not supported.  
Even updating from 4.0.x to 4.0.12 is not supported.  
Please see the note at the top.


Start
-----

Open the `deepamehta-4.0.12` folder and use the respective starter script for your platform:

    deepamehta-linux.sh         # choose "Run in terminal"
    deepamehta-macosx.command   # double-click it
    deepamehta-windows.bat      # double-click it

While DeepaMehta starts a terminal window opens and you see some information logged.  
Then a browser window opens and DeepaMehta is ready to use.

If no browser window appears open it manually:  
<http://localhost:8080/de.deepamehta.webclient/>

Login with `admin` and an empty password.


Stop
----

Go to the terminal window that opened while startup and press:

    Ctrl-C

This shuts down the webserver and puts the database in a consistent state.  
You can close the terminal window now.


Install plugins
---------------

You can extend DeepaMehta's functionality by installing plugins.
See the list of available plugins:  
<https://www.deepamehta.de/en/content/download>


Reset the database
------------------

Sometimes you might want to restart DeepaMehta with a fresh database:

1. Stop DeepaMehta.
2. Delete the `deepamehta-db` folder.
3. Start DeepaMehta.


Uninstall
---------

To remove DeepaMehta completely from your computer, including the database:

1. Stop DeepaMehta.
2. Delete the entire `deepamehta-4.0.12` folder.


Build from Source
-----------------

<https://trac.deepamehta.de/wiki/BuildFromSource>


Version History
---------------

**4.0.12** -- Sep 18, 2012

* New features:
    * Access Control foundation.
    * Backend security.
    * File repository.
* New use cases:
    * Publishing of fully interactive read-only topicmaps.
    * Fully closed workgroup installations.
* Improvements:
    * The detail panel lists the topics associated with the selected association.
    * Checkbox Renderer for multiple selection.
    * HTTPS is optional.
* Plugin development framework:
    * Simplified plugin development (more Convention Over Configuration).
    * Fix: Hot Deployment works as expected.
    * The core service processes update requests where aggreagted composite child topics are involved.
    * For handling core events plugins implement listener interfaces instead of overriding methods.
    * A plugin's client-side main file and the custom renderer implementations are namespaced per URI.
    * New renderer type: Multi Renderer.
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.11** -- May 19, 2012

* Improvements:
    * Implicit Saving: the user must no longer care about saving (#243).
    * Consolidated data model: the "many" cardinality is operational (#76).
    * The graphical type editor is fully functional (#77).
    * Nested detail panel rendering (#104).
* Fixes:
    * Links in the Associations list are truncated (#247).
    * Invalid association definitions doesn't prevent the Webclient from starting (#253).
* Plugin development framework:
    * Revised FieldRenderer and PageRenderer APIs lead to more clean plugin code.
    * The core service accepts update requests in 2 formats: canonic and simplified.
* Compatibility with 3 updated plugins:
    * DM4 Kiezatlas 2.0.1, DM4 Geomaps 0.3, DM4 Facets 0.3
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.10** -- Mar 24, 2012

* Fixes:
    * The Back button works cross-topicmap again (#231).
    * A build from scratch with a pristine maven repo works again (#234).
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.9** -- Feb 3, 2012

* Fix:
    * CKEditor works in non-english environments.
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.8** -- Feb 2, 2012

* Improvement:
    * CKEditor as the new WYSIWYG editor. Compared to the formerly used TinyMCE it provides a much better look&feel.
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.7** -- Jan 19, 2012

* Compatibility with 1 new and 2 updated plugins:
    * DM4 Kiezatlas 2.0 (new): a geographical content management system.
    * DM4 Geomaps 0.2 (updated): displays geo-related topics on a geographical map.
    * DM4 Facets 0.2 (updated): introduces multi-typing to the DeepaMehta data model.
* Improvements:
    * Auto-positioned topics appear near selection.
    * New "search result" icon.
* Fixes:
    * The topicmap state (translation) is persistent after auto scroll.
    * Deleted associations resulting from combobox value changes are removed from the canvas.
* See details in the release notes:
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

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
Sep 18, 2012
