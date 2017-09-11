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
    ]).then(() => {
      this.$store.dispatch('initialNavigation')
    })
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
