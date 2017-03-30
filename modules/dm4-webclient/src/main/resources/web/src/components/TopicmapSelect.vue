<template>
  <q-select label="Topicmap" :options="options" v-model="topicmapId"></q-select>
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

    topicmapTopics () {
      console.log('Computing "topicmapTopics"')
      return this.$store.state.topicmapPanel.topicmapTopics
    },

    options () {
      return this.topicmapTopics.map(topic => ({
        label: topic.value,
        value: topic.id
      }))
    }
  }
}
</script>
