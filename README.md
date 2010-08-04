
DeepaMehta 3
============

DeepaMehta 3 is a platform for collaboration and knowledge management.

Technologically DeepaMehta is made of Java, Neo4j, Apache Lucene, Apache Felix, Javascript/AJAX, jQuery, jQuery-UI, and HTML 5 Canvas.
DeepaMehta 3 is a complete rewrite of DeepaMehta 2.

<http://www.deepamehta.de/>


Requirements
------------

* Java 1.5 or higher.  

* A "modern" webbrowser.  
  Tested with Firefox 3.6, Safari 4, Safari 5.


Install
-------

1. Download latest release from here:  
   <http://github.com/jri/deepamehta3/downloads/>

2. Unpack zip archive.  
   A folder 'deepamehta3' is created.


Start
-----

1. Open the 'deepamehta3' folder and use the respective starter script for your platform:

        deepamehta-linux.sh         # choose "Run in terminal"
        deepamehta-mac.command      # double-click it
        deepamehta-windows.bat      # double-click it

   A terminal window opens and you see some information logged.

2. Open DeepaMehta in webbrowser:  
   <http://localhost:8080/de.deepamehta.3-client/index.html>


Stop
----

Go to the terminal window that opened while startup and type:

    exit 0

This puts the database in a consistent state.  
You can close the terminal window now.


Update
------

1. Start DeepaMehta (if not running)

2. Open Felix Web Console:  
   <http://localhost:8080/system/console/>

   Login with "admin" / "admin".  
   You see a list of bundles.

3. Update all 'DeepaMehta 3' bundles by clicking their 'Update' button.

4. Re-open DeepaMehta (resp. click webbrowser's 'Reload' button):  
   <http://localhost:8080/de.deepamehta.3-client/index.html>


Uninstall
---------

Stop DeepaMehta and delete the 'deepamehta3' folder.  
This removes DeepaMehta completely from your system, including the database.


Version History
---------------

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

**v0.3** -- Mar 5, 2010

* Persistent topicmaps (plugin *DM3 Topicmaps*)
* Type editor (plugin *DM3 Type Editor*)
* Icon picker (plugin *DM3 Icons*)
* New topic type "To Do" (plugin *DM3 Project*)
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
Aug 4, 2010
