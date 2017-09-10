<template>
  <div class="webclient">
    <component v-for="comp in components" :is="comp" :key="comp._dm5_id">
      <toolbar slot="dm5-topicmap-panel"></toolbar>
    </component>
    <dm5-detail-panel :object="object" :mode="mode"></dm5-detail-panel>
    <dm5-search-widget :menu-topic-types="menuTopicTypes"></dm5-search-widget>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  mounted () {
    // Before the initial topicmap can be rendered 2 promises must be fullfilled:
    // 1) the dm5 library is ready (type cache is populated)
    // 2) the topicmap renderer is ready (SVG data is loaded)
    Promise.all([
      dm5.getPromise(),
      // Note: the "initTopicmapRenderer" action is registered by the CytoscapeRenderer's mounted() hook (see comment
      // there). So we must do the dispatching here in the mounted() hook too. (created() would be too early.)
      // CytoscapeRenderer is a child component of Webclient, so the CytoscapeRenderer component is guaranteed to be
      // mounted *before* Webclient.
      this.$store.dispatch('initTopicmapRenderer')
    ]).then(this.initWebclientState)
  },

  computed: {

    object () {
      return this.$store.state.selectedObject
    },

    mode () {
      return this.$store.state.detailPanel.mode
    },

    components () {
      return this.$store.state.componentRegistry.components['dm5.webclient']
    },

    menuTopicTypes () {
      return this.$store.getters.menuTopicTypes
    }
  },

  watch: {
    $route (to, from) {
      const topicmapId = to.params.topicmapId
      const oldTopicmapId = from.params.topicmapId
      console.log('$route watcher topicmapId', topicmapId, oldTopicmapId, topicmapId != oldTopicmapId)
      // Note: path param values read from URL are strings. Path param values set by push() are numbers.
      // So we do *not* use exact equality (!==) here.
      if (topicmapId != oldTopicmapId) {
        this.$store.dispatch('renderTopicmap', topicmapId)
      }
      //
      var selected
      //
      const topicId = to.params.topicId
      const oldTopicId = from.params.topicId
      console.log('$route watcher topicId', topicId, oldTopicId, topicId != oldTopicId)
      if (topicId != oldTopicId) {
        if (topicId) {  // FIXME: 0 is a valid topic ID
          this.$store.dispatch('fetchTopicAndDisplayInDetailPanel', topicId)
          selected = true
        }
      }
      //
      const assocId = to.params.assocId
      const oldAssocId = from.params.assocId
      console.log('$route watcher assocId', assocId, oldAssocId, assocId != oldAssocId)
      if (assocId != oldAssocId) {
        if (assocId) {
          this.$store.dispatch('fetchAssocAndDisplayInDetailPanel', assocId)
          selected = true
        }
      }
      //
      if (!selected) {
        this.$store.dispatch('_unselect')
      }
    }
  },

  methods: {
    initWebclientState () {
      const topicmapId = this.$route.params.topicmapId
      const topicId    = this.$route.params.topicId
      const assocId    = this.$route.params.assocId
      console.log('Initial route (topicmapId, topicId, assocId)', topicmapId, topicId, assocId)
      if (topicmapId) {
        this.$store.dispatch('renderTopicmap', topicmapId)
      }
      if (topicId) {  // FIXME: 0 is a valid topic ID
        this.$store.dispatch('fetchTopicAndDisplayInDetailPanel', topicId)
      }
      if (assocId) {
        this.$store.dispatch('fetchAssocAndDisplayInDetailPanel', assocId)
      }
    }
  },

  components: {
    'toolbar':           require('./Toolbar'),
    'dm5-detail-panel':  require('dm5-detail-panel'),
    'dm5-search-widget': require('dm5-search-widget')
  }
}
</script>

<style>
.webclient {
  height: 100%;
  display: flex;
}

.webclient .topicmap-panel {
  flex-basis: 70%;
  position: relative;
}

.webclient .dm5-detail-panel {
  flex-basis: 30%;
  overflow: auto;
  box-sizing: border-box;
  padding: 0 12px 12px 12px;
  background-color: var(--background-color);
}
</style>
