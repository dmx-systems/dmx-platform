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
    console.log('dm5-date-picker created', this.assocDef)
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
    }
  }
}
</script>

<style>
</style>
