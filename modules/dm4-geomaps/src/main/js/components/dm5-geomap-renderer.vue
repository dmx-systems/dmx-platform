<template>
  <div class="dm5-geomap-renderer">
    <l-map :center="center" :zoom="zoom" :options="options">
      <l-tile-layer :url="url"></l-tile-layer>
    </l-map>
  </div>
</template>

<script>
import 'leaflet/dist/leaflet.css'

var {LMap, LTileLayer} = require('vue2-leaflet')

export default {

  created () {
    console.log('dm5-geomap-renderer created')
  },

  mounted () {
    console.log('dm5-geomap-renderer mounted')
  },

  destroyed () {
    console.log('dm5-geomap-renderer destroyed')
  },

  data () {
    return {
      center: [51, 11],
      zoom: 6,
      url: 'http://{s}.tile.osm.org/{z}/{x}/{y}.png',
      options: {
        zoomControl: false,
        attributionControl: false
      }
    }
  },

  components: {
    LMap, LTileLayer
  }
}
</script>

<style>
.dm5-geomap-renderer {
  height: 100%;
}
</style>
