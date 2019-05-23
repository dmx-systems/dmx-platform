<template>
  <el-dialog custom-class="dm5-login-dialog" :visible="visible" width="20em" title="Login" @open="open" @close="close">
    <div class="field" v-if="showSelect">
      <div class="field-label">Authorization Method</div>
      <el-select v-model="authMethod">
        <el-option v-for="authMethod in authMethods" :value="authMethod" :key="authMethod"></el-option>
      </el-select>
    </div>
    <div class="field">
      <div class="field-label">Username</div>
      <el-input v-model="credentials.username" ref="username" @keyup.native.enter="advance"></el-input>
    </div>
    <div class="field">
      <div class="field-label">Password</div>
      <el-input v-model="credentials.password" ref="password" @keyup.native.enter="login" type="password"></el-input>
    </div>
    <div class="field">
      {{message}}
    </div>
    <div slot="footer">
      <el-button type="primary" @click="login">OK</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {

  created () {
    // console.log('dm5-login-dialog created', this.authMethods)
    this.authMethod = this.authMethods[0]
  },

  mounted () {
    // console.log('dm5-login-dialog mounted')
  },

  data () {
    return {
      authMethod: undefined,
      credentials: {
        username: '',
        password: ''
      },
      message: ''
    }
  },

  computed: {

    authMethods () {
      return this.$store.state.accesscontrol.authMethods
    },

    visible () {
      return this.$store.state.accesscontrol.visible
    },

    showSelect () {
      return this.authMethods.length > 1
    }
  },

  methods: {

    login () {
      this.$store.dispatch('login', {
        credentials: this.credentials,
        authMethod:  this.authMethod
      }).then(success => {
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
.dm5-login-dialog .field + .field {
  margin-top: var(--field-spacing);
}
</style>
