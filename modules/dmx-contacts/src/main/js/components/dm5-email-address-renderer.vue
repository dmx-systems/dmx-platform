<template>
  <el-link v-if="infoMode" :href="mailto" type="primary" :underline="false">
    {{object.value}}
  </el-link>
  <el-input v-else v-model="object.value"></el-input>
</template>

<script>
export default {

  mixins: [
    require('./mixins/object').default,
    require('./mixins/mode').default,
    require('./mixins/info-mode').default
  ],

  computed: {
    mailto () {
      return 'mailto:' + this.object.value
    }
  }
}
</script>
