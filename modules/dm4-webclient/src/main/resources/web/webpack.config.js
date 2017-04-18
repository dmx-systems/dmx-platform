var webpack = require('webpack')
var HtmlWebpackPlugin = require('html-webpack-plugin')
var path = require('path')

module.exports = (env = {}) => {

  console.log("environ", env, process.env.NODE_ENV)

  var webpackConfig = {
    entry: './src/main.js',
    output: {
      path: path.resolve(__dirname, 'dist'),
      filename: 'build.js'
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
    },
    plugins: [
      new webpack.DefinePlugin({
        DEV: env.dev,
      }),
      new HtmlWebpackPlugin({
        template: 'src/index.html'
      })
    ]
  }

  if (env.dev) {
    webpackConfig.devServer = {
      port: 8082,
      open: true,
      proxy: [
        {
          context: ['/core'],
          target: 'http://localhost:8080'
        }
      ]
    }
  }

  return webpackConfig
}
