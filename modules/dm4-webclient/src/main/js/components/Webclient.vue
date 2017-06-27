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
import pluginManager from '../plugin-manager'

export default {

  mounted () {
    pluginManager.loadPlugins()
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
