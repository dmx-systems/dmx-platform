<template>
  <div class="dm5-webclient">
    <div v-for="comp in components" :id="mountId(comp)" :key="comp.id"></div>
    <dm5-detail-panel></dm5-detail-panel>
    <dm5-search-widget :menu-topic-types="menuTopicTypes"></dm5-search-widget>
  </div>
</template>

<script>
export default {

  computed: {

    components () {
      return this.$store.state.components.webclient
    },

    menuTopicTypes () {
      return this.$store.getters.menuTopicTypes
    }
  },

  methods: {
    mountId (comp) {
      return `mount-${comp.id}`
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
  flex-basis: 70%;
  position: relative;
}

.dm5-webclient .dm5-detail-panel {
  flex-basis: 30%;
  overflow: auto;
  box-sizing: border-box;
  padding: 0 12px 12px 12px;
  background-color: var(--background-color);
}
</style>
