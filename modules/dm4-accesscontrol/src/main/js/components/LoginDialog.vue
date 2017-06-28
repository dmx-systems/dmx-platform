<template>
  <el-dialog v-model="loginDialog" title="Login">
    <div>
      Username
      <el-input v-model="credentials.username" size="small"></el-input>
      Password
      <el-input v-model="credentials.password" size="small"></el-input>
      <div>{{message}}</div>
    </div>
    <div slot="footer">
      <el-button type="primary" @click="login">OK</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {

  data () {
    return {
      credentials: {
        username: '',
        password: ''
      },
      message:  ''
    }
  },

  computed: {
    loginDialog () {
      return this.$store.state.accesscontrol.loginDialog
    }
  },

  methods: {
    login () {
      this.$store.dispatch('login', this.credentials).then(success => {
        this.message = success ? 'Login OK' : 'Login failed'
      })
    }
  }
}
</script>

<style>
</style>
