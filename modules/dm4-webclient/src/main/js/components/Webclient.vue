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

  created () {
    dm5.init(this.$store, this.initWebclientState)
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
    }
  },

  methods: {
    initWebclientState () {
      const topicmapId = this.$route.params.topicmapId
      console.log('Initial route topicmapId', topicmapId)
      if (topicmapId) {
        this.$store.dispatch('renderTopicmap', topicmapId)
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
