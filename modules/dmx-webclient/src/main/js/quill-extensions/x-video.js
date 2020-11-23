export default {

  name: 'video',      // Same as blotName. Used in module.addHandler() calls to register toolbarHandler()

  extension (Quill) {

    const BlockEmbed = Quill.import('blots/block/embed')
    // const Video = Quill.import('formats/link')
    const Link = Quill.import('formats/link')

    const ATTRIBUTES = [
      'height',
      'width'
    ];

    class XVideo extends BlockEmbed {

      static create(value) {
        let node
        if (value.endsWith('.mp4')) {
          node = document.createElement('video')
          node.classList.add('ql-video')   // set manually as we don't call super.create()
          node.setAttribute('src', this.sanitize(value))
          node.setAttribute('controls', '')
          console.log('XVideo direct-link', node)
        } else {
          node = document.createElement('iframe')
          node.classList.add('ql-video')   // set manually as we don't call super.create()
          node.setAttribute('src', this.sanitize(value));
          node.setAttribute('frameborder', '0');
          node.setAttribute('allowfullscreen', true);
          console.log('XVideo embed', node)
        }
        return node;
      }

      static formats(domNode) {
        return ATTRIBUTES.reduce(function(formats, attribute) {
          if (domNode.hasAttribute(attribute)) {
            formats[attribute] = domNode.getAttribute(attribute);
          }
          return formats;
        }, {});
      }

      static sanitize(url) {
        console.log('sanitize')
        return Link.sanitize(url);
      }

      static value(domNode) {
        return domNode.getAttribute('src');
      }

      format(name, value) {
        if (ATTRIBUTES.indexOf(name) > -1) {
          if (value) {
            this.domNode.setAttribute(name, value);
          } else {
            this.domNode.removeAttribute(name);
          }
        } else {
          super.format(name, value);
        }
      }
    }
    XVideo.blotName = 'video'
    XVideo.className = 'ql-video'          // Note: x-video tag name varies, so while HTML->Parchment transformation
    //XVideo.tagName = 'IFRAME'              // ... we detect DOM elements by class name
    return XVideo
  },

  /*toolbarHandler (value) {
    console.log('x-video toolbarHandler', value, this)
    this.quill.theme.tooltip.edit('video');
    // this.handlers.video.call(this, value)
  },*/

  /*quillReady (quill) {
    // console.log('x-video quillReady', quill)
    // FIXME: handle multiple Quill instances
    // const button = document.querySelector('button.ql-x-video')
    // button.classList.add('ql-video')
    // button.textContent = 'T'    // TODO: use real (SVG) icon
    // button.setAttribute('title', 'Insert topic link')
  },*/

  /* infoDOMReady (dom) {
    // console.log('x-video infoDOMReady', dom)
  } */
}
