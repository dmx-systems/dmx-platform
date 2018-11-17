<template>
  <div class="dm5-user-account-options">
    <div class="field-label">Password</div>
    <el-input v-model="password" type="password"></el-input>
  </div>
</template>

<script>
export default {
  data () {
    return {
      password: ''
    }
  }
}
</script>
