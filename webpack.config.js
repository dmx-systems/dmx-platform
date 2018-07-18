const path = require('path')
const { VueLoaderPlugin } = require('vue-loader')
const CleanWebpackPlugin = require('clean-webpack-plugin')
const UglifyJsPlugin = require('uglifyjs-webpack-plugin')

module.exports = {
  entry: './modules/dm4-webclient/src/main/js/main.js',
  output: {
    filename: '[name].bundle.js',
    path: path.resolve(__dirname, 'modules/dm4-webclient/src/main/resources/web/dist'),
    publicPath: 'dist/'
  },
  resolve: {
    extensions: [".js", ".vue"],
    alias: {
      'modules': path.resolve(__dirname, 'modules')     // needed by plugin-manager.js
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
        exclude: /node_modules/
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
    new VueLoaderPlugin(),
    new CleanWebpackPlugin(['modules/dm4-webclient/src/main/resources/web/dist'])
  ],
  devServer: {
    port: 8082,
    proxy: {
      '/': {
        target: 'http://localhost:8080',
        bypass: req => {
          if (req.url === '/') {
            return 'modules/dm4-webclient/src/main/resources/web/'
          }
        }
      }
    },
    noInfo: true,
    open: true
  },
  performance: {
    hints: false
  }
}
