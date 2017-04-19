var path = require('path')

var pluginUri = 'de.deepamehta.workspaces'
var pluginIdent = '_' + pluginUri.replace(/\./g, '_')

module.exports = {
  entry: './src/main.js',
  output: {
    path: path.resolve(__dirname, 'dist'),
    publicPath: '/' + pluginUri + '/',
    filename: 'plugin.js',
    library: pluginIdent,
    libraryTarget: 'jsonp'
  },
  module: {
    rules: [
      {
        test: /\.vue$/,
        loader: 'vue-loader'
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        exclude: /node_modules/
      }
    ]
  }
}
