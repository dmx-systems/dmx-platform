<template>
  <div class="dm5-webclient">
    <div v-for="compDef in compDefs" :id="mountId(compDef)" :key="compDef.id"></div>
  </div>
</template>

<script>
import dm5 from 'dm5'
import axios from 'axios'
import Vue from 'vue'

export default {

  provide: {
    dm5, axios, Vue
  },

  computed: {
    compDefs () {
      return this.$store.state.compDefs.webclient
    }
  },

  methods: {
    mountId (compDef) {
      return `mount-${compDef.id}`
    }
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
  border-left: 1px solid var(--border-color-lighter);
}
</style>
