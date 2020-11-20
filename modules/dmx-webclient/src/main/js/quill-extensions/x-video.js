export default {

  name: 'x-video',

  extension (Quill) {

    const Video = Quill.import('formats/video')

    class XVideo extends Video {

      static create(value) {
        let node
        if (value.endsWith('.mp4')) {
          node = document.createElement('video')
          node.classList.add('x-video')
          node.setAttribute('src', super.sanitize(value))
          // node.setAttribute('type', 'video/mp4')
          node.setAttribute('controls', '')
          console.log('XVideo direct-link', node)
        } else {
          node = super.create(value)
          console.log('XVideo oembed-link', node)
        }
        return node
      }
    }
    XVideo.blotName = 'x-video';
    // XVideo.className = 'x-video';
    // XVideo.tagName = 'DIV';
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
