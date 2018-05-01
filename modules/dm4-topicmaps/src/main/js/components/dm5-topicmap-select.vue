<template>
  <div class="dm5-topicmap-select">
    <div class="field-label">Topicmap</div>
    <el-select v-model="topicmapId">
      <el-option v-for="topic in topics" :label="topic.value" :value="topic.id" :key="topic.id"></el-option>
    </el-select>
    <el-button type="text" icon="el-icon-info" @click="revealTopicmap"></el-button>
  </div>
</template>

<script>
export default {

  computed: {

    topicmapId: {
      get () {
        return this.$store.getters.topicmapId
      },
      set (id) {
        this.$store.dispatch('selectTopicmap', id)
      }
    },

    workspaceId () {
      return this.$store.state.workspaces.workspaceId
    },

    topics () {
      // Note 1: while initial rendering no workspace is selected yet
      // Note 2: when the workspace is switched its topicmap topics might not yet loaded
      return this.$store.state.topicmaps.topicmapTopics[this.workspaceId]
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
.dm5-topicmap-select {
  margin-left: 6px;
}

.dm5-topicmap-select .el-button {
  padding-left: 2px !important;
}
</style>
