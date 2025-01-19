<template>
  <el-button type="primary" link class="fa fa-info-circle" title="Reveal Topicmap Topic" @click="revealTopicmapTopic">
  </el-button>
</template>

<script>
export default {

  computed: {
    topicmapId () {
      return this.$store.getters.topicmapId
    }
  },

  methods: {
    revealTopicmapTopic () {
      this.$store.dispatch('revealTopicById', this.topicmapId)
    }
  }
}
</script>
