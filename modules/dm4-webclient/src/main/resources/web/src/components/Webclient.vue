<template>
  <div id="webclient">
    <toolbar></toolbar>
    <div id="content">
      <topicmap-panel></topicmap-panel>
      <detail-panel></detail-panel>
    </div>
  </div>
</template>

<script>
import pluginManager from '../plugin-manager'
import TopicmapPanel from 'dm5-topicmap-panel'
import DetailPanel   from 'dm5-detail-panel'

export default {
  mounted () {
    pluginManager.loadPlugins()
  },
  components: {
    'toolbar':        require('./Toolbar.vue'),
    'topicmap-panel': TopicmapPanel.component,
    'detail-panel':   DetailPanel.component
  }
}
</script>

<style>
#webclient {
  height: 100%;
  display: flex;
  flex-flow: column;
}

#webclient #content {
  display: flex;
  flex: auto;
}
</style>
