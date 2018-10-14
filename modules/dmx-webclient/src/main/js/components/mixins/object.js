import dm5 from 'dm5'

export default {
  props: {
    // the Topic/Assoc to render; is never undefined;
    // may be an "empty" topic/assoc, without ID, with just type set
    object: {
      type: dm5.DeepaMehtaObject,
      required: true
    }
  }
}
