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
        const pane      = document.querySelector('.dmx-detail-panel')
        const {
          offsetWidth: initialPaneWidth,
          offsetHeight: initialPaneHeight,
        } = pane

        const {addEventListener, removeEventListener} = window

        const resize = (initialSize, offset = 0) => {
          const containerWidth = container.clientWidth
          // console.log('resize', containerWidth, initialSize, offset)
          const paneWidth = initialSize - offset
          this.$store.dispatch('setDetailPanelWidth', paneWidth)
          return pane.style['flex-basis'] = paneWidth + 'px'
        }

        // Resize once to get current computed size
        let size = resize()

        // Trigger paneResizeStart event
        this.$emit('resizeStart', pane, resizer, size)

        const onMouseMove = function({pageX, pageY}) {
          size = resize(initialPaneWidth, pageX - initialPageX)
          self.$emit('resize', pane, resizer, size)
        }

        const onMouseUp = function() {
          // Run resize one more time to set computed width/height.
          size = resize(pane.clientWidth)
          removeEventListener('mousemove', onMouseMove)
          removeEventListener('mouseup', onMouseUp)
          self.$emit('resizeStop', pane, resizer, size)
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
