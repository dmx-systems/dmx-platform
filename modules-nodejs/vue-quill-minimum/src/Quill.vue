<template>
  <div ref="container"></div>
</template>

<script>
import Quill from 'quill'
import 'quill/dist/quill.snow.css'

export default {

  props: ['value', 'options'],

  mounted () {
    const quill = new Quill(this.$refs.container, this.options)
    quill.pasteHTML(this.value)
    quill.on('text-change', () => {
      const html = quill.root.innerHTML
      this.$emit('input', html)
    })
  }
}
</script>
