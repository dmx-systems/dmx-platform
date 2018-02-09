<template>
  <div class="dm5-webclient">
    <div v-for="compDef in compDefs" :id="mountId(compDef)" :key="compDef.id"></div>
    <dm5-detail-panel v-if="detailPanelVisibility"></dm5-detail-panel>
    <dm5-search-widget :menu-topic-types="menuTopicTypes"></dm5-search-widget>
  </div>
</template>

<script>
export default {

  computed: {

    // export context ### TODO: drop

    object () {
      return this.$store.state.object
    },

    writable () {
      return this.$store.state.writable
    },

    detail () {
      return this.$store.state.detail
    },

    mode () {
      return this.$store.state.mode
    },

    objectRenderers () {
      return this.$store.state.objectRenderers
    },

    //

    detailPanelVisibility () {
      return this.detail !== undefined
    },

    compDefs () {
      return this.$store.state.compDefs.webclient
    },

    menuTopicTypes () {
      return this.$store.getters.menuTopicTypes
    }
  },

  // Showing/hiding the detail panel changes the topicmap panel dimensions.
  // The Cytoscape renderer size needs to adapt.
  watch: {
    detailPanelVisibility () {
      this.$nextTick(() => {
        this.$store.dispatch('resizeTopicmapRenderer')
      })
    }
  },

  methods: {
    mountId (compDef) {
      return `mount-${compDef.id}`
    }
  },

  provide () {
    // Injection and Reactivity:
    // Returning {object: this.object}: no reactivity.
    // Returning a calculated property which returns {object: this.object}: no reactivity.
    // Returning just "this": only the topicmap panel is reactive, not the detail panel. This feels strange!
    return {
      context: this
    }
  },

  components: {
    'dm5-detail-panel':  require('dm5-detail-panel'),
    'dm5-search-widget': require('dm5-search-widget')
  }
}
</script>

<style>
.dm5-webclient {
  height: 100%;
  display: flex;
}

.dm5-webclient .dm5-topicmap-panel {
  flex-grow: 1;
  position: relative;
}

.dm5-webclient .dm5-detail-panel {
  flex-grow: 1;
  flex-basis: 30%;
  overflow: auto;
  box-sizing: border-box;
  padding: 0 12px 12px 12px;
  background-color: var(--background-color);
}
</style>
