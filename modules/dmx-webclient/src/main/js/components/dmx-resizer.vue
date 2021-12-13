<template>
  <div class="dmx-resizer" :style="{left: left + 'px'}" v-if="visible" @mousedown="onMouseDown"></div>
</template>

<script>
export default {

  computed: {

    visible () {
      return this.$store.state.details && this.$store.state.details.visible
    },

    left () {
      return this.$store.state.resizerPos
    }
  },

  methods: {

    onMouseDown ({pageX: initialPageX}) {
      const self = this
      const container = document.querySelector('.dmx-webclient')
      const panelL    = document.querySelector('.dmx-topicmap-panel')
      const panelR    = document.querySelector('.dmx-detail-panel')
      const initialPaneRWidth = panelR.offsetWidth
      const {addEventListener, removeEventListener} = window

      this.$emit('resizeStart')

      const onMouseMove = function ({pageX}) {
        const pos = resize(pageX - initialPageX)
        self.$store.dispatch('setResizerPos', pos)
      }

      const onMouseUp = function () {
        removeEventListener('mousemove', onMouseMove)
        removeEventListener('mouseup',   onMouseUp)
        self.$emit('resizeStop')
      }

      const resize = function (offset) {
        const paneRWidth = initialPaneRWidth - offset
        const paneLWidth = container.clientWidth - paneRWidth
        panelL.style.width = `${paneLWidth}px`
        panelR.style.width = `${paneRWidth}px`
        return paneLWidth
      }

      addEventListener('mousemove', onMouseMove)
      addEventListener('mouseup', onMouseUp)
    },

    // Public API

    resize () {
      const container = document.querySelector('.dmx-webclient')        //
      const panelL    = document.querySelector('.dmx-topicmap-panel')   // TODO: DRY
      const panelR    = document.querySelector('.dmx-detail-panel')     //
      const paneLWidth = this.left
      const paneRWidth = container.clientWidth - paneLWidth
      panelL.style.width = `${paneLWidth}px`
      panelR.style.width = `${paneRWidth}px`
    }
  }
}
</script>

<style>
.dmx-resizer {
  z-index: 1;
  position: absolute;
  width: 16px;
  height: 100%;
  margin-left: -8px;
  cursor: col-resize;
}
</style>
