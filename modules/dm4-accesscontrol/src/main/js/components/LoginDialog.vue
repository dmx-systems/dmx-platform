<template>
  <el-dialog :visible.sync="visible" title="Login">
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
    visible: {
      get: function () {
        return this.$store.state.accesscontrol.visible
      },
      set: function (visible) {
        // console.log('visible setter', visible)   // FIXME: called twice on close
        if (!visible) {
          this.close()
        }
      }
    }
  },

  methods: {

    login () {
      this.$store.dispatch('login', this.credentials).then(success => {
        if (success) {
          this.message = 'Login OK'
          this.close()
        } else {
          this.message = 'Login failed'
        }
      })
    },

    close () {
      this.$store.dispatch('closeLoginDialog')
    }
  }
}
</script>

<style>
</style>
