<template>
  <div class="dm5-workspace-options">
    <el-radio-group v-model="sharingModeUri">
      <el-radio label="dm4.workspaces.private">Private</el-radio>
      <el-radio label="dm4.workspaces.confidential">Confidential</el-radio>
      <el-radio label="dm4.workspaces.collaborative">Collaborative</el-radio>
      <el-radio label="dm4.workspaces.public">Public</el-radio>
      <el-radio label="dm4.workspaces.common">Common</el-radio>
    </el-radio-group>
  </div>
</template>

<script>
export default {

  created () {
    console.log('dm5-workspace-options created')
  },

  mounted () {
    console.log('dm5-workspace-options mounted')
  },

  data () {
    return {
      sharingModeUri: 'dm4.workspaces.private'
    }
  }
}
</script>

<style>
.dm5-workspace-options .el-radio-group .el-radio {
  display: block;       /* was inline-block */
  margin-left: 0;       /* was 30px (.el-radio+.el-radio) */
  line-height: 2;       /* was 1 */
}
</style>
