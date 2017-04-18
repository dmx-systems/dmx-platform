<template>
  <div>
    Topicmap
    <select>
      <option v-for="topic in topicmapTopics" :value="topic.id">{{topic.value}}</option>
    </select>
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
    topicmapTopics () {
      return this.$store.state.topicmaps.topicmapTopics
    }
  }
}
</script>
