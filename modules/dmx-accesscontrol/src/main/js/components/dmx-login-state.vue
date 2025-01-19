<template>
  <div class="dmx-login-state">
    <el-dropdown v-if="username" size="medium" trigger="click" @command="handle">
      <el-button type="primary" link class="fa fa-user-circle">
        <span class="el-icon-arrow-down el-icon--right"></span>
      </el-button>
      <el-dropdown-menu slot="dropdown">
        <el-dropdown-item command="revealUsername" title="Reveal Username topic">
          <b>{{username}}</b>
        </el-dropdown-item>
        <el-dropdown-item command="logout" divided>
          Logout
        </el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
    <el-button v-else class="sign-in" type="primary" @click="login">Sign in</el-button>
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
  margin-left: 12px;
}

.dmx-login-state .el-dropdown .el-button {
  padding-left:  0px !important;
  padding-right: 0px !important;
}

.dmx-login-state .el-button.sign-in {
  font-size: var(--label-font-size) !important;
  padding: 4px 8px !important;
}
</style>
