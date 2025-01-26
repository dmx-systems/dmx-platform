<template>
  <el-dropdown class="dmx-help-menu" trigger="click" @command="handle">
    <el-button type="primary" link class="fa fa-question-circle">
      <el-icon class="el-icon--right"><arrow-down></arrow-down></el-icon>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="item in items" :command="item.action" :divided="item.divided" :key="item.label">
          <el-link v-if="item.href" :href="item.href" target="_blank" :underline="false">{{item.label}}</el-link>
          <template v-else>{{item.label}}</template>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
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
