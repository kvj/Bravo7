var oauthClientID =     'fb23dcdaff40f2bb';
var oauthClientSecret = '9ea2f5f5bda2f8a6';
var placesAPI = 'AIzaSyDPf3mJRVrn6b4DRnjASJXnAiCcp50vI64';
var defaultLocation = {lat: 35.6811, lon: 139.7673};

var gMapsPlaceSearch = function(latLon, radius, handler) {//
    var url = 'https://maps.googleapis.com/maps/api/place/search/json?location='+latLon.lat()+','+latLon.lng()+'&radius='+radius+'&sensor=true&key='+placesAPI;
    Ext.Ajax.request({
        url: url,
        method: 'GET',
        success: function(response) {//Got it
            var result = Ext.JSON.decode(response.responseText, true);
            if (result) {
                if (result.status != 'OK') {
                    return handler(result.status || 'Google API error');
                };
                return handler(null, result);
            };
            return handler('Remote side error');
        },
        failure: function() {
            handler('HTTP error');
        },
    });
};

//var gMapsLatLonSearch = function(place, handler) {//
    //var url = 'https://maps.googleapis.com/maps/api/place/search/json?location='+latLon.lat()+','+latLon.lng()+'&radius='+radius+'&sensor=true&key='+placesAPI;
    //Ext.Ajax.request({
        //url: url,
        //method: 'GET',
        //success: function(response) {//Got it
            //var result = Ext.JSON.decode(response.responseText, true);
            //if (result) {
                //if (result.status != 'OK') {
                    //return handler(result.status || 'Google API error');
                //};
                //return handler(null, result);
            //};
            //return handler('Remote side error');
        //},
        //failure: function() {
            //handler('HTTP error');
        //},
    //});
//};

var oauthGetLoginURL = function(loginURL, clientID) {//Constructs OAuth2 login URL
    return loginURL+'?type=user_agent&client_id='+clientID;
};

var oauthGetAccessToken = function(code, tokenURL, clientID, clientSecret, handler) {//
    var url = tokenURL+'?code='+code+'&client_id='+clientID+'&client_secret='+clientSecret;
    Ext.Ajax.request({
        url: url,
        method: 'POST',
        success: function(response) {//Got it
            var obj = Ext.Object.fromQueryString(response.responseText);
            if (obj && obj.access_token) {//
                return handler(null, obj.access_token);
            };
            return handler('Code provided is invalid');
        },
        failure: function() {
            handler('HTTP error');
        },
    });
};

var oauthGetAccessTokenUsername = function(loginURL, clientID, clientSecret, username, password, handler) {//
    var url = loginURL+'?username='+username+'&password='+password+'&type=username&client_id='+clientID+'&client_secret='+clientSecret;
    console.log('url', url);
    Ext.Ajax.request({
        url: url,
        method: 'POST',
        success: function(response) {//Got it
            var obj = Ext.Object.fromQueryString(response.responseText);
            if (obj && obj.access_token) {//
                return handler(null, obj.access_token);
            };
            return handler('Invalid username/password');
        },
        failure: function() {
            handler('HTTP error');
        },
    });
};

var DataManager = function(url, access_token) {//
    this.url = url;
    this.token = access_token;
    this.networkHandler = function() {};
};

DataManager.prototype._request = function(path, data, method, handler) {//
    var url = this.url+path+'?access_token='+this.token;
    this.networkHandler(true);
    Ext.Ajax.request({
        url: url,
        method: method || 'GET',
        params: data,
        success: _.bind(function(response) {//Got it
            this.networkHandler(false);
            var result = Ext.JSON.decode(response.responseText, true);
            if (result) {
                if (result.error) {
                    return handler(result.error || ('Unknown error of '+path));
                };
                return handler(null, result);
            };
            return handler('Remote side error');
        }, this),
        failure: _.bind(function() {
            this.networkHandler(false);
            handler('HTTP error');
        }, this),
    });
};

DataManager.prototype.ping = function(handler) {//
    this._request('/ping', null, 'GET', handler);
};

DataManager.prototype.updateTemplate = function(tmpl, handler) {//Inser/update template
    this._request('/template', tmpl, 'POST', handler);
};

DataManager.prototype.getTemplates = function(params, handler, context) {//Inser/update template
    this._request('/templates', params, 'POST', function(err, obj) {
        if (err) {
            return handler(err);
        };
        handler(null, obj.array || [], context);
    });
};

DataManager.prototype.removeTemplate = function(tmpl, handler) {//Inser/update template
    this._request('/template', tmpl, 'DELETE', handler);
};

DataManager.prototype.checkin = function(tmplid, data, handler) {//Creates new checkin
    var params = data || {};
    params.template = tmplid;
    this._request('/checkin', params, 'POST', handler);
};

DataManager.prototype.upload = function(checkin, form, handler) {//Uploads file
    log('Submitting', checkin);
    form.submit({
        url: this.url+'/upload'+'?access_token='+this.token,
        method: 'POST',
        params: {
            checkin: checkin,
        },
        success: function() {
            log('Upload OK');
            handler(null);
        },
        failure: function() {
            log('Upload ERR');
            handler('Upload failed');
        },
    })
};

DataManager.prototype.updateCheckin = function(data, handler) {//Creates new checkin
    this._request('/checkin', data, 'POST', handler);
};

DataManager.prototype.subscribe = function(ids, handler) {//Creates new checkin
    this._request('/subscribe', {type: 'web', ids: ids}, 'POST', handler);
};

DataManager.prototype.removeCheckin = function(checkin, handler) {//Removes checkin
    this._request('/checkin', checkin, 'DELETE', handler);
};

DataManager.prototype.getCheckins = function(params, handler, context) {//Select checkins
    this._request('/checkins', params || {}, 'POST', function(err, obj) {
        if (err) {
            return handler(err);
        };
        handler(null, obj.array || [], context);
    });
};

DataManager.prototype.addRef = function(checkin_id, id, handler) {//Add ref
    this._request('/refs', {checkin: checkin_id, ids: id}, 'POST', handler);
};

DataManager.prototype.removeRef = function(checkin_id, id, handler) {//Add ref
    this._request('/refs', {checkin: checkin_id, ids: id}, 'DELETE', handler);
};

DataManager.prototype.getHooks = function(params, handler, context) {//Select checkins
    this._request('/hooks', params || {}, 'POST', function(err, obj) {
        if (err) {
            return handler(err);
        };
        handler(null, obj.array || [], context);
    });
};

DataManager.prototype.updateHook = function(data, handler) {//Creates new checkin
    this._request('/hook', data, 'POST', handler);
};

DataManager.prototype.removeHook = function(id, handler) {//Removes checkin
    this._request('/hook', {id: id}, 'DELETE', handler);
};

var testDataManager = function(url, token) {
    module('DataManager');
    asyncTest('API test', function() {
        console.log('Testing...');
        ok(url);
        ok(token);
        var api = new DataManager(url, token);
        api.ping(function(err, user) {
            ok(user);
            ok(user.login, user.login);
            testTmpls();
        });
        var testTmpls = function() {//
            api.updateTemplate({name: 'Test tmpl#1', tags: 'tag1 tag2', template: '{"type": "task", "text": "", "location": {"name": "Office"}}'}, function(err, tmpl) {
                ok(tmpl, err);
                if (!tmpl) {
                    return start();
                };
                ok(tmpl.id);
                equal(tmpl.name, 'Test tmpl#1');
                ok(tmpl.tags);
                if (!tmpl.tags) {
                    return start();
                };
                equal(tmpl.tags.length, 2);
                api.getTemplates({}, function(err, list) {
                    ok(list, err);
                    if (!list) {
                        return start();
                    };
                    ok(list.length>0);
                    var id = null;
                    for (var i = 0; i < list.length; i++) {
                        if (tmpl.id == list[i].id) {
                            id = tmpl.id;
                        };
                    };
                    equal(id, tmpl.id);
                    testCheckin(tmpl.id);
                });
            })
        };
        var testCheckin = function(tmplid) {//Create checkin based on template id
            api.checkin(tmplid, {text: 'Task text', other: 'xxx', created: new Date().getTime()}, function(err, ch) {//
                ok(ch, err);
                if (!ch) {
                    return start();
                };
                equal(ch.template, tmplid);
                ok(ch.id);
                equal(ch.text, 'Task text');
                equal(ch.type, 'task');
                ok(!ch.other);
                ok(ch.created);
                ok(ch.location);
                if (ch.location) {
                    equal(ch.location.name, 'Office');
                };
                api.updateCheckin({id: ch.id, template: ch.template, text: 'Task text2'}, function(err, ch2) {
                    ok(ch2, err);
                    if (!ch2) {
                        return start();
                    };
                    equal(ch.id, ch2.id);
                    equal(ch.template, ch2.template);
                    equal(ch2.type, 'task');
                    equal(ch2.text, 'Task text2');
                    testRefs(tmplid, ch.id);
                });

            });
        };
        var testRefs = function(tmplid, id) {//

            api.checkin(tmplid, {created: new Date().getTime()}, function(err, ch1) {//
                ok(ch1, err);
                api.checkin(tmplid, {created: new Date().getTime()}, function(err, ch2) {//
                    ok(ch2, err);
                    api.getCheckins({ids: [id, ch1.id, ch2.id]}, function(err, arr) {//
                        ok(arr, err);
                        equal(arr.length, 3, 'Array len is 3');
                        api.addRef(id, ch1.id, function(err) {//
                            ok(!err, err);
                            api.addRef(id, ch2.id, function(err) {//
                                ok(!err, err);
                                api.getCheckins({ids: [id]}, function(err, arr) {//
                                    ok(arr, err);
                                    if (!arr) {
                                        return start();
                                    };
                                    equal(arr.length, 1);
                                    if (!arr[0] || !arr[0].refs) {
                                        return start();
                                    };
                                    equal(arr[0].refs.length, 2);
                                    api.addRef(id, ch1.id, function(err) {
                                        ok(!err, err);
                                        api.getCheckins({ids: [id]}, function(err, arr) {//
                                            ok(arr, err);
                                            equal(arr.length, 1, 'One element');
                                            equal(arr[0].refs.length, 2, '2 refs');
                                            api.getCheckins({ids: arr[0].refs}, function(err, arr) {
                                                ok(arr, err);
                                                equal(arr.length, 2);
                                                api.removeRef(id, ch1.id, function(err) {
                                                    ok(!err, err);
                                                    api.getCheckins({ids: [id]}, function(err, arr) {//
                                                        ok(arr, err);
                                                        equal(arr.length, 1, 'One element');
                                                        equal(arr[0].refs.length, 1, 'Only one ref left');
                                                        api.removeCheckin({id: id}, function(err) {
                                                            ok(!err, err);
                                                            api.removeCheckin(ch1, function(err) {
                                                                ok(!err, err);
                                                                api.removeTemplate(ch2, function(err) {
                                                                    ok(!err, err);
                                                                    api.removeTemplate({id: tmplid}, function(err) {
                                                                        ok(!err, err);
                                                                        start();
                                                                    });
                                                                });
                                                            });
                                                        });
                                                    })
                                                })
                                            })
                                        })
                                    })
                                })
                            })
                        })
                    });
                });
            });
        };
    });
};

var iconRegistry = {
blue : [
'b-home',
'b-shoppingbasket',
'b-package',
'b-clock',
'b-appointment',
'b-contacts',
'b-notebook',
'b-callout',
'b-im',
'b-search',
'b-ribbon',
'b-shield',
'b-gps',
'b-location',
'b-pushpin',
'b-flag',
'b-sharethis',
'b-globeeua',
'b-cut',
'b-approvebutton',
'b-cube',
'b-folder',
'b-infocirclealt',
'b-snowflake',
'b-weather',
'b-chargingbattery',
'b-heartalt',
],
gray : [
'gr-creditcard',
'gr-deliverytruck',
'gr-email',
'gr-suitcase',
'gr-sms',
'gr-settings',
'gr-businesscard',
'gr-business',
'gr-government',
'gr-money',
'gr-stacks',
'gr-map',
'gr-binoculars',
'gr-paperclip',
'gr-monitor',
'gr-smartphone',
'gr-revert',
'gr-cycle',
'gr-pencil',
'gr-checkbox',
'gr-checkcirclealt',
'gr-presentation',
'gr-flowchart',
'gr-commandline',
'gr-umbrella',
'gr-halfbattery',
'gr-controller',
'gr-dollar',
'gr-euro',
'gr-yen',
'gr-wand',
'gr-watch',
],
green : [
'g-home',
'g-clock',
'g-thumbsup',
'g-calendar',
'g-appointment',
'g-email',
'g-emailopen',
'g-download',
'g-bookmark',
'g-sms',
'g-callout',
'g-im',
'g-wifi',
'g-smiliehappy',
'g-shield',
'g-location',
'g-pushpin',
'g-flag',
'g-runningman',
'g-cut',
'g-check',
'g-checkbox',
'g-checkcircle',
'g-checkcirclealt',
'g-approvebutton',
'g-globe',
'g-tree',
'g-fullbattery',
'g-heartalt',
'g-award',
'g-reading',
],
magenta : [
'm-star',
'm-airplane',
'm-bolt',
'm-film',
'm-videocam',
'm-camera',
'm-document',
'm-sun',
'm-coffeecup',
'm-abc',
'm-pear',
'm-wine',
'm-beer',
],
orange : [
'o-alram',
'o-star',
'o-lightbulb',
'o-bmw',
'o-smiliejustok',
'o-bolt',
'o-warningalt',
'o-forward',
'o-brush',
'o-exit',
'o-pagecurl',
'o-aim',
'o-exclamationcirclealt',
'o-sun',
'o-lock',
'o-emptybattery',
],
red : [
'r-phone',
'r-mobilephone',
'r-thumbsdown',
'r-upload',
'r-callout',
'r-smiliesad',
'r-trash',
'r-shield',
'r-hourglass',
'r-flag',
'r-caution',
'r-forward',
'r-approvebutton',
'r-piechart',
'r-bullseye',
'r-questioncirclealt',
'r-mountains',
'r-highfive',
'r-heartalt',
'r-poison',
],
};

var DataProvider = function() {//Store all connections, refreshes and provides data
    this.iconMap = {};
    var cats = _.keys(iconRegistry);
    for (var i = 0; i < cats.length; i++) {
        var arr = iconRegistry[cats[i]] || [];
        for (var j = 0; j < arr.length; j++) {
            this.iconMap[arr[j]] = cats[i];
        };
    };
};

DataProvider.prototype.buildIcon = function(icon, cat, small) {
    if (!cat) {
        cat = this.iconMap[icon];
    };
    if (cat && icon) {
        if (small) {//
            return 'img/icon20/'+icon+'.png';
        };
        return 'img/icon32/'+icon+'.png';
    };
    return null;
};

DataProvider.prototype.setupSSE = function(conn) {//Start EventSource
    log('Opening SSE:', conn.name);
    var source = new EventSource(conn.url+'/sse?access_token='+conn.conn.token);
    source.addEventListener('message', _.bind(function(e) {//
        //log('Data['+conn.name+']:', e.data);
        var obj = Ext.JSON.decode(e.data, true);
        if (obj && conn.dataHandler) {
            conn.dataHandler(obj);
        };
    }, this), false);
    source.addEventListener('open', _.bind(function(e) {//
        log('SSE['+conn.name+'] opened');
        conn.connected = true;
        if (conn.sessionHandler) {
            conn.sessionHandler(true);
        };
    }, this), false);
    source.addEventListener('error', _.bind(function(e) {//
        console.log('SSE['+conn.name+'] error', e);
        conn.connected = false;
        if (conn.sessionHandler) {
            conn.sessionHandler(false);
        };
    }, this), false);

};

DataProvider.prototype.startConnections = function(arr, handler) {//Pings API
    this.conn = [];
    var gr = new AsyncGrouper(arr.length, _.bind(function(gr) {//Pings done
        var err = gr.findError();
        if (err) {
            this.conn = [];
            return handler(err);
        };
        for (var i = 0; i < this.conn.length; i++) {
            this.setupSSE(this.conn[i]);
        };
        return handler();
    }, this));
    for (var i = 0; i < arr.length; i++) {//Create
        var c = new DataManager(arr[i].url, arr[i].token);
        this.conn.push({conn: c, name: arr[i].name, templates: [], url: arr[i].url});
        c.ping(gr.handler);
    };
};

DataProvider.prototype.size = function() {
    return this.conn? this.conn.length: 0;
};

DataProvider.prototype.get = function(i) {
    return this.conn[i];
};

DataProvider.prototype.loadTemplates = function(handler) {
    var gr = new AsyncGrouper(this.conn.length, _.bind(function(gr) {
        var err = gr.findError();
        if (err) {
            return handler(err);
        };
        this.templates = [];
        this.templateMap = {};
        this.tags = [''];
        for (var i = 0; i < gr.results.length; i++) {
            var arr = gr.results[i][1];
            var conn = gr.results[i][2];
            for (var j = 0; j < arr.length; j++) {
                arr[j]._template = new Ext.Template(arr[j].display || '{text}');
                var entry = {tmpl: arr[j], conn: conn};
                this.templates.push(entry);
                this.templateMap[arr[j].id] = entry;
                var tags = arr[j].tags || [];
                for (var k = 0; k < tags.length; k++) {//
                    if (_.indexOf(this.tags, tags[k]) == -1) {
                        this.tags.push(tags[k]);
                    };
                };
            };
        };
        this.tags.sort();
        this.templates.sort(function(a, b) {
            if (a.tmpl.name == b.tmpl.name) {
                return 0;
            };
            if (a.tmpl.name > b.tmpl.name) {
                return 1;
            };
            return -1;
        });
        return handler(null, this.templates, this.tags);
    }, this));
    for (var i = 0; i < this.conn.length; i++) {//getTemplates
        this.conn[i].conn.getTemplates({}, gr.handler, this.conn[i]);
    };
};

DataProvider.prototype.getTemplatesByTag = function(tag) {//Gets templates by tag
    var result = [];
    for (var i = 0; i < this.templates.length; i++) {//
        var tmpl = this.templates[i];
        if ((!tag && (!tmpl.tmpl.tags || tmpl.tmpl.tags.length == 0)) || (_.indexOf(tmpl.tmpl.tags || [], tag || '') != -1)) {
            result.push(tmpl);
        };
    };
    return result;
};

DataProvider.prototype.getTemplatesByIDs = function(ids) {//Gets templates by tag
    var result = [];
    for (var i = 0; i < ids.length; i++) {//
        var tmpl = this.templateMap[ids[i]];
        if (tmpl) {
            result.push(tmpl);
        };
    };
    return result;
};

DataProvider.prototype.loadHooks = function(handler) {
    var gr = new AsyncGrouper(this.conn.length, _.bind(function(gr) {
        var err = gr.findError();
        if (err) {
            return handler(err);
        };
        var hooks = [];
        for (var i = 0; i < gr.results.length; i++) {
            var arr = gr.results[i][1];
            var conn = gr.results[i][2];
            for (var j = 0; j < arr.length; j++) {
                hooks.push({hook: arr[j], conn: conn})
            };
        };
        //checkins.sort(function(a, b) {
            //if (a.checkin.created > b.checkin.created) {
                //return 1;
            //};
            //return -1;
        //});
        return handler(null, hooks);
    }, this));
    for (var i = 0; i < this.conn.length; i++) {//getHooks
        this.conn[i].conn.getHooks({}, gr.handler, this.conn[i]);
    };
};

DataProvider.prototype.subscribe = function(ids, handler) {
    var gr = new AsyncGrouper(this.conn.length, _.bind(function(gr) {
        var err = gr.findError();
        if (err) {
            return handler(err);
        };
        return handler(null);
    }, this));
    for (var i = 0; i < this.conn.length; i++) {//getHooks
        this.conn[i].conn.subscribe(ids || [], gr.handler, this.conn[i]);
    };
};

DataProvider.prototype.loadCheckins = function(params, handler) {//Selects checkins
    var gr = new AsyncGrouper(this.conn.length, _.bind(function(gr) {
        var err = gr.findError();
        if (err) {
            return handler(err);
        };
        var checkins = [];
        for (var i = 0; i < gr.results.length; i++) {
            var arr = gr.results[i][1];
            var conn = gr.results[i][2];
            for (var j = 0; j < arr.length; j++) {
                var template = this.templateMap[arr[j].template];
                if (!template) {
                    continue;
                };
                checkins.push({tmpl: template.tmpl, checkin: arr[j], conn: conn})
            };
        };
        //checkins.sort(function(a, b) {
            //if (a.checkin.created > b.checkin.created) {
                //return 1;
            //};
            //return -1;
        //});
        return handler(null, checkins);
    }, this));
    for (var i = 0; i < this.conn.length; i++) {//getTemplates
        this.conn[i].conn.getCheckins(params, gr.handler, this.conn[i]);
    };
};
