var path = require('path')
var webpack = require('webpack')
var HtmlWebpackPlugin = require('html-webpack-plugin')

module.exports = (env = {}) => {

  var webpackConfig = {
    entry: './modules/dm4-webclient/src/main/js/main.js',
    output: {
      filename: 'webclient.js',
      path: path.resolve(__dirname, 'modules/dm4-webclient/src/main/resources/web/')
    },
    resolve: {
      extensions: [".js", ".vue"],
      alias: {
        'modules': path.resolve(__dirname, 'modules')
      }
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
          exclude: /node_modules\/(?!dm5)/
          // Note: at the moment the dm5 modules are distributed not in pre-compiled form.
          // For the production build we must include them in babel processing as UglifyJs accpets no ES6.
          // Note: regex x(?!y) matches x only if x is not followed by y. Only x is part of the match results.
          // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp
          // TODO: distribute the dm5 modules in pre-compiled form.
        },
        {
          test: /\.css$/,
          loader: ['style-loader', 'css-loader']
        },
        {
          test: /\.(png|jpg|jpeg|gif|eot|ttf|woff|woff2|svg|svgz)(\?.+)?$/,
          loader: 'file-loader'
        }
      ]
    },
    plugins: [
      new webpack.DefinePlugin({
        DEV: env.dev,
      }),
      new HtmlWebpackPlugin({
        template: 'modules/dm4-webclient/src/main/js/index.html'
      })
    ]
  }

  if (env.dev) {
    webpackConfig.devServer = {
      port: 8082,
      proxy: {
        '/': 'http://localhost:8080'
      },
      noInfo: true,
      open: true
    }
  }

  return webpackConfig
}
