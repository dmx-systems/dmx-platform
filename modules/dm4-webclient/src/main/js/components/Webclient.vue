<template>
  <div class="dm5-webclient">
    <component v-for="comp in components" :is="comp.comp" :key="comp.id"></component>
    <dm5-detail-panel v-if="detailPanel"></dm5-detail-panel>
    <dm5-search-widget :menu-topic-types="menuTopicTypes"></dm5-search-widget>
  </div>
</template>

<script>
export default {

  computed: {

    // export context

    object () {
      return this.$store.state.object
    },

    writable () {
      return this.$store.state.writable
    },

    mode () {
      return this.$store.state.mode
    },

    inlineCompId () {
      return this.$store.state.inlineCompId
    },

    objectRenderers () {
      return this.$store.state.objectRenderers
    },

    //

    detailPanel () {
      return this.$store.state.detailPanel
    },

    components () {
      return this.$store.state.components.webclient
    },

    menuTopicTypes () {
      return this.$store.getters.menuTopicTypes
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
