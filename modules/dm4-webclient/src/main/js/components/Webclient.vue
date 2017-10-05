<template>
  <div class="webclient">
    <component v-for="comp in components" :is="comp" :key="comp._dm5_id">
      <toolbar slot="dm5-topicmap-panel"></toolbar>
    </component>
    <dm5-detail-panel></dm5-detail-panel>
    <dm5-search-widget :menu-topic-types="menuTopicTypes" :extra-menu-items="extraMenuItems"></dm5-search-widget>
  </div>
</template>

<script>
import dm5 from 'dm5'

export default {

  data () {
    return {
      // TODO: let the respective module inject the extra items
      extraMenuItems: [
        {
          uri: 'dm4.topicmaps.topicmap',
          label: 'Topicmap',
          create: name => {
            this.$store.dispatch('createTopicmap', name)
          }
        },
        {
          uri: 'dm4.workspaces.workspace',
          label: 'Workspace',
          create: name => {
            this.$store.dispatch('createWorkspace', name)
          }
        }
      ]
    }
  },

  computed: {

    components () {
      return this.$store.state.components.webclient
    },

    menuTopicTypes () {
      return this.$store.getters.menuTopicTypes
    }
  },

  components: {
    'toolbar':           require('./Toolbar'),
    'dm5-detail-panel':  require('dm5-detail-panel'),
    'dm5-search-widget': require('dm5-search-widget')
  }
}
</script>

<style>
.webclient {
  height: 100%;
  display: flex;
}

.webclient .topicmap-panel {
  flex-basis: 70%;
  position: relative;
}

.webclient .dm5-detail-panel {
  flex-basis: 30%;
  overflow: auto;
  box-sizing: border-box;
  padding: 0 12px 12px 12px;
  background-color: var(--background-color);
}
</style>
