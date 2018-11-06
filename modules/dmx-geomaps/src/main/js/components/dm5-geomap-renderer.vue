<template>
  <div class="dm5-geomap-renderer">
    <l-map :center="center" :zoom="zoom" :options="options">
      <l-tile-layer :url="url"></l-tile-layer>
      <l-marker v-for="topic in geoCoordTopics" :lat-lng="latLng(topic)" :key="topic.id"
          @popupopen="popupOpen(topic.id, $event)">
        <l-popup v-loading="loading">
          <dm5-object-renderer v-if="domainTopic" :object="domainTopic" :quill-config="quillConfig">
          </dm5-object-renderer>
          <dm5-topic-list v-else :topics="domainTopics" no-sort-menu @topic-click="showDetails"></dm5-topic-list>
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
      // map
      center: [51, 5],
      zoom: 6,
      url: 'http://{s}.tile.osm.org/{z}/{x}/{y}.png',
      options: {
        zoomControl: false,
        attributionControl: false
      },
      // popup
      domainTopic: undefined,     // has precedence
      domainTopics: [],
      loading: undefined
    }
  },

  computed: {
    geoCoordTopics () {
      // Note: the geomap renderer might be unavailable while renderer switching (see topicmap-panel.js)
      const renderer = this.$store.state['dmx.geomaps.geomap_renderer']
      if (!renderer) {
        // console.log('Geomap renderer not available')
        return
      }
      // Note: the geomap might not be available yet as it is loaded *after* the topicmap renderer is installed
      const geomap = renderer.geomap
      if (!geomap) {
        // console.log('Geomap not available')
        return
      }
      return geomap.geoCoordTopics
    }
  },

  methods: {

    popupOpen (geoCoordId, event) {
      console.log('popupOpen', geoCoordId, event.popup)
      this.domainTopic = undefined
      this.domainTopics = []
      this.loading = true
      dm5.restClient.getDomainTopics(geoCoordId).then(topics => {
        // console.log('domain topic', topic)
        switch (topics.length) {
        case 0:
          throw Error(`No domain topics for geo coord topic ${geoCoordId}`)
        case 1:
          this.showDetails(topics[0]); break
        default:
          this.domainTopics = topics
          this.loading = false
        }
      })
    },

    showDetails (topic) {
      this.loading = true
      dm5.restClient.getTopic(topic.id, true, true).then(topic => {
        this.domainTopic = topic
        this.loading = false
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
    'dm5-object-renderer': require('dm5-object-renderer').default,
    'dm5-topic-list':      require('dm5-topic-list').default
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
  min-width:  200px;
  min-height:  42px;     /* see --loading-spinner-size in element-ui/packages/theme-chalk/src/common/var.scss */
}
</style>
