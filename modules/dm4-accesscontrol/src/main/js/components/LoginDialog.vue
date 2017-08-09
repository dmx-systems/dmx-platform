<template>
  <el-dialog custom-class="login-dialog" :visible.sync="visible" size="tiny" title="Login">
    <div>
      <div class="field-label">Username</div>
      <el-input v-model="credentials.username" size="small"></el-input>
    </div>
    <div class="password-field">
      <div class="field-label">Password</div>
      <el-input v-model="credentials.password" size="small"></el-input>
    </div>
    <div class="message">{{message}}</div>
    <div slot="footer">
      <el-button type="primary" size="small" @click="login">OK</el-button>
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
      get () {
        return this.$store.state.accesscontrol.visible
      },
      set (visible) {
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
.login-dialog .password-field {
  margin-top: 1em;
}

.login-dialog .message {
  margin-top: 1em;
}
</style>
