<template>
  <topicmap-panel :topicmap="topicmap"></topicmap-panel>
</template>

<script>
export default {
  computed: {
    topicmap () {
      return this.$store.state.topicmaps.topicmap
    }
  },
  components: {
    'topicmap-panel': require('modules-nodejs/dm5-topicmap-panel/src/components/TopicmapPanel')
  }
}
</script>

<style>
</style>
