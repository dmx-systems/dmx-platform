<template>
  <div class="dmx-workspace-options">
    <div class="field-label">Sharing Mode</div>
    <el-radio-group v-model="sharingModeUri">
      <el-radio label="dmx.workspaces.private">Private</el-radio>
      <el-radio label="dmx.workspaces.confidential">Confidential</el-radio>
      <el-radio label="dmx.workspaces.collaborative">Collaborative</el-radio>
      <el-radio label="dmx.workspaces.public">Public</el-radio>
      <el-radio label="dmx.workspaces.common">Common</el-radio>
    </el-radio-group>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dmx-workspace-options created')
  },

  mounted () {
    // console.log('dmx-workspace-options mounted')
  },

  data () {
    return {
      sharingModeUri: 'dmx.workspaces.private'
    }
  }
}
</script>

<style>
.dmx-workspace-options .el-radio-group .el-radio {
  display: block;       /* was inline-block */
  margin-left: 0;       /* was 30px (.el-radio+.el-radio) */
  line-height: 1.8;     /* was 1 */
}
</style>
