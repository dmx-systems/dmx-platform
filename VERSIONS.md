
Version History
===============


5.3.4
-----

Feb 22, 2024

#### New plugins available

- [dmx-pdf-search](https://github.com/dmx-systems/dmx-pdf-search) - fulltext search for PDF files,
  works transparently in DMX Webclient's search dialog
- [dmx-tesseract](https://github.com/dmx-systems/dmx-tesseract) - integrates the
  [Tesseract](https://github.com/tesseract-ocr/tesseract) OCR engine, works together with `dmx-pdf-search` plugin

#### Bug fixes

- Render color emojis in all browsers and platforms (#536)
    - Related: on all platforms the DMX Webclient uses "Ubuntu" as main font (#536)

#### Plugin development

- Core support for custom fulltext indexing, e.g. for making PDF content searchable (#529)
    - `CoreService` API: add `indexTopicFulltext()`
    - `DMXStorage` SPI: add `indexTopicFulltext()`
- Extended Core API support for DB updates (Thanks to @gevlish!)
    - Deleting a particular value from a multi-value is now supported for duplicate values (#528)  
      BREAKING CHANGE: `ChildTopicsModel`'s `addDeletionRef()` takes an `assocId` argument (instead of `topicId`).
    - `ModelFactory` API: add `newAssocModel()` with `SimpleValue` parameter (#531)
    - `ChildTopicsModel` API: add `addRef()`/`setRef()` with `AssocModel` parameter (#531)
    - In an update-assoc-value call the assoc type is not required to be set (#530)
- Improved handling of web resources (Thanks to @thebohemian!)
    - Webpack output goes to `target/` (instead of `src/`), `resources-static/` is obsolete,
      `clean-webpack-plugin` is not needed anymore (#535, #504)
- Core fix: `RelatedTopicModel` JSON serialization includes `assoc`
- Platform's SLF4J is upgraded to 2.0, includes Aries SPI Fly and ASM dependencies (#533)


5.3.3
-----

Nov 11, 2023

#### Improvements

- Security: password hashes are stored with salt (#518) Thanks to @almereyda!
    - Optional additional site-wide salting, new config property `dmx.security.site_salt`
    - Legacy user accounts are converted automatically as soon as user logs in
- Security: include session IDs in server log only at FINE level (#514) Thanks to @junes!
- Configuration: in config.properties explain `dmx.host.url` in more detail (#517)

#### Bug fixes

- Several URL bugs fixed related to logout/reload (#524)
    - Strip selection from URL if selected topic/association not readable anymore after logout
    - Invalid routes (stale topicmap ID and/or topic ID) are redirected to valid route
- Several password related bug fixes
    - Several users can have the same password (#85)
    - Saving an user account w/o any changes works as expected (#520)
    - User can change her password also if she lacks WRITE permission for the current workspace (#519)
    - Change-username requests are rejected. DMX usernames are fixed. (#521)
    - More clear error message when user tries to empty her password (#523)
- Webclient search results don't break in-mid word (#522) Thanks to @gevlish!
- Line wrap Webclient error notifications (#525)

#### Plugin development

- Core API (BREAKING CHANGE): `Credentials` makes plain text password available (#515) Thanks to @thebohemian!
- Core API: add `getConfigDir()` to `DMXUtils` (#527) Thanks to @thebohemian!
- Core's ValueIntegrator supports custom workspace assignments for created topics/associations (#519)
- Webservice: boolean return values of RESTful methods are JSON serialized automatically (#516) Thanks to @thebohemian!


5.3.2
-----

Aug 24, 2023

#### Improvements

- When an user is deleted the ownership of her workspaces is transferred to the current user

#### Plugin development

- AccessControl API: add `getWorkspacesByOwner()`


5.3.1
-----

Jun 23, 2023

#### Improvements

- The Webclient recovers after a network or server problem, e.g. when internet goes away or server is restarted.
  An alert is shown and after pressing OK the Webclient is relaunched. Obtained by updating to `dmx-api` 3.0.1
