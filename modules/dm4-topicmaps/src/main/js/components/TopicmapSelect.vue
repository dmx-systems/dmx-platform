<template>
  <div class="topicmap-select">
    <div class="field-label">Topicmap</div>
    <el-select v-model="topicmapId" size="small">
      <el-option v-for="topic in topicmapTopics" :label="topic.value" :value="topic.id" :key="topic.id"></el-option>
    </el-select>
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
      this.$store.dispatch('renderTopicmap', topicmapId)
    }
  },

  computed: {
    topicmapTopics () {
      return this.$store.state.topicmaps.topicmapTopics
    }
  }
}
</script>

<style>
</style>
