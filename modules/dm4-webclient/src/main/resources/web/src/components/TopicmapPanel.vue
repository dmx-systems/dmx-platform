<template>
  <div id="topicmap-panel">
    <workspace-selector></workspace-selector>
    <q-select label="Topicmap" :options="options" v-model="topicmapId"></q-select>
  </div>
</template>

<script>
export default {

  data () {
    return {
      topicmapId: undefined
    }
  },

  watch: {
    topicmapId: function (topicmapId) {
      console.log('select topicmap', topicmapId)
    }
  },

  computed: {
    state () {
      return this.$store.state.topicmapPanel
    },
    options () {
      return this.state.topicmapTopics.map(topic => ({
        label: topic.value,
        value: topic.id
      }))
    }
  }
}
</script>
