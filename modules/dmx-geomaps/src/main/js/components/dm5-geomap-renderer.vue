<template>
  <l-map class="dm5-geomap-renderer" :center.sync="center" :zoom.sync="zoom" :options="options">
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

let popup

export default {

  created () {
    // console.log('dm5-geomap-renderer created')
  },

  mounted () {
    // console.log('dm5-geomap-renderer mounted')
  },

  destroyed () {
    // console.log('dm5-geomap-renderer destroyed')
  },

  props: {
    quillConfig: Object
  },

  data () {
    return {
      // map
      url: 'https://{s}.tile.osm.org/{z}/{x}/{y}.png',
      options: {
        zoomControl: false,
        zoomSnap: 0,
        attributionControl: false
      },
      // popup
      domainTopic: undefined,     // has precedence
      domainTopics: [],
      loading: undefined
    }
  },

  computed: {

    geomap () {
      const geomap = this.$store.state.geomaps.geomap
      // Note: the geomap might not be available yet as it is loaded *after* the topicmap renderer is installed
      if (!geomap) {
        // console.log('### Geomap not yet available')
        return
      }
      return geomap
    },

    center: {
      get () {
        if (this.geomap) {
          const viewProps = this.geomap.viewProps
          return [
            viewProps['dmx.geomaps.latitude'],
            viewProps['dmx.geomaps.longitude']
          ]
        }
      },
      set (center) {
        // console.log('set center', center, this.center)
        const viewProps = this.geomap.viewProps
        viewProps['dmx.geomaps.latitude']  = center.lat
        viewProps['dmx.geomaps.longitude'] = center.lng
        this.storeGeomapState()
      }
    },

    zoom: {
      get () {
        return this.geomap && this.geomap.viewProps['dmx.geomaps.zoom']
      },
      set (zoom) {
        this.geomap.viewProps['dmx.geomaps.zoom'] = zoom
        this.storeGeomapState()
      }
    },

    geoCoordTopics () {
      return this.geomap && this.geomap.geoCoordTopics
    }
  },

  methods: {

    popupOpen (geoCoordId, event) {
      // console.log('popupOpen', geoCoordId, event.popup)
      popup = event.popup
      this.domainTopic = undefined    // clear popup
      this.domainTopics = []          // clear popup
      this.loading = true
      dm5.restClient.getDomainTopics(geoCoordId).then(topics => {
        // console.log('domain topic', topic)
        switch (topics.length) {
        case 0:
          throw Error(`no domain topics for geo coord topic ${geoCoordId}`)
        case 1:
          this.showDetails(topics[0]); break
        default:
          this.domainTopics = topics
          this.loading = false
          this.updatePopup()
        }
      })
    },

    showDetails (topic) {
      this.loading = true
      dm5.restClient.getTopic(topic.id, true, true).then(topic => {
        this.domainTopic = topic
        this.loading = false
        this.updatePopup()
      })
    },

    updatePopup () {
      setTimeout(() => popup.update(), 300)
      /* does not work
      this.$nextTick()
        .then(() => {
          console.log('showDetail', popup)
          popup.update()
        })
      */
    },

    latLng (geoCoordTopic) {
      // Note: Leaflet uses lat-lon order while most other tools (including DMX) and formats use lon-lat order.
      // For exhaustive background information on this topic see https://macwright.org/lonlat/
      return [
        geoCoordTopic.children['dmx.geomaps.latitude'].value,
        geoCoordTopic.children['dmx.geomaps.longitude'].value
      ]
    },

    storeGeomapState () {
      this.$store.dispatch('_storeGeomapState', {
        center: this.center,
        zoom:   this.zoom
      })
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
/* Leaflet overrides */

.leaflet-container {
  font: unset;
}

.leaflet-popup-content {
  min-width:  200px;
  min-height:  42px;     /* see --loading-spinner-size in element-ui/packages/theme-chalk/src/common/var.scss */
}
</style>
