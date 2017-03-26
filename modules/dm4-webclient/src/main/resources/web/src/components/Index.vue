<template>
  <q-layout>
    <topicmap-panel class="width-3of4"></topicmap-panel>
    <detail-panel class="width-1of4"></detail-panel>
  </q-layout>
</template>

<script>
export default {
  components: {
    'topicmap-panel': require('./TopicmapPanel.vue'),
    'detail-panel': require('./DetailPanel.vue')
  }
}
</script>
