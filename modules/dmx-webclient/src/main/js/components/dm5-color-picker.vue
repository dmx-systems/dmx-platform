<template>
  <div class="dm5-color-field">
    <div v-if="infoMode" :style="style">{{object.value}}</div>
    <el-color-picker v-else v-model="object.value" size="medium"></el-color-picker>
  </div>
</template>

<script>
export default {

  mixins: [
    require('./mixins/object').default,       // object to render
    require('./mixins/mode').default,
    require('./mixins/info-mode').default
  ],

  data () {
    return {
      style: {
        'background-color': this.object.value
      }
    }
  }
}
</script>
