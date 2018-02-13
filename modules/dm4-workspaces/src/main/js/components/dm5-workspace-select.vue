<template>
  <div class="dm5-workspace-select">
    <div class="field-label">Workspace</div>
    <el-select v-model="workspaceId">
      <el-option v-for="topic in workspaceTopics" :label="topic.value" :value="topic.id" :key="topic.id"></el-option>
    </el-select>
    <el-button type="text" icon="el-icon-info" @click="revealWorkspace"></el-button>
  </div>
</template>

<script>
export default {

  computed: {

    workspaceId: {
      get () {
        return this.$store.state.workspaces.workspaceId
      },
      set (id) {
        this.$store.dispatch('selectWorkspace', id)
      }
    },

    workspaceTopics () {
      return this.$store.state.workspaces.workspaceTopics
    }
  },

  methods: {
    revealWorkspace () {
      this.$store.dispatch('revealTopicById', this.workspaceId)
    }
  }
}
</script>

<style>
.dm5-workspace-select .el-button {
  padding-left: 2px;
}
</style>
