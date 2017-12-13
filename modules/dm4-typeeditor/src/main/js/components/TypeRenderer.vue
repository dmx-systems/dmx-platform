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
    require('dm5-detail-panel/src/components/mixins/info-mode').default
  ],

  components: {
    'dm5-object':        require('dm5-detail-panel/src/components/dm5-object'),
    'dm5-boolean-field': require('dm5-detail-panel/src/components/dm5-boolean-field')
  }
}
</script>

<style>
</style>
