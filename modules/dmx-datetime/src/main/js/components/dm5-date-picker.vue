<template>
  <div class="dm5-date-picker">
    <div class="field-label">{{fieldLabel}}</div>
    <div v-if="infoMode">{{object.value}}</div>
    <el-date-picker v-else v-model="date"></el-date-picker>
  </div>
</template>

<script>
export default {

  created () {
    // console.log('dm5-date-picker created', this.assocDef)
  },

  mixins: [
    require('./mixins/object').default,       // object to render
    require('./mixins/info-mode').default,
    require('./mixins/assoc-def').default,    // undefined for top-level object
    require('./mixins/context').default
  ],

  computed: {

    type () {
      return this.object.getType()
    },

    // TODO: copy in dm5-value-renderer.vue
    fieldLabel () {
      const customAssocType = this.assocDef && this.assocDef.getCustomAssocType()
      return customAssocType && customAssocType.isSimple() ? customAssocType.value : this.type.value
    },

    mode () {
      return this.context.mode
    },

    date: {

      get () {
        console.log('date getter', this.object)
        const c = this.object.childs
        const y = c['dmx.datetime.year'].value
        const m = c['dmx.datetime.month'].value
        const d = c['dmx.datetime.day'].value
        return y && m && d && new Date(y, m - 1, d)
      },

      set (date) {
        console.log('date setter', date)
        // Note: if date field is cleared null is passed
        const d = this.object.childs
        d['dmx.datetime.year'].value  = date && date.getFullYear()  || ''
        d['dmx.datetime.month'].value = date && date.getMonth() + 1 || ''
        d['dmx.datetime.day'].value   = date && date.getDate()      || ''
      }
    }
  }
}
</script>

<style>
</style>
