<template>
  <el-dropdown class="dm5-help-menu" size="medium" trigger="click">
    <el-button type="text" class="fa fa-question-circle">
      <span class="el-icon-arrow-down el-icon--right"></span>
    </el-button>
    <el-dropdown-menu slot="dropdown">
      <el-dropdown-item>Help</el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script>
export default {
}
</script>

<style>
.dm5-help-menu {
  margin-left: 1em;
}

.dm5-help-menu .el-button {
  padding-left:  0px !important;
  padding-right: 0px !important;
}
</style>
