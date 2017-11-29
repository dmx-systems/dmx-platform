<template>
  <div class="login-state">
    <div v-if="username" class="info">
      Logged in as <span class="username">{{username}}</span>
    </div>
    <el-button type="text" size="mini" @click="dispatch">{{buttonLabel}}</el-button>
  </div>
</template>

<script>
export default {

  computed: {

    username () {
      return this.$store.state.accesscontrol.username
    },

    buttonLabel () {
      return this.username ? 'Logout' : 'Login'
    },

    buttonAction () {
      return this.username ? 'logout' : 'openLoginDialog'
    }
  },

  methods: {
    dispatch () {
      this.$store.dispatch(this.buttonAction)
    }
  }
}
</script>

<style>
.login-state {
  flex: auto;
  text-align: right;
}

.login-state .info {
  font-size: var(--label-font-size);
  color:     var(--label-color);
}

.login-state .info .username {
  color: black;
}

.login-state .el-button {
  font-size: var(--label-font-size) !important;
  padding: 0;
}
</style>
