<template>
  <el-dropdown class="dmx-help-menu" trigger="click" @command="handle">
    <el-button type="primary" link class="fa fa-question-circle">
      <el-icon class="el-icon--right"><arrow-down></arrow-down></el-icon>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="item in items" :command="item" :divided="item.divided" :key="item.label">
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
    // Note: for some items "action" is undefined. For such an item if the template would pass item.action, the value
    // received here would not be undefined but an empty object. This is something about JS Proxy objects and/or Vue3
    // (templates), I don't know at the moment. The solution is to let the template pass the entire item and access
    // "action" only here in the method. Then it is undefined (as naively expected).
    handle (item) {
      item.action && this.$store.dispatch(item.action)
    }
  }
}
</script>
