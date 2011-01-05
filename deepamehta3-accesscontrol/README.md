
DeepaMehta 3 Access Control
===========================

This plugin adds access control to DeepaMehta 3: configurable access privilegs by the means of users, groups, roles, and access control lists (ACLs). Access control might be an issue with shared DeepaMehta installations.

DeepaMehta 3 is a platform for collaboration and knowledge management.  
<http://github.com/jri/deepamehta3>


Installing
----------

The DeepaMehta 3 Access Control plugin is typically installed while the DeepaMehta 3 standard installation.  
See link above.

**WARNING:** This plugin is in early state and not yet ready for production use. That's why it is installed but *deactivated* by default. Do *not* activate the plugin on a DeepaMehta 3 installation that holds your real content. Although you would not loose any data your installation might not properly upgrade to later versions of the DeepaMehta 3 Access Control plugin.

Activate the DeepaMehta 3 Access Control plugin via the Apache Felix shell (the terminal window that opens while starting DeepaMehta):

1. Find out the bundle ID of the DeepaMehta 3 Access Control plugin by using the `lb` command (list bundles):

        lb

   You will find the DeepaMehta 3 Access Control plugin and its bundle ID (it is supposed to be 39 or something like that) in the displayed list of bundles:

        ID|State      |Level|Name
        ..|..         |..   |..
        39|Resolved   |    1|DeepaMehta 3 Access Control (0.4.1)

   The *Resolved* state means the plugin is not yet activated.

2. Activate the DeepaMehta 3 Access Control plugin by using the `start` command:

        start 39

   While the DeepaMehta 3 Access Control plugin starts you'll see some information logged.

   Now, when using the `lb` command again you see the DeepaMehta 3 Access Control plugin in *Active* state:

        39|Active     |    1|DeepaMehta 3 Access Control (0.4.1)

3. Stop DeepaMehta and start it again (as described in its [README](http://github.com/jri/deepamehta3)).  
   Sorry for this extra step!

4. You're done. The DeepaMehta browser window opens automatically.


Usage
-----

* Login via the *Special* menu.  
  Initially one user exists: *admin* without password.

* Simple access privilegs are pre-configured:
  * To create or edit any content you must login.
  * A guest user can search/view everything but can't create or edit anything.
  * An user can edit only the topics she has created.

  For the moment the access privilegs are not configurable by the user.

* Create a new user by choosing *User* from the type menu and pressing the *Create* button.  

* Modify an user (e.g. change its password) by revealing the user topic and pressing the *Edit* button.

* Logout via the *Special* menu.


Version History
---------------

**v0.4.1** -- Jan 3, 2011

* New role: MEMBER.
* Extended service API.
* Compatible with DeepaMehta 3 v0.4.4

**v0.4** -- Nov 25, 2010

* 3 roles: CREATOR, OWNER, EVERYONE.
* 2 permissions: WRITE, CREATE.
* Compatible with DeepaMehta 3 v0.4.3


------------
Jörg Richter  
Jan 3, 2011
