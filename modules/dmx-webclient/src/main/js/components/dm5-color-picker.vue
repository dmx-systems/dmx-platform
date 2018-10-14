<template>
  <div :class="['dm5-color-picker', mode]">
    <div class="color-box" v-if="infoMode" :style="style"></div>
    <el-color-picker v-else v-model="object.value" size="medium"></el-color-picker>
    <div class="color-value">{{object.value}}</div>
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

<style>
.dm5-color-picker {
  display: flex;
  align-items: center;
}

.dm5-color-picker .color-box {
  width: 24px;
  height: 24px;
  box-sizing: border-box;   /* copied from .el-color-picker__color */
  border: 1px solid #999;   /* copied from .el-color-picker__color */
  border-radius: 2px;       /* copied from .el-color-picker__color */
}

.dm5-color-picker .color-value {
  margin-left: 12px;
}

.dm5-color-picker.form .color-value {
  margin-left: 7px;
  margin-right: 12px;
}
</style>
