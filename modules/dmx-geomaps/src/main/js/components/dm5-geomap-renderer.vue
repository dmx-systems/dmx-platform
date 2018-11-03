<template>
  <div class="dm5-geomap-renderer">
    <l-map :center="center" :zoom="zoom" :options="options">
      <l-tile-layer :url="url"></l-tile-layer>
      <l-marker v-for="topic in geoCoordTopics" :lat-lng="latLng(topic)" :key="topic.id"></l-marker>
    </l-map>
  </div>
</template>

<script>
import { LMap, LTileLayer, LMarker } from 'vue2-leaflet'
import 'leaflet/dist/leaflet.css'

// stupid hack so that leaflet's images work after going through webpack
// https://github.com/PaulLeCam/react-leaflet/issues/255
delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
    iconUrl:       require('leaflet/dist/images/marker-icon.png'),
    iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
    shadowUrl:     require('leaflet/dist/images/marker-shadow.png')
})

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
      center: [51, 5],
      zoom: 6,
      url: 'http://{s}.tile.osm.org/{z}/{x}/{y}.png',
      options: {
        zoomControl: false,
        attributionControl: false
      }
    }
  },

  computed: {
    geoCoordTopics () {
      // Note: the geomap is loaded *after* the topicmap renderer is installed (see topicmap-panel.js)
      const geomap = this.$store.state['dmx.geomaps.geomap_renderer'].geomap
      return geomap && geomap.geoCoordTopics
    }
  },

  methods: {
    latLng (geoCoordTopic) {
      return L.latLng(
        geoCoordTopic.childs['dmx.geomaps.latitude'].value,
        geoCoordTopic.childs['dmx.geomaps.longitude'].value
      )
    }
  },

  components: {
    LMap, LTileLayer, LMarker
  }
}
</script>

<style>
.dm5-geomap-renderer {
  height: 100%;
}
</style>
