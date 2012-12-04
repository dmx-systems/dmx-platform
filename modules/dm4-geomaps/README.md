
DM4 Geomaps
===========

A DeepaMehta 4 module for displaying geo-related topics on geographical maps.


Requirements
------------

* A DeepaMehta 4.0.11 installation  
  <https://github.com/jri/deepamehta>

* Plugin DM4 Facets 0.3  
  <https://github.com/jri/dm4-facets>


Installation
------------

1. Install the DM4 Facets plugin. See link above.

2. Download the DM4 Geomaps plugin:  
   <http://deepamehta.newthinking.net/maven2/de/deepamehta/deepamehta-geomaps/0.3/deepamehta-geomaps-0.3.jar>

3. Move the DM4 Geomaps plugin to the `deepamehta-4.0.11/bundle` folder.

4. Restart DeepaMehta.


Usage
-----

Create a geomap:

1. Choose *New Topicmap...* from the Topicmap menu. The New Topicmap dialog box appears.

2. Type in the topicmap name and choose *Geomap* from the Type menu.

3. Press *Create*. The geomap appears.

Place markers on the geomap:

* Create a Person or Institution topic, and type in its address. Once you press the *Save* button a corresponding marker is placed on the geomap.

* Reveal an existing Person or Institution by performing a search. Once you click a result item a corresponding marker is placed on the geomap.

Get information about a marker:

* Just click the marker. You see the underlying topic's detail information in the right-side panel.

Note: geomaps are working also for your self-defined domain types. Just add the *Address* topic type to your type definition. (The Address topic type is provided by the Contacts plugin which is part of the DeepaMehta standard distribution.)


Version History
---------------

**0.3** -- May 19, 2012

* Compatible with DeepaMehta 4.0.11

**0.2** -- Jan 19, 2011

* SVG overlay for feature rendering (OpenLayers vector layer replaces marker layer).
* Automatic feature relocation in case of an address change.
* Distinctive rendering of selected feature.
* The selected feature is always within viewport.
* Support for programatic feature selection.
* Service call for getting all the domain topics of a geomap.
* Compatible with DeepaMehta 4.0.7

**0.1** -- Nov 27, 2011

* Creating geomaps.
* Automatic marker placement for geo-related topics.
* Compatible with DeepaMehta 4.0.6


------------
Jörg Richter  
May 19, 2012
