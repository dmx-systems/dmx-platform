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

  mounted () {
    // Note 1: both, the Topicmap Panel and the Detail Panel, rely on a populated type cache.
    // The type cache must be ready *before* "initialNavigation" is dispatched.
    //
    // Note 2: "initialNavigation" dispatches "renderTopicmap" which dispatches "syncTopicmap".
    // The "syncTopicmap" action is registered by the CytoscapeRenderer's mounted() hook (see comment there).
    // So we must do the dispatching here in the mounted() hook too. (created() might be too early.)
    // As CytoscapeRenderer is a child component of Webclient it is guaranteed to be mounted *before* Webclient.
    //
    const p1 = dm5.ready()
    const p2 = this.$store.state.workspaces.ready
    console.log('### Webclient mounted!', p1, p2)
    Promise.all([p1, p2]).then(() => {
      this.$store.dispatch('initialNavigation')
    })
  },

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
