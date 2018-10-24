<template>
  <div class="dm5-date-picker">
    <div v-if="infoMode">{{object.value}}</div>
    <el-date-picker v-else v-model="date"></el-date-picker>
  </div>
</template>

<script>
export default {

  mixins: [
    require('./mixins/object').default,       // object to render
    require('./mixins/info-mode').default,
    require('./mixins/context').default
  ],

  computed: {

    date: {
      get () {
        console.log('date getter', this.object)
        const d = this.object.childs
        return new Date(
          d['dmx.datetime.year'].value,
          d['dmx.datetime.month'].value - 1,
          d['dmx.datetime.day'].value
        )
      },
      set (date) {
        console.log('date setter', date.getFullYear(), date.getMonth() + 1, date.getDate())
        const d = this.object.childs
        d['dmx.datetime.year'].value  = date.getFullYear()
        d['dmx.datetime.month'].value = date.getMonth() + 1
        d['dmx.datetime.day'].value   = date.getDate()
      }
    },

    mode () {
      return this.context.mode
    }
  }
}
</script>

<style>
</style>
