<template>
  <div class="dmx-resizer" :style="{left}" v-if="visible" @mousedown="onMouseDown"></div>
</template>

<script>
export default {

  computed: {

    visible () {
      return this.$store.state.details && this.$store.state.details.visible
    },

    left () {
      const width = document.querySelector('.dmx-webclient').clientWidth
      return `${width - this.$store.state.detailPanelWidth}px`
    }
  },

  methods: {
    onMouseDown({pageX: initialPageX}) {
      const self = this
      const container = document.querySelector('.dmx-webclient')
      const panelL    = document.querySelector('.dmx-topicmap-panel')
      const panelR    = document.querySelector('.dmx-detail-panel')
      const initialPaneWidth = panelR.offsetWidth
      const {addEventListener, removeEventListener} = window

      this.$emit('resizeStart')

      const onMouseMove = function ({pageX}) {
        resize(pageX - initialPageX)
        self.$emit('resize')
      }

      const onMouseUp = function () {
        removeEventListener('mousemove', onMouseMove)
        removeEventListener('mouseup',   onMouseUp)
        self.$emit('resizeStop')
      }

      const resize = function (offset) {
        const containerWidth = container.clientWidth
        // console.log('resize', containerWidth, initialPaneWidth, offset)
        const paneWidth = initialPaneWidth - offset
        self.$store.dispatch('setDetailPanelWidth', paneWidth)
        panelL.style.width = `${containerWidth - paneWidth}px`
        panelR.style.width = `${paneWidth}px`
      }

      addEventListener('mousemove', onMouseMove)
      addEventListener('mouseup', onMouseUp)
    }
  }
}
</script>

<style>
.dmx-resizer {
  z-index: 1;
  position: absolute;
  width: 10px;
  height: 100%;
  margin-left: -5px;
  background-color: rgba(255, 0, 0, .2);
  cursor: col-resize;
}
</style>
