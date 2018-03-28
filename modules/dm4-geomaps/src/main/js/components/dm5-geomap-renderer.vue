<template>
  <div class="dm5-geomap-renderer">
    <l-map :center="center" :zoom="zoom">
      <l-tile-layer :url="url"></l-tile-layer>
    </l-map>
  </div>
</template>

<script>
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

export default {

  created () {
    console.log('dm5-geomap-renderer created')
    this.$store.registerModule('geomapRenderer', require('../geomaps').default)
  },

  mounted () {
    console.log('dm5-geomap-renderer mounted')
    // TODO: allow different renderers for the same topicmap type.
    // At the moment we have a 1 to 1 relationship, so a renderer simply identifies themselves by topicmap type.
    this.$emit('renderer-mounted', 'dm4.geomaps.geomap_renderer')
  },

  destroyed () {
    console.log('dm5-geomap-renderer destroyed')
  },

  data () {
    return {
      center: L.latLng(51, 11),
      zoom: 6,
      url: 'http://{s}.tile.osm.org/{z}/{x}/{y}.png'
    }
  },

  components: {
    'l-map':        require('vue2-leaflet').LMap,
    'l-tile-layer': require('vue2-leaflet').LTileLayer
  }
}
</script>

<style>
.dm5-geomap-renderer {
  height: 100%;
}
</style>
