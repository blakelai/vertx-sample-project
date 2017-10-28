'use strict'
const path = require('path')
const utils = require('./utils')
const config = require('../config/index')
const vueLoaderConfig = require('./vue-loader.conf')
const WebpackCdnPlugin = require('webpack-cdn-plugin');

function resolve (dir) {
  return path.join(__dirname, '..', dir)
}

module.exports = {
  entry: {
    app: './src/main.js'
  },
  output: {
    path: config.build.assetsRoot,
    filename: '[name].js',
    publicPath: process.env.NODE_ENV === 'production'
      ? config.build.assetsPublicPath
      : config.dev.assetsPublicPath
  },
  resolve: {
    extensions: ['.js', '.vue', '.json'],
    alias: {
      'vue$': 'vue/dist/vue.esm.js',
      '@': resolve('src'),
    }
  },
  module: {
    rules: [
      {
        test: /\.(js|vue)$/,
        loader: 'eslint-loader',
        enforce: 'pre',
        include: [resolve('src'), resolve('test')],
        options: {
          formatter: require('eslint-friendly-formatter')
        }
      },
      {
        test: /\.vue$/,
        loader: 'vue-loader',
        options: vueLoaderConfig
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        include: [resolve('src'), resolve('test')]
      },
      {
        test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: utils.assetsPath('img/[name].[hash:7].[ext]')
        }
      },
      {
        test: /\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: utils.assetsPath('media/[name].[hash:7].[ext]')
        }
      },
      {
        test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: utils.assetsPath('fonts/[name].[hash:7].[ext]')
        }
      }
    ]
  },
  plugins: [
    new WebpackCdnPlugin({
      modules: [
        {
          name: 'jquery',
          var: '$',
          path: 'jquery.slim.min.js'
        },
        {
          name: 'popper.js',
          path: 'umd/popper.min.js'
        },
        {
          name: 'bootstrap',
          cdn: 'twitter-bootstrap',
          style: 'css/bootstrap.css',
          path: 'js/bootstrap.min.js'
        },
        {
          name: 'font-awesome',
          style: 'css/font-awesome.css',
          cssOnly: true
        },
        {
          name: 'axios',
          path: 'axios.min.js'
        },
        {
          name: 'lodash',
          cdn: 'lodash.js',
          var: '_',
          path: 'lodash.min.js'
        },
        {
          name: 'sockjs-client',
          path: 'sockjs.min.js'
        },
        {
          name: 'vertx3-eventbus-client',
          cdn: 'vertx',
          var: 'EventBus',
          path: 'vertx-eventbus.min.js'
        }
      ],
      prodUrl: '//cdnjs.cloudflare.com/ajax/libs/:name/:version/:path',
      publicPath: '/node_modules'
    })
  ]
}
