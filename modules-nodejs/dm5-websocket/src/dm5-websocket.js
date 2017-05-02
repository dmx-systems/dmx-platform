import http from 'axios'

var config = http.get('/websockets')

/**
 * A constructor for a WebSocket connection.
 *
 * The URL to connect to is determined automatically, based on the server-side `dm4.websockets.url` config property.
 * The created WebSocket auto-reconnects once timed out by the browser (usually every 5 minutes).
 * WebSocket messages are expected to be JSON. Serialization/Deserialization performs automatically.
 *
 * @param   pluginUri
 *              the URI of the calling plugin.
 * @param   messageProcessor
 *              the function that processes incoming messages.
 *              One argument is passed: the message pushed by the server (a deserialzed JSON object).
 *
 * @return  The created WebSocket object, wrapped as a DM5 proprietary object.
 *          This object provides a "send_message" function which takes 1 argument: the message to be
 *          sent to the server. The argument will be automatically serialized as a JSON object.
 */
export default function (pluginUri, messageProcessor) {

  var url, ws

  config.then(response => {
    url = response.data['dm4.websockets.url']
    console.log('[DM5] CONFIG: the WebSocket server is reachable at', url)
    setupWebsocket()
  })

  this.send_message = function (message) {
    ws.send(JSON.stringify(message))
  }

  function setupWebsocket () {
    ws = new window.WebSocket(url, pluginUri)

    ws.onopen = function (e) {
      console.log('[DM5] Opening WebSocket connection to', e.target.url)
    }

    ws.onmessage = function (e) {
      var message = JSON.parse(e.data)
      console.log('[DM5] Message received', message)
      messageProcessor(message)
    }

    ws.onclose = function (e) {
      console.log('[DM5] Closing WebSocket connection to', e.target.url, e.reason)
      console.log('[DM5] Reopening ...')
      setTimeout(setupWebsocket, 1000)
    }
  }
}
