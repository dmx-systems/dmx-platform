import store from '../store/webclient'

export default {

  name: 'topic-link',     // Same as blotName. Used in module.addHandler() calls to register toolbarHandler()

  extension (Quill) {

    class TopicLink extends Quill.import('formats/link') {    // Note: 'formats/link' extends 'blots/inline'

      // Creates a DOM node corresponding to the given (model) values
      static create (value) {
        // console.log('TopicLink create()', value)
        const node = super.create()
        node.removeAttribute('target')    // target attribute was added by Link class
        node.setAttribute('href', '')     // href attribute required to render in link style
        node.dataset.topicId = value.topicId
        node.dataset.linkId  = value.linkId
        return node
      }

      // Returns the (model) value represented by the given DOM node
      static formats (node) {
        // console.log('TopicLink formats()', node)
        return {
          topicId: node.dataset.topicId,  // FIXME: convert to Number?
          linkId:  node.dataset.linkId    // FIXME: convert to Number?
        }
      }
    }
    TopicLink.blotName = 'topic-link'     // Used in "toolbar" config, quill.format() and module.addHandler() calls.
                                          // Will be prefixed by "ql-" to form the CSS class name for the toolbar
                                          // button.
    // TopicLink.tagName = 'A'            // not needed as it is derived
    TopicLink.className = 'topic-link'    // CSS class to be added to the <a> element
    return TopicLink
  },

  toolbarHandler (value) {
    // "value" is the button of/off state
    // "this" refers to the Quill toolbar instance
    console.log('topicLinkHandler', value)
    store.dispatch('openSearchWidget', {
      pos: {
        model:  {x: 100, y: 100},   // TODO
        render: {x: 100, y: 100}
      },
      options: {
        noSelect: true,
        topicHandler: topic => {
          console.log('createTopicLink', topic)
          this.quill.format('topic-link', {
            topicId: topic.id,
            linkId: undefined   // TODO
          })
        }
      }
    })
  },

  quillReady (quill) {
    // FIXME: handle multiple Quill instances
    const button = document.querySelector('button.ql-topic-link')
    button.textContent = 'T'    // TODO: use real (SVG) icon
    button.setAttribute('title', 'Insert topic link')
  },

  infoDOMReady (dom) {
    // FIXME: event handlers are registered twice, through updated()
    dom.querySelectorAll('a.topic-link').forEach(link => {
      link.addEventListener('click', e => {
        e.preventDefault()    // suppress browser's default link click behavior
        e.stopPropagation()   // prevent activating inline edit
        const topicId = Number(e.target.dataset.topicId)
        console.log('topic link clicked', topicId)
        store.dispatch('revealTopicById', topicId)
      })
    })
  }
}
