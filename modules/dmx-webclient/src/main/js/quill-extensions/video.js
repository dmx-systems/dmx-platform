export default {

  name: 'video',      // Same as blotName. Used in module.addHandler() calls to register toolbarHandler()

  overwrite: true,

  extension (Quill) {

    const BlockEmbed = Quill.import('blots/block/embed')
    const Link = Quill.import('formats/link')

    const ATTRIBUTES = [
      'height',
      'width'
    ];

    class Video extends BlockEmbed {

      static create(value) {
        const tag = value.endsWith('.mp4')  ? 'video' : 'iframe'    // TODO: isDirectLink()
        const node = document.createElement(tag)
        node.classList.add('ql-video')    // set manually as we don't call super.create()
        node.setAttribute('src', Link.sanitize(value))
        if (tag === 'video') {
          node.setAttribute('controls', '')
          console.log('Video direct-link', node)
        } else {
          node.setAttribute('frameborder', '0');
          node.setAttribute('allowfullscreen', true);
          console.log('Video embed-link', node)
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
    Video.blotName = 'video'
    Video.className = 'ql-video'     // Note: this blot's tag name varies, so while HTML->Parchment ...
    // Video.tagName = 'IFRAME'      // ... transformation we detect DOM elements by class name
    return Video
  }
}
