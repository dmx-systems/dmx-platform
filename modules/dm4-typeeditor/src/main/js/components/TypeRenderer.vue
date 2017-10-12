<template>
  <div>
    <object-renderer :object="object" :mode="mode"></object-renderer>
    <!-- Data Type -->
    <div class="field-label">Data Type</div>
    <div v-if="infoMode">{{dataType.value}}</div>
    <el-select v-else v-model="type.dataType" size="small">
      <el-option v-for="dataType in dataTypes" :label="dataType.value" :value="dataType.uri" :key="dataType.uri">
      </el-option>
    </el-select>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  props: [
    'object',   // the type to render (a dm5.Topic)
    'mode'      // 'info' or 'form'
  ],

  computed: {

    type () {
      return this.object.asType()
    },

    dataType () {
      return this.type.getDataType()
    },

    dataTypes () {
      return this.$store.state.typeCache.dataTypes
    }
  },

  mixins: [
    require('dm5-detail-panel/src/components/mixins/infoMode').default
  ],

  components: {
    'object-renderer': require('dm5-detail-panel/src/components/ObjectRenderer')
  }
}
</script>

<style>
</style>
