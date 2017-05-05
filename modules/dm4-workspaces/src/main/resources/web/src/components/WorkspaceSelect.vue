<template>
  <div class="workspace-select">
    <div class="label">Workspace</div>
    <el-select v-model="workspaceId" size="small">
      <el-option v-for="topic in workspaceTopics" :label="topic.value" :value="topic.id" :key="topic.id"></el-option>
    </el-select>
  </div>
</template>

<script>
export default {

  data () {
    return {
      workspaceId: undefined
    }
  },

  watch: {
    workspaceId: function (workspaceId) {
      console.log('select workspace', workspaceId)
    }
  },

  computed: {
    workspaceTopics () {
      return this.$store.state.workspaces.workspaceTopics
    }
  }
}
</script>

<style>
.workspace-select .label {
  font-size: 14px;    /* should match .el-dialog__body style */
  color: #48576a;     /* should match .el-dialog__body style */
}
</style>