<template>
  <el-button type="text" class="fa fa-info-circle" title="Reveal Workspace Topic" @click="revealWorkspaceTopic">
  </el-button>
</template>

<script>
export default {

  computed: {
    workspaceId () {
      return this.$store.state.workspaces.workspaceId
    }
  },

  methods: {
    revealWorkspaceTopic () {
      this.$store.dispatch('revealTopicById', this.workspaceId)
    }
  }
}
</script>
