<template>
  <div class="dm5-geomap-renderer">
    <l-map :center="center" :zoom="zoom" :options="options">
      <l-tile-layer :url="url"></l-tile-layer>
      <l-marker v-for="topic in geoCoordTopics" :lat-lng="latLng(topic)" :key="topic.id"
          @popupopen="popupOpen(topic.id)">
        <l-popup>
          <div><!-- popup needs at least one element, otherwise reactivity doesn't work -->
            <dm5-object-renderer v-if="object" :object="object" :quill-config="quillConfig"></dm5-object-renderer>
          </div>
        </l-popup>
      </l-marker>
    </l-map>
  </div>
</template>

<script>
import { LMap, LTileLayer, LMarker, LPopup } from 'vue2-leaflet'
import 'leaflet/dist/leaflet.css'
import dm5 from 'dm5'

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

  props: {
    quillConfig: Object
  },

  data () {
    return {
      // popup details
      object: undefined,
      // map options
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
      // Note: the geomap renderer might be unavailable while renderer switching (see topicmap-panel.js)
      const renderer = this.$store.state['dmx.geomaps.geomap_renderer']
      if (!renderer) {
        console.log('Geomap renderer not available')
        return
      }
      // Note: the geomap might not be available yet as it is loaded *after* the topicmap renderer is installed
      const geomap = renderer.geomap
      if (!geomap) {
        console.log('Geomap not available')
        return
      }
      return geomap.geoCoordTopics
    }
  },

  methods: {

    popupOpen (geoCoordId) {
      // console.log('popupOpen', geoCoordId)
      dm5.restClient.getDomainTopic(geoCoordId, true).then(topic => {
        // console.log('domain topic', topic)
        this.object = topic
      })
    },

    latLng (geoCoordTopic) {
      return [
        geoCoordTopic.childs['dmx.geomaps.latitude'].value,
        geoCoordTopic.childs['dmx.geomaps.longitude'].value
      ]
    }
  },

  components: {
    LMap, LTileLayer, LMarker, LPopup,
    'dm5-object-renderer': require('dm5-object-renderer').default
  }
}
</script>

<style>
.dm5-geomap-renderer {
  height: 100%;
}

/* Leaflet overrides */

.leaflet-container {
  font: unset;
}

.leaflet-popup-content {
  min-width: 200px;
}
</style>
