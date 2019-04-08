<template>
  <div class="dm5-topicmap-select">
    <el-select v-model="topicmapId">
      <el-option-group label="Topicmap">
        <el-option v-for="topic in topics" :label="topic.value" :value="topic.id" :key="topic.id"></el-option>
      </el-option-group>
    </el-select>
    <el-button type="text" icon="el-icon-info" @click="revealTopicmapTopic"></el-button>
    <el-button type="text" icon="el-icon-rank" @click="fitTopicmapViewport"></el-button>
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

    revealTopicmapTopic () {
      this.$store.dispatch('revealTopicById', this.topicmapId)
    },

    fitTopicmapViewport () {
      this.$store.dispatch('fitTopicmapViewport', this.topicmapId)
    }
  }
}
</script>

<style>
.dm5-topicmap-select {
  margin-left: 18px;
}

.dm5-topicmap-select .el-button {
  padding-left:  2px !important;
  padding-right: 2px !important;
  margin-left: 0;
}
</style>
