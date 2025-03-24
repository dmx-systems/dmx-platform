<template>
  <div class="dmx-login-state">
    <el-dropdown v-if="username" trigger="click" @command="handle">
      <el-button type="primary" link class="fa fa-user-circle">
        <el-icon class="el-icon--right"><arrow-down></arrow-down></el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="revealUsername" title="Reveal Username topic">
            <b>{{username}}</b>
          </el-dropdown-item>
          <el-dropdown-item command="logout" divided>
            Logout
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
    <el-button v-else class="sign-in-button" type="primary" @click="login">Sign in</el-button>
  </div>
</template>

<script>
export default {

  computed: {
    username () {
      return this.$store.state.accesscontrol.username
    }
  },

  methods: {

    handle (command) {
      command && this.$store.dispatch(command)
    },

    login () {
      this.$store.dispatch('openLoginDialog')
    }
  }
}
</script>

<style>
.dmx-login-state {
  margin-left: 8px;
}

.dmx-login-state .sign-in-button {
  font-size: var(--label-font-size) !important;
  height: 28px;           /* Element Plus default is 32px */
  padding-left: 8px;      /* Element Plus default is 15px */
  padding-right: 8px;     /* Element Plus default is 15px */
}
</style>
