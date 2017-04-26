<template>
  <div class="webclient">
    <toolbar></toolbar>
    <div class="content">
      <component v-for="comp in components" :is="comp" :key="comp._dm5_id"></component>
      <detail-panel :object="object"></detail-panel>
    </div>
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
    components () {
      return this.$store.state.componentRegistry.components['dm5.webclient.content']
    }
  },

  components: {
    'toolbar':      require('./Toolbar'),
    'detail-panel': require('modules-nodejs/dm5-detail-panel/src/components/DetailPanel')
  }
}
</script>

<style>
.webclient {
  height: 100%;
  display: flex;
  flex-flow: column;
}

.webclient .content {
  display: flex;
  flex: auto;
}

.webclient .content .topicmap-panel {
  flex-basis: 70%;
}

.webclient .content .detail-panel {
  flex-basis: 30%;
}
</style>
