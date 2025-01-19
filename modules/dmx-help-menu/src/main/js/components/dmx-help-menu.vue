<template>
  <el-dropdown class="dmx-help-menu" size="medium" trigger="click" @command="handle">
    <el-button type="primary" link class="fa fa-question-circle">
      <span class="el-icon-arrow-down el-icon--right"></span>
    </el-button>
    <el-dropdown-menu slot="dropdown">
      <el-dropdown-item v-for="item in items" :command="item.action" :divided="item.divided" :key="item.label">
        <el-link v-if="item.href" :href="item.href" target="_blank" :underline="false">{{item.label}}</el-link>
        <template v-else>{{item.label}}</template>
      </el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script>
export default {

  computed: {
    items () {
      return this.$store.state.helpmenu.items
    }
  },

  methods: {
    handle (command) {
      command && this.$store.dispatch(command)
    }
  }
}
</script>

<style>
.dmx-help-menu .el-button {
  padding-left:  0px !important;
  padding-right: 0px !important;
}
</style>
