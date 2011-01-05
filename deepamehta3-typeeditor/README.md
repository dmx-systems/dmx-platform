
DeepaMehta 3 Type Editor
========================

This DeepaMehta 3 plugin allows interactive creation and modification of topic types. You can e.g. create a topic type "Book" along with its data fields "Title", "Author", "Abstract", and "Publication Date". Once the topic type "Book" is created you can create books and search for books.

Data fields carry data of different types. There are 5 types: *Text*, *Number*, *Date* (backed by a date picker widget), *Styled Text* (backed by a WYSIWYG editor), and *Reference*. The latter is special: a reference data field carries a reference to one or more other topics (of a certain type). The data field "Author" e.g. could carry a reference to a *Person* topic.

The DeepaMehta 3 Type Editor plugin allows modification of existing topic types as well, like e.g. the topic types *Note*, *Person*, and *Institution* as provided by the DeepaMehta 3 standard installation.

The DeepaMehta 3 Type Editor plugin works in conjunction with 2 other plugins: 1) The [DeepaMehta 3 Icon Picker](http://github.com/jri/deepamehta3-iconpicker) plugin lets you attatch an icon to a topic type, and 2) The [DeepaMehta 3 Type Search](http://github.com/jri/deepamehta3-typesearch) plugin to perform a type-based search. Both plugins are part of the DeepaMehta 3 standard installation.

DeepaMehta 3 is a platform for collaboration and knowledge management.  
<http://github.com/jri/deepamehta3>


Installing
----------

The DeepaMehta 3 Type Editor plugin is typically installed while the DeepaMehta 3 standard installation.  
<http://github.com/jri/deepamehta3>


Usage
-----

A topic type is itself a topic. It appears on the canvas and is basically handled like other topics.

*   Create a new topic type by choosing *Topic Type* from the Create menu and press the *Create* button.
    Enter a name and an unique URL for the topic type.

*   Add a data field by pressing the *Add Data Field* button. Name the data field and choose its type.
    Five types are available: *Text*, *Number*, *Date*, *Styled Text*, and *Reference*.
    Depending on its type set further options for the data field, e.g. for reference fields
    choose the refered topic type.

*   Change the order of the data fields by dragging them around.

*   Remove a data field by pressing its *Close* button (upper right corner).

*   When you're done press the *Save* button. The newly created topic type now appears in the *Create* menu
    as well as in the *By Type* search menu -- ready for creating resp. searching topics of that type.

*   Modify an existing topic type by revealing it, then press the *Edit* button.

*   Delete a topic type by revealing it, then press the *Delete* button.

Note: When you change a topic type, e.g. by renaming it, or by adding/removing/renaming data fields, the changes apply to all existing topics of that type.


Version History
---------------

**v0.4.3** -- Jan 3, 2011

* Compatible with DeepaMehta 3 v0.4.4

**v0.4.2** -- Nov 25, 2010

* Compatible with DeepaMehta 3 v0.4.3

**v0.4.1** -- Oct 16, 2010

* Compatible with DeepaMehta 3 v0.4.1

**v0.4** -- Aug 4, 2010

* New data field type: *Number*
* The order of data fields is changable by drag and drop
* Compatible with DeepaMehta 3 v0.4

**v0.3** -- Mar 6, 2010

* Basic functionality
* Compatible with DeepaMehta 3 v0.3


------------
Jörg Richter  
Jan 3, 2011
