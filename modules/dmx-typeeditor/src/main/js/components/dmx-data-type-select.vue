<template>
  <el-select v-model="type.dataTypeUri"><!-- eslint-disable-line vue/no-mutating-props -->
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
      <template v-if="isTopicType">
        <el-option :label="dataTypes['dmx.core.value'].value"  :value="dataTypes['dmx.core.value'].uri">
        </el-option>
        <el-option :label="dataTypes['dmx.core.entity'].value" :value="dataTypes['dmx.core.entity'].uri">
        </el-option>
      </template>
      <el-option v-else :label="dataTypes['dmx.core.composite'].value" :value="dataTypes['dmx.core.composite'].uri">
      </el-option>
    </el-option-group>
  </el-select>
</template>

<script>
import dmx from 'dmx-api'

export default {

  props: {
    type: {   // the type whose data type to render
      type: dmx.DMXType,
      required: true
    }
  },

  computed: {

    isTopicType () {
      return this.type.isTopicType
    },

    dataTypes () {
      return this.$store.state.typeCache.dataTypes
    }
  }
}
</script>
