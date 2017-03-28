<template>
  <q-layout>
    <toolbar slot="header"></toolbar>
    <topicmap-panel class="width-3of4"></topicmap-panel>
    <detail-panel class="width-1of4"></detail-panel>
  </q-layout>
</template>

<script>
export default {

  mounted () {
    console.log('DOM ready!')
    initPlugins()
  },

  components: {
    'toolbar': require('./Toolbar.vue'),
    'topicmap-panel': require('./TopicmapPanel.vue'),
    'detail-panel': require('./DetailPanel.vue')
  }
}

function initPlugins () {
  // ### TODO: load plugins dynamically
  require('../../../../../../../dm4-workspaces/src/main/resources/web/src/main.js').default.init()
}
</script>
