<template>
  <div class="topicmap-select">
    <div class="field-label">Topicmap</div>
    <el-select v-model="topicmapId" size="small">
      <el-option v-for="topic in topicmapTopics" :label="topic.value" :value="topic.id" :key="topic.id"></el-option>
    </el-select><el-button size="small" class="fa fa-info-circle" @click="revealTopicmap"></el-button>
  </div>
</template>

<script>
export default {

  computed: {

    topicmap () {
      return this.$store.state.topicmaps.topicmap
    },

    topicmapId: {
      get () {
        // Note: in the moment the Webclient components are mounted no topicmap is loaded yet
        return this.topicmap && this.topicmap.id
      },
      set (topicmapId) {
        this.$store.dispatch('callTopicmapRoute', topicmapId)
      }
    },

    topicmapTopics () {
      return this.$store.state.topicmaps.topicmapTopics
    }
  },

  methods: {
    revealTopicmap () {
      this.$store.dispatch('revealTopicById', this.topicmapId)
    }
  }
}
</script>

<style>
</style>
