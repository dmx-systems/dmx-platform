const HtmlWebpackPlugin = require('html-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const {VueLoaderPlugin} = require('vue-loader')
const {DefinePlugin} = require('webpack')
const path = require('path')

module.exports = (env = {}) => {

  const webpackConfig = {
    entry: './modules/dmx-webclient/src/main/js/main.js',
    output: {
      path: path.join(__dirname, '/modules/dmx-webclient/target/classes/web'),
      filename: env.dev ? '[name].js' : '[chunkhash].[name].js'
    },
    resolve: {
      extensions: ['.js', '.vue'],
      alias: {
        // used by plugin-manager.js
        modules:            path.join(__dirname, '/modules'),
        'modules-external': path.join(__dirname, '/modules-external')
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
          // Note: dmx-cytoscape-renderer makes use of ?. Conditional Chaining JS operator and needs to go through babel
          exclude: /node_modules\/(?!dmx-cytoscape-renderer)/   // x(?!y) is Negative Lookahead Assertion regex operator
        },
        {
          test: /\.css$/,
          loader: [env.dev ? 'style-loader' : MiniCssExtractPlugin.loader, 'css-loader']
        },
        {
          test: /\.(png|jpg|jpeg|gif|eot|ttf|woff|woff2|svg|svgz)(\?.+)?$/,
          loader: 'file-loader',
          options: {
            esModule: false   // Note: since file-loader 5.0 "esModule" is true by default.
          }                   // Does not work with <img src"..."> element in vue template.
        }
      ]
    },
    plugins: [
      new HtmlWebpackPlugin({
        template: 'modules/dmx-webclient/src/main/resources-build/index.html',
        favicon:  'modules/dmx-webclient/src/main/resources-build/favicon.png'
      }),
      new MiniCssExtractPlugin({
        filename: env.dev ? '[name].css' : '[contenthash].[name].css'
      }),
      new VueLoaderPlugin(),
      new DefinePlugin({
        DEV: env.dev
      })
    ],
    stats: {
      entrypoints: false,
      children: false,
      assetsSort: 'chunks'
    },
    performance: {
      hints: false
    }
  }

  if (env.dev) {
    webpackConfig.devServer = {
      port: 8082,
      proxy: {'/': 'http://localhost:8080'},
      noInfo: true,
      open: true
    }
  }

  return webpackConfig
}
