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
      return this.$store.state.topicmapPanelWidth + 'px'
    }
  },

  methods: {
    onMouseDown({target: resizer, pageX: initialPageX, pageY: initialPageY}) {
      if (resizer.className.match('dmx-resizer')) {
        let self = this
        let container = document.querySelector('.dmx-webclient')
        let pane      = document.querySelector('.dmx-detail-panel')
        let {
          offsetWidth: initialPaneWidth,
          offsetHeight: initialPaneHeight,
        } = pane

        const {addEventListener, removeEventListener} = window

        const resize = (initialSize, offset = 0) => {
          let containerWidth = container.clientWidth
          // console.log('resize', containerWidth, initialSize, offset)
          let paneWidth = initialSize - offset
          return pane.style['flex-basis'] = paneWidth + 'px'
        }

        // Resize once to get current computed size
        let size = resize()

        // Trigger paneResizeStart event
        self.$emit('resizeStart', pane, resizer, size)

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
  background-color: var(--background-color-darker);
  cursor: col-resize
}
</style>
