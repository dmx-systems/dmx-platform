<template>
  <div class="workspace-select">
    <div class="field-label">Workspace</div>
    <el-select v-model="workspaceId" size="small">
      <el-option v-for="topic in workspaceTopics" :label="topic.value" :value="topic.id" :key="topic.id"></el-option>
    </el-select><el-button size="small" class="fa fa-info-circle" @click="revealWorkspace"></el-button>
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
    workspaceId: function () {
      this.$store.dispatch('selectWorkspace', this.workspaceId)
    }
  },

  computed: {
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
</style>
