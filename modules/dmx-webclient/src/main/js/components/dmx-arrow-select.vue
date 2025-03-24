<template>
  <div :class="['dmx-arrow-select', mode]">
    <div v-if="infoMode">{{object.value}}</div>
    <el-select v-else v-model="object.value" clearable>
      <el-option v-for="shape in shapes" :label="shape" :value="shape" :key="shape"></el-option>
    </el-select>
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
      shapes: [
        'triangle',
        'triangle-tee',
        'circle-triangle',
        'triangle-cross',
        'triangle-backcurve',
        'vee',
        'tee',
        'square',
        'circle',
        'diamond',
        'chevron'
      ]
    }
  }
}
</script>

<style>
.dmx-arrow-select .el-select {
  width: 200px !important;
}
</style>
