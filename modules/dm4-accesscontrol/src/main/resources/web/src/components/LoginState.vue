<template>
  <div class="login-state">
    <div v-if="user">
      Logged in as {{user}}<br>
      <el-button type="text" size="mini" @click="logout">Logout</el-button>
    </div>
    <el-button v-else type="text" size="mini" @click="openLoginDialog">Login</el-button>
  </div>
</template>

<script>
export default {

  computed: {
    user () {
      return this.$store.state.accesscontrol.user
    }
  },

  methods: {

    openLoginDialog () {
      this.$store.dispatch('openLoginDialog')
    },

    logout () {
      console.log('Logout', this.user)
      this.$store.dispatch('logout')
      this.$store.dispatch('setUser', '')
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
