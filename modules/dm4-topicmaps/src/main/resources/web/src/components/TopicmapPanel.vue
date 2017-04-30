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
    'topicmap-panel': require('dm5-topicmap-panel')
  }
}
</script>

<style>
</style>
