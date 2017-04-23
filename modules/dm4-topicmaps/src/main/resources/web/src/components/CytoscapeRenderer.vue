<template>
  <div class="cytoscape-renderer" ref="container"></div>
</template>

<script>
import cytoscape from 'cytoscape'

export default {
  data: () => ({
    cy: undefined
  }),

  mounted () {
    console.log('CytoscapeRenderer created!', this.$refs.container)
    this.cy = cytoscape({
      container: this.$refs.container,
      style: [
        {
          selector: 'node',
          style: {
            'shape': 'rectangle',
            'width': 'label',
            'height': 'label',
            'label': 'data(label)',
            'text-valign': 'center'
          }
        },
        {
          selector: 'edge',
          style: {
            'width': 5,
            'curve-style': 'bezier',
            'target-arrow-shape': 'triangle',
            'label': 'data(id)',
            'text-rotation': 'autorotate'
          }
        }
      ],
      layout: {
        name: 'preset'
      }
    })
    //
    this.$store.watch(
      state => state.topicmaps.topicmap,
      topicmap => this.setTopicmapData(topicmap)
    )
  },

  methods: {
    setTopicmapData (topicmap) {
      console.log('setTopicmapData', topicmap)
      var elements = topicmap.topics.map(topic => ({
        data: {
          id: topic.id,
          label: topic.value
        },
        position: {
          x: topic.view_props['dm4.topicmaps.x'],
          y: topic.view_props['dm4.topicmaps.y']
        }
      }))
      this.cy.add(elements)
    }
  }
}
</script>

<style>
.cytoscape-renderer {
  flex: auto;
  overflow: hidden;
  background-color: #fef;
}
</style>
