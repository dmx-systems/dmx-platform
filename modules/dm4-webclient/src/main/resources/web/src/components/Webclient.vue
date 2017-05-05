<template>
  <div class="webclient">
    <component v-for="comp in components" :is="comp" :key="comp._dm5_id">
      <toolbar slot="dm5-topicmap-panel"></toolbar>
    </component>
    <detail-panel :object="object" :mode="mode"></detail-panel>
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
      return this.$store.state.detailPanelMode
    },

    components () {
      return this.$store.state.componentRegistry.components['dm5.webclient']
    }
  },

  components: {
    'toolbar':      require('./Toolbar'),
    'detail-panel': require('dm5-detail-panel')
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
}

.webclient .detail-panel {
  flex-basis: 30%;
  background-color: #f4f4f4;
}
</style>
