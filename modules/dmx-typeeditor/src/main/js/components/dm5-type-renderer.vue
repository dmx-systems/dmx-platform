<template>
  <div class="dm5-type-renderer">
    <!-- Generic Object -->
    <dm5-object :object="object" :level="0" :context="context"></dm5-object>
    <!-- Type URI -->
    <div class="field">
      <div class="field-label">Type URI</div>
      <div v-if="infoMode">{{object.uri}}</div>
      <el-input v-else v-model="object.uri"></el-input>
    </div>
    <!-- Data Type -->
    <div class="field">
      <div class="field-label">Data Type</div>
      <div v-if="infoMode">{{dataType.value}}</div>
      <el-select v-else v-model="object.dataTypeUri">
        <el-option-group label="Simple">
          <el-option :label="dataTypes['dmx.core.text'].value"     :value="dataTypes['dmx.core.text'].uri">
          </el-option>
          <el-option :label="dataTypes['dmx.core.number'].value"   :value="dataTypes['dmx.core.number'].uri">
          </el-option>
          <el-option :label="dataTypes['dmx.core.boolean'].value"  :value="dataTypes['dmx.core.boolean'].uri">
          </el-option>
          <el-option :label="dataTypes['dmx.core.html'].value"     :value="dataTypes['dmx.core.html'].uri">
          </el-option>
        </el-option-group>
        <el-option-group label="Composite">
          <el-option :label="dataTypes['dmx.core.value'].value"    :value="dataTypes['dmx.core.value'].uri">
          </el-option>
          <el-option :label="dataTypes['dmx.core.identity'].value" :value="dataTypes['dmx.core.identity'].uri">
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
    require('./mixins/info-mode').default,
    require('./mixins/context').default
  ],

  props: {
    object: {   // the type to render
      type: dm5.Type,
      required: true
    }
  },

  computed: {

    mode () {
      return this.context.mode
    },

    dataType () {
      return this.object.getDataType()
    },

    dataTypes () {
      return this.$store.state.typeCache.dataTypes
    }
  },

  components: {
    'dm5-object': require('dm5-object-renderer/src/components/dm5-object').default
  }
}
</script>

<style>
</style>
