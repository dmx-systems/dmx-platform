const HtmlWebpackPlugin = require('html-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const { VueLoaderPlugin } = require('vue-loader')
const { DefinePlugin } = require('webpack')
const path = require('path')

module.exports = env => {

  const webpackConfig = {
    entry: './modules/dmx-webclient/src/main/js/main.js',
    output: {
      path: path.join(__dirname, '/modules/dmx-webclient/target/classes/web'),
      filename: env.WEBPACK_SERVE ? '[name].js' : '[contenthash].[name].js'
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
          use: 'vue-loader'
        },
        {
          test: /\.js$/,
          use: 'babel-loader',
          // Note: dmx-cytoscape-renderer makes use of "?." (JS Optional Chaining operator). quill makes use of "static"
          // class fields. These must go through babel. x(?!y) is Negative Lookahead Assertion regex operator.
          exclude: /node_modules\/(?!(dmx-cytoscape-renderer|quill))/
        },
        {
          test: /\.css$/,
          use: [env.WEBPACK_SERVE ? 'style-loader' : MiniCssExtractPlugin.loader, 'css-loader']
        },
        {
          test: /\.(png|jpg|jpeg|gif|eot|ttf|woff|woff2|svg|svgz)(\?.+)?$/,
          type: 'asset/resource'
        }
      ]
    },
    plugins: [
      new HtmlWebpackPlugin({
        template: 'modules/dmx-webclient/src/main/resources-build/index.html',
        favicon:  'modules/dmx-webclient/src/main/resources-build/favicon.png'
      }),
      new MiniCssExtractPlugin({
        filename: env.WEBPACK_SERVE ? '[name].css' : '[contenthash].[name].css'
      }),
      new VueLoaderPlugin(),
      new DefinePlugin({
        DEV: env.WEBPACK_SERVE
      })
    ],
    stats: {
      assets: false,
      modules: false
    },
    performance: {
      hints: false
    }
  }

  if (env.WEBPACK_SERVE) {
    webpackConfig.devServer = {
      port: 8082,
      proxy: [{
        context: '/',
        target: 'http://localhost:8080'
      }],
      open: true
    }
  }

  return webpackConfig
}
