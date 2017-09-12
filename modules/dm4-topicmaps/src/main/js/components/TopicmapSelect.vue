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

  data () {
    return {
      topicmapId: undefined
    }
  },

  watch: {
    topicmapId () {
      this.$store.dispatch('callTopicmapRoute', this.topicmapId)
    }
  },

  computed: {
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
