<template>
  <div>
    <div class="row">

      <div class="col-md-12">
    <span class="dropdown">
      <button class="btn btn-secondary dropdown-toggle" type="button" id="pageDropdownButton" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
        <i class="fa fa-file-text" aria-hidden="true"></i> Pages
      </button>
      <div class="dropdown-menu" aria-labelledby="pageDropdownButton">
        <a v-for="page in pages" :key="page.id" class="dropdown-item" v-on:click="load(page.id)"
           href="#">{{page.name}}</a>
      </div>
    </span>
        <span>
      <button type="button" class="btn btn-secondary" v-on:click="reload()"><i class="fa fa-refresh"
                                                                               aria-hidden="true"></i> Reload</button>
    </span>
        <span>
      <button type="button" class="btn btn-secondary" v-on:click="newPage()"><i class="fa fa-plus-square"
                                                                                aria-hidden="true"></i> New page</button>
    </span>
        <span class="float-right">
      <button type="button" class="btn btn-secondary" v-on:click="deletePage()" v-show="pageExists()"><i
        class="fa fa-trash"
        aria-hidden="true"></i> Delete page</button>
    </span>
      </div>

      <div class="col-md-12">
        <div class="invisible alert" role="alert" id="alertMessage">
          {{alertMessage}}
        </div>
      </div>

      <div class="col-md-12">
        <div class="alert alert-warning" v-bind:class="{ invisible: !pageModified }" role="alert">
          The page has been modified by another user.
          <a href="#" v-on:click="load(pageId)">Reload</a>
        </div>
      </div>

    </div>
    <div class="row">

      <div class="col-md-6" id="rendering"></div>

      <div class="col-md-6">
        <form>
          <div class="form-group">
            <label for="markdown">Markdown</label>
            <textarea id="markdown" class="form-control" rows="25" v-model="pageMarkdown"></textarea>
          </div>
          <div class="form-group">
            <label for="pageName">Name</label>
            <input class="form-control" type="text" value="" id="pageName" v-model="pageName"
                   :disabled="pageExists()">
          </div>
          <button type="button" class="btn btn-secondary" v-on:click="savePage()"><i class="fa fa-pencil"
                                                                                   aria-hidden="true"></i> Save
          </button>
        </form>
      </div>

    </div>
  </div>
</template>

<script>
  import axios from 'axios'
  import _ from 'lodash'
  import EventBus from 'vertx3-eventbus-client'

  function generateUUID () {
    var d = new Date().getTime()
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      var r = (d + Math.random() * 16) % 16 | 0
      d = Math.floor(d / 16)
      return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16)
    })
  }

  var DEFAULT_PAGENAME = 'Example page'
  var DEFAULT_MARKDOWN = '# Example page\n\nSome text _here_.\n'

  var eb = new EventBus(window.location.protocol + '//' + window.location.host + '/eventbus')
  var clientUuid = generateUUID()

  export default {
    name: 'WikiPage',
    data: function () {
      return {
        pageId: undefined,
        pageName: DEFAULT_PAGENAME,
        pageMarkdown: DEFAULT_MARKDOWN,
        markdownRenderingPromise: null,
        pageModified: false,
        pages: [],
        alertMessage: ''
      }
    },
    methods: {
      newPage: function () {
        this.pageId = undefined
        this.pageName = DEFAULT_PAGENAME
        this.pageMarkdown = DEFAULT_MARKDOWN
      },
      reload: function () {
        var self = this
        axios.get('/api/pages')
          .then(function (response) {
            self.pages = response.data.pages
          })
      },
      pageExists: function () {
        return this.pageId !== undefined
      },
      load: function (id) {
        this.pageModified = false
        var self = this
        axios.get('/api/pages/' + id)
          .then(function (response) {
            var page = response.data.page
            self.pageId = page.id
            self.pageName = page.name
            self.pageMarkdown = page.markdown
            self.updateRendering(page.html)
          })
      },
      updateRendering: function (html) {
        document.getElementById('rendering').innerHTML = html
      },
      savePage: function () {
        var self = this
        var payload
        if (this.pageId === undefined) {
          payload = {
            'name': this.pageName,
            'markdown': this.pageMarkdown
          }
          axios.post('/api/pages', payload)
            .then(function (ok) {
              self.reload()
              self.success('Page created')
              var guessMaxId = _.maxBy(self.pages, function (page) { return page.id })
              self.load(guessMaxId.id || 0)
            }, function (err) {
              self.error(err.response.data.error)
            })
        } else {
          payload = {
            'client': clientUuid,
            'markdown': this.pageMarkdown
          }
          axios.put('/api/pages/' + this.pageId, payload)
            .then(function (ok) {
              self.success('Page saved')
            }, function (err) {
              self.error(err.response.data.error)
            })
        }
      },
      deletePage: function () {
        var self = this
        axios.delete('/api/pages/' + this.pageId)
          .then(function (ok) {
            self.reload()
            self.newPage()
            self.success('Page deleted')
          }, function (err) {
            self.error(err.response.data.error)
          })
      },
      success: function (message) {
        this.alertMessage = message
        var alert = document.getElementById('alertMessage')
        alert.classList.add('alert-success')
        alert.classList.remove('invisible')
        window.setTimeout(function () {
          alert.classList.add('invisible')
          alert.classList.remove('alert-success')
        }, 3000)
      },
      error: function (message) {
        this.alertMessage = message
        var alert = document.getElementById('alertMessage')
        alert.classList.add('alert-danger')
        alert.classList.remove('invisible')
        window.setTimeout(function () {
          alert.classList.add('invisible')
          alert.classList.remove('alert-danger')
        }, 5000)
      }
    },
    watch: {
      pageMarkdown: function (text) {
        if (eb.state !== EventBus.OPEN) return
        if (this.markdownRenderingPromise !== null) {
          clearTimeout(this.markdownRenderingPromise)
        }

        var self = this
        this.markdownRenderingPromise = setTimeout(function () {
          console.log('pageMarkdown has been modified')
          self.markdownRenderingPromise = null
          eb.send('app.markdown', text, function (err, reply) {
            if (err === null) {
              self.updateRendering(reply.body)
            } else {
              console.warn('Error rendering Markdown content: ' + JSON.stringify(err))
            }
          })
        }, 300)
      }
    },
    mounted: function () {
      this.reload()

      var self = this
      eb.onopen = function () {
        eb.registerHandler('page.saved', function (error, message) {
          if (error) {
            console.log(error.toString())
            return
          }

          if (message.body &&
            self.pageId === message.body.id &&
            clientUuid !== message.body.client) {
            self.pageModified = true
          }
        })
      }
    }
  }
</script>
