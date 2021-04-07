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
    onMouseDown({target: resizer, pageX: initialPageX, pageY: initialPageY}) {
      if (resizer.className.match('dmx-resizer')) {
        const self = this
        const container = document.querySelector('.dmx-webclient')
        const panelL    = document.querySelector('.dmx-topicmap-panel')
        const panelR    = document.querySelector('.dmx-detail-panel')
        const initialPaneWidth = panelR.offsetWidth
        const {addEventListener, removeEventListener} = window

        this.$emit('resizeStart')

        const onMouseMove = function ({pageX, pageY}) {
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
}
</script>

<style>
.dmx-resizer {
  position: absolute;
  width: 5px;
  height: 100%;
  background-color: rgba(255, 0, 0, .3);  /* var(--background-color-darker); */
  cursor: col-resize
}
</style>
