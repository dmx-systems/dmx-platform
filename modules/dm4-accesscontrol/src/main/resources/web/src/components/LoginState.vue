<template>
  <div class="login-state">
    <div v-if="username">
      Logged in as {{username}}
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
  font-size: 75%;
}
</style>
