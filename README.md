
DeepaMehta 4
============

DeepaMehta 4 is a platform for collaboration and knowledge management. The vision of DeepaMehta is a Post-Desktop Metaphor user interface that abolishes applications, windows, files, and folders in favor of stable personal views of contextual content. The goal of DeepaMehta is to provide knowledge workers of all kind a cognitive adequate work environment, right after your desktop computer or laptop has booted up.

Technically DeepaMehta 4 is made of  
Server-side: Java, Neo4j, Felix (OSGi), Jetty, Lucene, Jersey, Thymeleaf (optional), Karaf (optional).  
Client-side: Javascript, jQuery, jQuery-UI, HTML5 Canvas, CKEditor, OpenLayers (optional).

DeepaMehta 4 is a rewrite of DeepaMehta 2.  
(DeepaMehta 3 was a research & development effort.)

Project website:  
<https://www.deepamehta.de/>

Live demo, nightly builds:  
<http://demo.deepamehta.de/>

Issue tracker, documentation, release notes:  
<https://trac.deepamehta.de/>

API documentation:  
<http://api.deepamehta.de/>

Source code:  
<https://github.com/jri/deepamehta>

Mailing lists:  
<https://lists.berlios.de/mailman/listinfo/deepamehta-users>  
<https://lists.berlios.de/mailman/listinfo/deepamehta-devel>  
<https://lists.berlios.de/mailman/listinfo/deepamehta-german>

Licensed under GNU General Public License, version 3.


**+++DISCLAIMER+++**  
DeepaMehta is under heavy development. While you can do productive work with it, DeepaMehta does not meet professional standards yet. At least there are lacks in a) security, b) robustness, and c) usability. Do not put sensitive data in DeepaMehta, in particular when you setup DeepaMehta for network access. Be aware that data loss may occur when you use DeepaMehta improperly. The DeepaMehta developers assume no liability for lost or compromised data. Please keep this in mind when using this software.


---

To install and use DeepaMehta follow 5 mandatory steps:


1. Check requirements
---------------------

* **Java 1.6** (or newer).

  If you don't know weather Java is already installed on your computer or what Java is at all go to
  <http://www.java.com>.

* A "modern" **webbrowser**.

  DeepaMehta works fine at least with Firefox 3.6 (or newer), Google Chrome, or Safari 5 (or newer).  
  Doesn't work with IE8. Possibly works with IE9 or IE10 (not tested).


2. Download DeepaMehta
----------------------

There are 2 distributions to suit different needs:

* The **DeepaMehta Standard Distribution** focuses on small download size and easy setup for single users.  
  <http://download.deepamehta.de/deepamehta-4.1.2.zip> (6.3 MB)

* The **DeepaMehta Karaf Distribution** focuses on client-server setup and supports remote administration.  
  <http://download.deepamehta.de/deepamehta-4.1.2-karaf.tar.gz> (13.4 MB)

Note: the remainder of this README applies to the Standard Distribution.  
For setting up the Karaf Distribution refer to <https://trac.deepamehta.de/wiki/KarafDistribution>.


3. Install DeepaMehta
---------------------

Unzip the downloaded file.  
A folder `deepamehta-4.1.2` is created.

Update note: if you want update an existing DeepaMehta installation continue with "Updating DeepaMehta" now (see below).


4. Start DeepaMehta
-------------------

Open the `deepamehta-4.1.2` folder and use the respective starter script for your platform:

    deepamehta-linux.sh         # choose "Run in terminal"
    deepamehta-macosx.command   # double-click it
    deepamehta-windows.bat      # double-click it

While DeepaMehta starts a terminal window opens and you see some information logged.  
Then a browser window opens and DeepaMehta is ready to use.

To open the DeepaMehta browser window manually:  
<http://localhost:8080/de.deepamehta.webclient/>

Login with `admin` and empty password. Now you're ready to create content.


5. Stop DeepaMehta
------------------

Go to the terminal window that opened while startup and press:

    Ctrl-C

This shuts down the web server and puts the database in a consistent state.  
You can now close the terminal window.


---

Auxiliary tasks follow:


Updating DeepaMehta
-------------------

You can update from DeepaMehta 4.1.x to 4.1.2 while keeping your data:

1. Install DeepaMehta 4.1.2 (see steps 2. and 3. above).
2. Stop DeepaMehta 4.1.x if running. (Also stop DeepaMehta 4.1.2 if already started.)
3. Copy the `deepamehta-db` folder from your DeepaMehta 4.1.x installation into the `deepamehta-4.1.2` folder.
   (Replace as necessary.)
4. Proceed with "Start DeepaMehta" (see step 4. above).

Updating from DeepaMehta 2 or 3 to DeepaMehta 4 is not supported. Even updating from 4.0.x is not supported.
The first updatable DeepaMehta version is 4.1.


Install DeepaMehta plugins
--------------------------

You can extend DeepaMehta's functionality by installing plugins.  
See the list of available plugins:  
<https://www.deepamehta.de/content/download>


Reset the DeepaMehta database
-----------------------------

Sometimes you might want to restart DeepaMehta with a fresh database:

1. Stop DeepaMehta.
2. Delete the `deepamehta-db` folder.
3. Start DeepaMehta.

Caution: you will loose all your data.


Uninstall DeepaMehta
--------------------

1. Stop DeepaMehta.
2. Delete the entire `deepamehta-4.1.2` folder.

This removes DeepaMehta completely from your computer, including all your data.


Build DeepaMehta from Source
----------------------------

<https://trac.deepamehta.de/wiki/BuildFromSource>


---


Version History
---------------

**4.1.2** -- Sep 7, 2013

* Bug fix: editing a topic displayed in a geomap works again (was broken in previous release).
* See the full changelog in the release notes:  
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.1.1** -- Sep 1, 2013

* Timestamps: topics and associations have creation and modification timestamps. Time API.
* Exploiting the browser cache. Makes use of intrinsic HTTP features for reduced network traffic.
* Edit conflict detection supports collaborative work. Fights the "lost update" problem.
* Plugin development framework:
    * Property API for topic/association metadata. Trie-indexing. Range queries.
    * Flexible HTTP response generation in resource methods and event handlers.
* Various bug fixes, in particular login/logout related issues.
* See the full changelog in the release notes:  
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.1** -- Mar 11, 2013

* The first version with a data sustainability guarantee.
    * Data entered in DM 4.1 is guaranteed to be transferred to all future DM4 releases automatically.
* Minor improvements in usability and functionality.
* Crucial access control related bug fixes and other bug fixes.
* Plugin development framework:
    * The core class CompositeValue is attached to the DB.
* See more changes and details in the release notes:  
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.14** -- Jan 30, 2013

* Rewritten storage layer:
    * High-speed traversal: Traversal is significantly speed up by the means of a Lucene index for association metadata.
    * Compact architecture: DM-independent MehtaGraph abstraction. The additional bridging layer is dropped.
    * Modular storage layer: 3rd-party developers can implement alternate storage layers.
    * 1st-class associations: Association user data is indexed as well (just like topic user data).
* Additional performance measures:
    * For DB read operations no transactions are created.
    * Delivering core events involves no runtime reflection.
* See more changes and details in the release notes:  
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

**4.0.13** -- Dec 24, 2012

* New features:
    * The Geomaps plugin is included: display geo-related topics on an OpenStreetMap.
    * Apache Karaf-based distribution for easy client-server setup and maintenance.
    * Association Type Editor: allows the user to create custom association types.
    * For developers: server-side HTML generation with Thymeleaf templates.
* GUI improvements:
    * Cluster move: moving an association on the canvas moves the visually connected subnetwork.
    * More informative Page Panel: topics which are already visible on the canvas are displayed distinctively.
    * Differentiated gestures for revealing topics via the Page Panel: "reveal" vs. "reveals and focus".
    * More clearly arranged association listings in the Page Panel: associations are grouped by type.
    * Keyboard shortcut: create associations via shift-drag.
* Other improvements:
    * Performance: revoke the performance loss introduced in version 4.0.12 in conjunction with Access Control.
    * Updated 3rd-party components, most notably Neo4j 1.2 -> 1.8
* Plugin development framework:
    * Consume services by Java annotations.
    * A plugin's provided service is automatically picked up.
    * More efficient aggregation update logic and idempotent operations.
    * Client-side load mechanism for auxiliary scripts.
* See more changes and details in the release notes:  
  <https://trac.deepamehta.de/wiki/ReleaseNotes>

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
* See more changes and details in the release notes:  
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
* See more changes and details in the release notes:  
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

* Access Control (early state): configurable access privileges for users and groups.
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
    * Application server in the middle-tier (Java), accessible via REST API.
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
<http://deepamehta.newthinking.net/docs/milestones.html>



------------
Jörg Richter  
Sep 7, 2013
