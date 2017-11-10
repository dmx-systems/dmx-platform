<template>
  <div>
    <!-- Generic Object -->
    <object-renderer :object="object" :mode="mode"></object-renderer>
    <!-- Data Type -->
    <div class="field">
      <div class="field-label">Data Type</div>
      <div v-if="infoMode">{{dataType.value}}</div>
      <el-select v-else v-model="object.dataTypeUri" size="small">
        <el-option v-for="dataType in dataTypes" :label="dataType.value" :value="dataType.uri" :key="dataType.uri">
        </el-option>
      </el-select>
    </div>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  props: {
    object: {   // the type to render
      type: [dm5.TopicType, dm5.AssocType],   // TODO: export dm5.Type?
      required: true
    }
  },

  computed: {

    dataType () {
      return this.object.getDataType()
    },

    dataTypes () {
      return this.$store.state.typeCache.dataTypes
    }
  },

  mixins: [
    require('dm5-detail-panel/src/components/mixins/mode').default,
    require('dm5-detail-panel/src/components/mixins/infoMode').default
  ],

  components: {
    'object-renderer': require('dm5-detail-panel/src/components/ObjectRenderer'),
    'boolean-field':   require('dm5-detail-panel/src/components/BooleanField')
  }
}
</script>

<style>
</style>
