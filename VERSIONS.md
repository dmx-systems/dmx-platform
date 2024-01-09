
Version History
===============


5.3.4
-----

*unreleased*

#### Plugin development:

- CoreService API: add `indexTopicFulltext()` (#529)
    - DMXStorage SPI: add `indexTopicFulltext()` (#529)
- Deleting a particular value from a muti-value is now supported for duplicate child values. (#528) Thanks to @gevlish!  
  Core API (BREAKING CHANGE): `ChildTopicsModel`'s `addDeletionRef()` takes `assocId` argument.
- Core: assoc type must not be set to update assoc value (#530) Thanks to @gevlish!
- Fix: RelatedTopicModel JSON serialization includes "assoc" (#530)
- ModelFactory API: add newAssocModel(SimpleValue) (#531) Thanks to @gevlish!
- ChildTopicsModel API: add add/setRef() w/ AssocModel param (#531) Thanks to @gevlish!
- Upgrade to SLF4J 2.0, adding Aries SPI Fly and ASM dependencies (#533)
- Improve resource handling: `resources-static/` obsolete (#535, #504)


5.3.3
-----

Nov 11, 2023

#### Improvements:

- Security: password hashes are stored with salt (#518) Thanks to @almereyda!
    - Optional additional site-wide salting, new config property `dmx.security.site_salt`
    - Legacy user accounts are converted automatically as soon as user logs in
- Security: include session IDs in server log only at FINE level (#514) Thanks to @junes!
- Configuration: in config.properties explain `dmx.host.url` in more detail (#517)

#### Bug fixes:

- Several URL bugs fixed related to logout/reload (#524)
    - Strip selection from URL if selected topic/association not readable anymore after logout
    - Invalid routes (stale topicmap ID and/or topic ID) are redirected to valid route
- Several password related bugs fixed:
    - Several users can have the same password (#85)
    - Saving an user account w/o any changes works as expected (#520)
    - User can change her password also if she lacks WRITE permission for the current workspace (#519)
    - Change-username requests are rejected. DMX usernames are fixed. (#521)
    - More clear error message when user tries to empty her password (#523)
- Webclient search results don't break in-mid word (#522) Thanks to @gevlish!
- Line wrap Webclient error notifications (#525)

#### Plugin development:

- Core API (BREAKING CHANGE): `Credentials` makes plain text password available (#515) Thanks to @thebohemian!
- Core API: add `getConfigDir()` to `DMXUtils` (#527) Thanks to @thebohemian!
- Core's ValueIntegrator supports custom workspace assignments for created topics/associations (#519)
- Webservice: boolean return values of RESTful methods are JSON serialized automatically (#516) Thanks to @thebohemian!


5.3.2
-----

Aug 24, 2023

#### Improvements:

- When an user is deleted the ownership of her workspaces is transfered to the current user

#### Plugin development:

- AccessControl API: add `getWorkspacesByOwner()`


5.3.1
-----

Jun 23, 2023

#### Improvements:

- The Webclient recovers after a network or server problem, e.g. when internet goes away or server is restarted.
  An alert is shown and after pressing OK the Webclient is relaunched. Obtained by updating to `dmx-api` 3.0.1
