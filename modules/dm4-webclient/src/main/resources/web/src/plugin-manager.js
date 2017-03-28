var plugins = [
  '/de.deepamehta.workspaces/js/manifest.js',
  '/de.deepamehta.workspaces/js/app.js'
]

plugins.forEach(url => loadScript(url))

function loadScript (url) {
  console.log('Loading script', url)
  var script = document.createElement('script')
  script.src = url
  document.head.appendChild(script)
}
