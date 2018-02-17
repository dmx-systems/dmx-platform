<template>
  <div class="dm5-webclient">
    <div v-for="compDef in compDefs" :id="mountId(compDef)" :key="compDef.id"></div>
    <dm5-search-widget :menu-topic-types="menuTopicTypes"></dm5-search-widget>
  </div>
</template>

<script>
export default {

  computed: {

    compDefs () {
      return this.$store.state.compDefs.webclient
    },

    menuTopicTypes () {
      return this.$store.getters.menuTopicTypes
    }
  },

  methods: {
    mountId (compDef) {
      return `mount-${compDef.id}`
    }
  },

  components: {
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
  flex-basis: 70%;
  overflow: hidden;     /* leave place for the detail panel */
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
