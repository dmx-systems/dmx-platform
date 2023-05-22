<template>
  <div class="dmx-type-renderer">
    <!-- Type Value -->
    <dmx-value-renderer :object="object" :level="0" :path="[]" :context="context"></dmx-value-renderer>
    <!-- Type URI -->
    <div class="field">
      <div class="field-label">Type URI</div>
      <div v-if="infoMode">{{type.uri}}</div>
      <el-input v-else v-model="type.uri"></el-input><!-- eslint-disable-line vue/no-mutating-props -->
    </div>
    <!-- Data Type -->
    <div class="field">
      <div class="field-label">Data Type</div>
      <div v-if="infoMode">{{dataType.value}}</div>
      <dmx-data-type-select v-else :type="type"></dmx-data-type-select>
    </div>
    <!-- Comp Defs -->
    <dmx-comp-def-list v-if="type.isComposite" :comp-defs="compDefs" :mode="mode" @comp-def-click="click">
    </dmx-comp-def-list>
  </div>
</template>

<script>
import dmx from 'dmx-api'

export default {

  mixins: [
    require('./mixins/info-mode').default,
    require('./mixins/context').default
  ],

  props: {
    object: {   // the type to render
      type: dmx.DMXType,
      required: true
    }
  },

  computed: {

    type () {
      return this.object
    },

    mode () {
      return this.context.mode
    },

    dataType () {
      return this.type.dataType
    },

    compDefs () {
      return this.type.compDefs
    }
  },

  methods: {
    click (compDef) {
      const childType = compDef.childType
      childType.assoc = compDef    // type cache side effect ### FIXME
      this.$store.dispatch('revealRelatedTopic', {relTopic: childType})
    }
  },

  components: {
    'dmx-data-type-select': require('./dmx-data-type-select').default,
    'dmx-comp-def-list':    require('./dmx-comp-def-list').default
  }
}
</script>

<style>
.dmx-type-renderer > .field,
.dmx-type-renderer .dmx-comp-def-list {
  margin-top: var(--field-spacing);
}
</style>
