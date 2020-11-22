export default {

  name: 'x-video',      // Same as blotName. Used in module.addHandler() calls to register toolbarHandler()

  extension (Quill) {

    const Video = Quill.import('formats/video')

    class XVideo extends Video {

      static create(value) {
        let node
        if (value.endsWith('.mp4')) {
          node = document.createElement('video')
          node.classList.add('x-video')   // set manually as we don't call super.create()
          node.setAttribute('src', super.sanitize(value))
          node.setAttribute('controls', '')
          console.log('XVideo direct-link', node)
        } else {
          node = super.create(value)
          console.log('XVideo oembed-link', node)
        }
        return node
      }
    }
    XVideo.blotName = 'x-video'
    XVideo.className = 'x-video'          // Note: x-video tag name varies, so while HTML->Parchment transformation
    // XVideo.tagName = 'DIV'             // ... we detect DOM elements by class name
    return XVideo
  },

  /* toolbarHandler (value) {
    console.log('x-video toolbarHandler', value)
  }, */

  quillReady (quill) {
    console.log('x-video quillReady', quill)
  },

  infoDOMReady (dom) {
    console.log('x-video infoDOMReady', dom)
  }
}
