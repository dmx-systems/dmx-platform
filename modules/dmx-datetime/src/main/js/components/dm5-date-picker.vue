<template>
  <div class="dm5-date-picker">
    <div class="field-label">{{fieldLabel}}</div>
    <div v-if="infoMode">{{dateString}}</div>
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
        // console.log('date getter', this.object)
        const c = this.object.childs
        const y = c['dmx.datetime.year'].value
        const m = c['dmx.datetime.month'].value
        const d = c['dmx.datetime.day'].value
        // Topics created through "filling" have empty string values. If any topic is empty we don't create a
        // Date object but return an empty string. The Element UI Date Picker interprets that as "not set".
        return y && m && d && new Date(y, m - 1, d)
      },

      set (date) {
        // console.log('date setter', date)
        // Note: if a date field is cleared in the GUI we receive null here. To clear a field at server-side an empty
        // string must be sent. null would deserialize as JSONObject$Null causing the SimpleValue constructor to fail.
        const c = this.object.childs
        c['dmx.datetime.year'].value  = date && date.getFullYear()  || ''
        c['dmx.datetime.month'].value = date && date.getMonth() + 1 || ''
        c['dmx.datetime.day'].value   = date && date.getDate()      || ''
      }
    },

    dateString () {
      // Note: after updating the server sends the Date topic without its childs. This is a bug (#153).
      // Calculation of "this.date" would fail. As a workaround we display nothing.
      return this.object.childs['dmx.datetime.year'] && this.date.toLocaleDateString()
    }
  },

  components: {
    'el-date-picker': () => {
      import('element-ui/lib/theme-chalk/date-picker.css' /* webpackChunkName: "el-date-picker" */)
      return import('element-ui/lib/date-picker.js'       /* webpackChunkName: "el-date-picker" */)
    }
  }
}
</script>

<style>
</style>
