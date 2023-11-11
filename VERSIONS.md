
Version History
===============

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
