<template>
  <el-link v-if="infoMode" :href="object.value" type="primary" :underline="false" target="_blank">
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
  ]
}
</script>
