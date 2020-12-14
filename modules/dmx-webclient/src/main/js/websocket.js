import DMXWebSocket from 'dmx-websocket'
import store from './store/webclient'

/* eslint no-new: 0 */

new DMXWebSocket('systems.dmx.webclient', message => {
  store.dispatch('_' + message.type, message.args)
})
