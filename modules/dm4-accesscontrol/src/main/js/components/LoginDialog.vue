<template>
  <el-dialog custom-class="login-dialog" :visible="visible" width="20em" title="Login" @open="open" @close="close">
    <div>
      <div class="field-label">Username</div>
      <el-input v-model="credentials.username" ref="username" @keyup.native.enter="advance"></el-input>
    </div>
    <div class="password-field">
      <div class="field-label">Password</div>
      <el-input v-model="credentials.password" ref="password" @keyup.native.enter="login"></el-input>
    </div>
    <div class="message">{{message}}</div>
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
      message: ''
    }
  },

  computed: {
    visible () {
      return this.$store.state.accesscontrol.visible
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

    open () {
      this.message = ''
      // Note: on open the DOM is not yet ready
      this.$nextTick(() => this.$refs.username.focus())
    },

    close () {
      // FIXME: called twice when closing programmatically (through login())
      // console.log('close login')
      this.$store.dispatch('closeLoginDialog')
    },

    advance () {
      this.$refs.password.focus()
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
