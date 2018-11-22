<template>
  <el-select v-model="type.dataTypeUri">
    <el-option-group label="Simple">
      <el-option :label="dataTypes['dmx.core.text'].value"       :value="dataTypes['dmx.core.text'].uri">
      </el-option>
      <el-option :label="dataTypes['dmx.core.number'].value"     :value="dataTypes['dmx.core.number'].uri">
      </el-option>
      <el-option :label="dataTypes['dmx.core.boolean'].value"    :value="dataTypes['dmx.core.boolean'].uri">
      </el-option>
      <el-option :label="dataTypes['dmx.core.html'].value"       :value="dataTypes['dmx.core.html'].uri">
      </el-option>
    </el-option-group>
    <el-option-group label="Composite">
      <template v-if="isTopicType">
        <el-option :label="dataTypes['dmx.core.value'].value"    :value="dataTypes['dmx.core.value'].uri">
        </el-option>
        <el-option :label="dataTypes['dmx.core.identity'].value" :value="dataTypes['dmx.core.identity'].uri">
        </el-option>
      </template>
      <el-option :label="dataTypes['dmx.core.composite'].value"  :value="dataTypes['dmx.core.composite'].uri" v-else>
      </el-option>
    </el-option-group>
  </el-select>
</template>

<script>
import dm5 from 'dm5'

export default {

  props: {
    type: {   // the type whose data type to render
      type: dm5.Type,
      required: true
    }
  },

  computed: {

    isTopicType () {
      return this.type.isTopicType()
    },

    dataTypes () {
      return this.$store.state.typeCache.dataTypes
    }
  }
}
</script>