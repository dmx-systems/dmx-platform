<template>
  <div class="topicmap-select">
    <div class="field-label">Topicmap</div>
    <el-select v-model="topicmapId" size="small">
      <el-option v-for="topic in topics" :label="topic.value" :value="topic.id" :key="topic.id"></el-option>
    </el-select><el-button size="small" class="fa fa-info-circle" @click="revealTopicmap"></el-button>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  created () {
    this.$store.watch(
      state => state.topicmaps.topicmapId,
      id => {
        console.log('### Topicmap ID watcher', id)
        this.$store.dispatch('getTopicmap', id)
      }
    )
  },

  computed: {

    topicmapId: {
      get () {
        // Note: in the moment the Webclient components are mounted no topicmap is loaded yet
        return this.topicmap && this.topicmap.id
      },
      set (id) {
        this.$store.dispatch('selectTopicmap', id)
      }
    },

    workspaceId () {
      return this.$store.state.workspaces.workspaceId
    },

    topicmap () {
      return this.$store.state.topicmaps.topicmap
    },

    topics () {
      // Note 1: while initial rendering no workspace is selected yet
      // Note 2: when the workspace is switched its topicmap topics might not yet loaded
      const topicmapTopics = this.$store.state.topicmaps.topicmapTopics[this.workspaceId]
      return topicmapTopics && topicmapTopics.topics
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
