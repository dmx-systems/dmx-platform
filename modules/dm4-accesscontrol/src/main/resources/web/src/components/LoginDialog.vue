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
      this.$store.dispatch('login', this.credentials).then(response => {
        console.log('Login OK', this.credentials.username)
        this.message = 'Login OK'
        return this.credentials.username
      }).catch(error => {
        console.log('Login failed', error)
        this.message = 'Login failed'
      }).then(username => {
        if (username) {
          this.$store.dispatch('closeLoginDialog')
          this.$store.dispatch('setUser', username)
        }
      })
    }
  }
}
</script>

<style>
</style>
