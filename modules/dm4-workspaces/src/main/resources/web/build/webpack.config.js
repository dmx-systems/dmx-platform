var
  path = require('path'),
  webpack = require('webpack'),
  config = require('../config'),
  cssUtils = require('./css-utils'),
  merge = require('webpack-merge'),
  projectRoot = path.resolve(__dirname, '../'),
  ProgressBarPlugin = require('progress-bar-webpack-plugin'),
  ExtractTextPlugin = require('extract-text-webpack-plugin'),
  HtmlWebpackPlugin = require('html-webpack-plugin'),
  useCssSourceMap = false,
  pluginUri = config.pluginUri,
  pluginIdent = '_' + pluginUri.replace(/\./g, '_'),
  quasarTheme = 'mat'   // 'ios' or 'mat'

function resolve (dir) {
  return path.join(__dirname, '..', dir)
}

var webpackConfig = {
  entry: './src/main.js',
  output: {
    path: path.resolve(__dirname, '../dist'),
    publicPath: '/' + pluginUri + '/',
    filename: 'js/[name].js',
    chunkFilename: 'js/[id].[chunkhash].js',
    library: pluginIdent,
    libraryTarget: 'jsonp',
    jsonpFunction: "webpackJsonp" + pluginIdent
  },
  resolve: {
    extensions: ['.js', '.vue', '.json'],
    modules: [
      resolve('src'),
      resolve('node_modules')
    ],
    alias: {
      quasar: path.resolve(__dirname, '../node_modules/quasar-framework/'),
      src: path.resolve(__dirname, '../src'),
      assets: path.resolve(__dirname, '../src/assets'),
      components: path.resolve(__dirname, '../src/components')
    }
  },
  module: {
    rules: [
      { // eslint
        enforce: 'pre',
        test: /\.(vue|js)$/,
        loader: 'eslint-loader',
        include: projectRoot,
        exclude: /node_modules/,
        options: {
          formatter: require('eslint-friendly-formatter')
        }
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        include: projectRoot,
        exclude: /node_modules/
      },
      {
        test: /\.vue$/,
        loader: 'vue-loader',
        options: {
          postcss: cssUtils.postcss,
          loaders: merge({js: 'babel-loader'}, cssUtils.styleLoaders({
            sourceMap: useCssSourceMap,
            extract: true
          }))
        }
      },
      {
        test: /\.json$/,
        loader: 'json-loader'
      },
      {
        test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: 'img/[name].[hash:7].[ext]'
        }
      },
      {
        test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: 'fonts/[name].[hash:7].[ext]'
        }
      }
    ]
  },
  plugins: [
    /*
      Take note!
      Uncomment if you wish to load only one Moment locale:

      new webpack.ContextReplacementPlugin(/moment[\/\\]locale$/, /en/),
    */

    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: '"production"'
      },
      'DEV': false,
      'PROD': true,
      '__THEME': '"' + quasarTheme + '"'
    }),
    new webpack.LoaderOptionsPlugin({
      minimize: true,
      options: {
        context: path.resolve(__dirname, '../src'),
        postcss: cssUtils.postcss
      }
    }),
    new ProgressBarPlugin({
      // Progress Bar Webpack plugin format
      // https://github.com/clessg/progress-bar-webpack-plugin#options
      format: ' [:bar] ' + ':percent'.bold + ' (:msg)'
    }),
    new webpack.optimize.UglifyJsPlugin({
      sourceMap: false,
      minimize: true,
      compress: {
        warnings: false
      }
    }),
    // extract css into its own file
    new ExtractTextPlugin({
      filename: '[name].[contenthash].css'
    }),
    new HtmlWebpackPlugin({
      filename: path.resolve(__dirname, '../dist/index.html'),
      template: 'src/index.html',
      inject: true,
      minify: {
        removeComments: true,
        collapseWhitespace: true,
        removeAttributeQuotes: true
        // more options:
        // https://github.com/kangax/html-minifier#options-quick-reference
      },
      // necessary to consistently work with multiple chunks via CommonsChunkPlugin
      chunksSortMode: 'dependency'
    }),
    // split vendor js into its own file
    new webpack.optimize.CommonsChunkPlugin({
      name: 'vendor',
      minChunks: function (module, count) {
        // any required modules inside node_modules are extracted to vendor
        return (
          module.resource &&
          /\.js$/.test(module.resource) &&
          module.resource.indexOf(
            path.join(__dirname, '../node_modules')
          ) === 0
        )
      }
    }),
    // extract webpack runtime and module manifest to its own file in order to
    // prevent vendor hash from being updated whenever app bundle is updated
    new webpack.optimize.CommonsChunkPlugin({
      name: 'manifest',
      chunks: ['vendor']
    })
  ],
  devtool: false,
  performance: {
    hints: false
  }
}

webpackConfig.module.rules = webpackConfig.module.rules.concat(cssUtils.styleRules({
  sourceMap: false,
  extract: true,
  postcss: true
}))

module.exports = webpackConfig
