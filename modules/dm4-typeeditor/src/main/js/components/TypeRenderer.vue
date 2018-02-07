<template>
  <div>
    <!-- Generic Object -->
    <dm5-object :object="object" :mode="mode" :level="0"></dm5-object>
    <!-- Data Type -->
    <div class="field">
      <div class="field-label">Data Type</div>
      <div v-if="infoMode">{{dataType.value}}</div>
      <el-select v-else v-model="object.dataTypeUri">
        <el-option-group label="Simple">
          <el-option :label="dataTypes['dm4.core.text'].value"     :value="dataTypes['dm4.core.text'].uri">
          </el-option>
          <el-option :label="dataTypes['dm4.core.html'].value"     :value="dataTypes['dm4.core.html'].uri">
          </el-option>
          <el-option :label="dataTypes['dm4.core.number'].value"   :value="dataTypes['dm4.core.number'].uri">
          </el-option>
          <el-option :label="dataTypes['dm4.core.boolean'].value"  :value="dataTypes['dm4.core.boolean'].uri">
          </el-option>
        </el-option-group>
        <el-option-group label="Composite">
          <el-option :label="dataTypes['dm4.core.value'].value"    :value="dataTypes['dm4.core.value'].uri">
          </el-option>
          <el-option :label="dataTypes['dm4.core.identity'].value" :value="dataTypes['dm4.core.identity'].uri">
          </el-option>
        </el-option-group>
      </el-select>
    </div>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  mixins: [
    require('./mixins/mode-prop').default,
    require('./mixins/info-mode').default
  ],

  props: {
    object: {   // the type to render
      type: dm5.Type,
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

  components: {
    'dm5-object': require('dm5-object-renderer/src/components/dm5-object')
  }
}
</script>

<style>
</style>
