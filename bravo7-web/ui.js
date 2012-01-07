var normalizeJSONString = function(text) {
    var json = Ext.JSON.decode(text, true);
    return json? Ext.JSON.encode(json): '{}';
};

var infoToHash = function(info) {
    if (!info) {
        return {};
    };
    var arr = info.split(',');
    var res = {};
    for (var i = 0; i < arr.length; i++) {
        var arr2 = arr[i].split(':');
        if (arr2.length == 2) {
            res[arr2[0]] = arr2[1];
        };
    };
    return res;
};

var HookManager = function(ui) {//Manages hooks in a grid
    this.ui = ui;
    Ext.define('Hook', {
        extend: 'Ext.data.Model',
        fields: [
            {name: 'id', type: 'string'},
            {name: 'index', type: 'int'},
            {name: 'conn', type: 'string'},
            {name: 'name',  type: 'string'},
            {name: 'type',  type: 'string'},
            {name: 'params', type: 'string'},
            {name: 'created', type: 'int'},
            {name: 'updated', type: 'int'},
            {name: 'when', type: 'string'},
            {name: 'body', type: 'string'},
        ]
    });
    this.store = new Ext.data.Store({
        autoLoad: true,
        data: [],
        model: 'Hook',
    });
    var menu = [];
    for (var i = 0; i < this.ui.data.size(); i++) {
        menu.push({
            text: this.ui.data.get(i).name,
            handler: _.bind(function(item) {
                var hook = Ext.ModelManager.create({}, 'Hook');
                this.edit(item.conn, hook);
            }, this),
            conn: this.ui.data.get(i),
        });
    };
    var dateRenderer = function(value) {
        return Ext.Date.format(new Date(value), 'n/j/y h:i a');
    };
    this.panel = Ext.create('widget.grid', {
        title: 'Hooks',
        store: this.store,
        closable: false,
        columns: [{
            header: 'Name:',
            dataIndex: 'name',
            flex: 1,
        }, {
            header: 'Type:',
            width: 100,
            dataIndex: 'type',
        }, {
            header: 'Connection:',
            width: 100,
            dataIndex: 'conn',
        }, {
            header: 'Updated:',
            width: 130,
            renderer: dateRenderer,
            dataIndex: 'updated',
        }, {
            header: 'Created:',
            width: 130,
            renderer: dateRenderer,
            dataIndex: 'created',
        }],
        listeners: {
            itemdblclick: _.bind(function(grid, record) {
                this.edit(this.data[record.get('index')].conn, record);
            }, this),
        },
        tbar: {
            items: [{
                text: 'New',
                menu: menu,
            }, {
                text: 'Reload',
                handler: _.bind(function() {
                    this.reload();
                }, this),
            }],
        }
    });
};

HookManager.prototype.reload = function() {
    this.panel.setLoading(true);
    this.ui.data.loadHooks(_.bind(function(err, arr) {
        this.panel.setLoading(false);
        if (err) {//
            Ext.Msg.alert('Error', err);
            return;
        };
        this.store.removeAll();
        this.data = arr;
        for (var i = 0; i < arr.length; i++) {
            var hook = arr[i].hook;
            hook.conn = arr[i].conn.name;
            hook.params = Ext.JSON.encode(hook.params);
            hook.when = Ext.JSON.encode(hook.when);
            hook.index = i;
            this.store.add(hook);
        };
    }, this))
};

HookManager.prototype.edit = function(conn, hook) {//Creates new tab or jumps to existing
    if (hook.get('id')) {//Search for existing tab
        for (var i = 0; i < this.ui.tabs.items.length; i++) {//Search tab
            var tab = this.ui.tabs.items.get(i);
            if (tab.hook_id == hook.get('id')) {
                this.ui.tabs.setActiveTab(tab);
                return;
            };
        };
    };
    var instance = this;
    var form = Ext.create('widget.form', {
        autoScroll: true,
        tbar: {
            items: [{
                text: 'Save',
                handler: _.bind(function() {//Send form
                    if (!form.getForm().isValid()) {
                        return;
                    };
                    var obj = form.getValues();
                    obj.id = hook.get('id');
                    obj.when = normalizeJSONString(obj.when);
                    obj.params = normalizeJSONString(obj.params);
                    tab.setLoading(true);
                    conn.conn.updateHook(obj, _.bind(function(err) {
                        tab.setLoading(false);
                        if (err) {
                            Ext.Msg.alert(err);
                        } else {
                            if (!obj.id) {//New tab
                                tab.close();
                            };
                            this.reload();
                        };
                    }, this))
                }, this)
            }, {
                text: 'Remove',
                hidden: !hook.get('id'),
                handler: _.bind(function() {//
                    tab.setLoading(true);
                    conn.conn.removeHook(hook.get('id'), _.bind(function(err) {//
                        tab.setLoading(false);
                        if (err) {
                            Ext.Msg.alert(err);
                        } else {
                            tab.close();
                            this.reload();
                        };
                    }, this))
                }, this)
            }]
        },
        defaults: {
            anchor: '100%'
        },
        layout: 'anchor',
        defaultType: 'textfield',
        bodyPadding: 5,
        items: [{
            fieldLabel: 'ID',
            xtype: 'displayfield',
            name: 'id',
            value: hook.get('id'),
        }, {
            fieldLabel: 'Name',
            name: 'name',
            allowBlank: false,
            value: hook.get('name'),
        }, {
            fieldLabel: 'Type',
            name: 'type',
            allowBlank: false,
            value: hook.get('type'),
        }, {
            fieldLabel: 'When',
            name: 'when',
            xtype: 'textareafield',
            fieldCls: 'monospace',
            growMax: 200,
            grow: true,
            value: hook.get('when') || '{\n\n}'
        }, {
            fieldLabel: 'Params',
            name: 'params',
            xtype: 'textareafield',
            fieldCls: 'monospace',
            growMax: 200,
            grow: true,
            value: hook.get('params') || '{\n\n}'
        }, {
            fieldLabel: 'Body (if applicable)',
            name: 'body',
            xtype: 'textareafield',
            fieldCls: 'monospace',
            growMax: 200,
            grow: true,
            value: hook.get('body')
        }],
    });
    var tab = Ext.create('widget.panel', {
        title: (hook.get('name') || 'Untitled') + ' ('+conn.name+')',
        closable: true,
        layout: 'fit',
        hook_id: hook.get('id'),
        items: form
    });
    this.ui.tabs.add(tab);
    this.ui.tabs.setActiveTab(tab);
};

var FavManager = function(ui) {//Manages Favorites and shows them on panel
    this.ui = ui;
    this.favDiv = Ext.getBody().createChild({cls: 'favs'});
    this.expandData = {};
    this.panel = Ext.create('widget.panel', {
        split: true,
        collapsible: true,
        autoScroll: true,
        region: 'west',
        tbar: {
            items: [{
                text: 'Reload',
                handler: _.bind(function() {
                    this.reload();
                }, this),
            }, {
                text: 'Reset',
                menu: [{
                    text: 'Templates',
                    handler: _.bind(function() {//Reset array and reload
                        this.reset(this.TEMPLATES_KEY);
                        this.reload();
                    }, this),
                }, {
                    text: 'Checkins',
                    handler: _.bind(function() {//Reset array and reload
                        this.reset(this.CHECKINS_KEY);
                        this.reload();
                    }, this),
                }]
            }]
        },
        title: 'Favorites:',
        contentEl: this.favDiv,
        width: 250,
    });
    this.TEMPLATES_KEY = 'template_favs';
    this.CHECKINS_KEY = 'checkin_favs';
    this.FILTER_KEY = 'checkin_filter';
    this.arrays = [this.TEMPLATES_KEY, this.CHECKINS_KEY, this.FILTER_KEY];
    this.data = {};
    for (var i = 0; i < this.arrays.length; i++) {//
        this.data[this.arrays[i]] = Ext.JSON.decode(localStorage[this.arrays[i]], true) || [];
    };
    //this.data[this.CHECKINS_KEY]  = Ext.JSON.decode(localStorage[this.CHECKINS_KEY], true) || [];
};

FavManager.prototype.isFavorite = function(id, key) {
    return _.indexOf(this.data[key], id) == -1? false: true;
};

FavManager.prototype.subscribe = function(handler) {//Subscribes for updates
    this.panel.setLoading(true);
    this.ui.data.subscribe(this.data[this.CHECKINS_KEY], _.bind(function(err, checkins) {//Favs here
        this.panel.setLoading(false);
        if (err) {
            Ext.Msg.alert('Error', err);
        };
        if (handler) {
            handler(err);
        };
    }, this));
};

FavManager.prototype.reload = function() {//Reloads favs
    this.panel.setLoading(true);
    var tmpls = this.ui.data.getTemplatesByIDs(this.data[this.TEMPLATES_KEY]);
    this.ui.data.loadCheckins({ids: this.data[this.CHECKINS_KEY]}, _.bind(function(err, checkins) {//Favs here
        this.panel.setLoading(false);
        if (err) {
            Ext.Msg.alert('Error', err);
            return;
        };
        Ext.Array.every(this.favDiv.query('*'), function(item) {//
            Ext.removeNode(item);
            return true;
        });
        this.favDiv.createChild({cls: 'fav_title', html: 'Templates:'});
        for (var i = 0; i < tmpls.length; i++) {//Show template
            this.ui.renderTemplate({
                parent: this.favDiv,
                cls: 'fav_item fav_item_template',
                element: this.panel,
                template: tmpls[i].tmpl,
                //handler: _.bind(function() {
                    //this.reload();
                //}, this),
                conn: tmpls[i].conn
            });
        };
        this.favDiv.createChild({style: 'clear: both;'});
        this.favDiv.createChild({cls: 'fav_title', html: 'Checkins:'});
        for (var i = 0; i < checkins.length; i++) {//
            this.ui.renderCheckin({
                checkin: checkins[i].checkin,
                template: checkins[i].tmpl,
                conn: checkins[i].conn,
                show_text: true,
                expandData: this.expandData,
                parent: this.favDiv,
                cls: 'fav_item fav_item_checkin',
                element: this.panel,
                //handler: _.bind(function() {
                    //this.reload();
                //}, this),
            });
        };
        this.favDiv.createChild({style: 'clear: both;'});
    }, this));
};

FavManager.prototype.reset = function(key) {
    this.data[key] = [];
    localStorage[key] = [];
};

FavManager.prototype.save = function(key) {
    localStorage[key] = Ext.JSON.encode(this.data[key] || []);
};

FavManager.prototype.manageFavorite = function(id, add, key) {//Manages fav
    if (add) {
        if (_.indexOf(this.data[key], id) == -1) {
            this.data[key].push(id);
            this.save(key);
            if (key != this.FILTER_KEY) {//
                this.reload();
                this.subscribe();
            };
        };
    } else {
        var sz = this.data[key].length;
        this.data[key] = _.without(this.data[key], id);
        if (sz != this.data[key].length) {
            this.save(key);
            if (key != this.FILTER_KEY) {//
                this.reload();
                this.subscribe();
            };
        };
    };
};

var UICalendar = function(ui) {
    this.ui = ui;
    this.renderType = 'calendar';
    this.checkinDiv = Ext.getBody().createChild({cls: 'checkins'});
    this.checkinToolbar = Ext.create('widget.toolbar', {
        items: [],
    });
    this.checkinPanel = Ext.create('widget.panel', {
        region: 'center',
        tbar: this.checkinToolbar,
        autoScroll: true,
        contentEl: this.checkinDiv
    });
    this.searchForm = Ext.create('widget.form', {
        defaults: {
            anchor: '100%'
        },
        width: '100%',
        border: false,
        layout: 'anchor',
        defaultType: 'textfield',
        bodyPadding: 3,
        items: [{
            xtype: 'label',
            text: 'From:',
        }, {
            xtype: 'datefield',
            name: 'from',
        }, {
            xtype: 'label',
            text: 'To:',
        }, {
            xtype: 'datefield',
            name: 'to',
        }, {
            xtype: 'label',
            text: 'Text:',
        }, {
            name: 'query'
        }, {
            xtype: 'label',
            text: 'Template:',
        }, {
            name: 'template'
        }],
        buttons: [{
            text: 'Search',
            handler: _.bind(function() {//Submit
                this.filter = this.searchForm.getValues();
                this.reload('search');
            }, this),
        }, {
            text: 'Reset',
            handler: _.bind(function() {//Reset form
                this.searchForm.getForm().reset();
            }, this),
        }],
    });
    this.panel = Ext.create('widget.panel', {
        title: 'Flow',
        tbar: {
            items: [{
                text: 'Reload',
                handler: _.bind(function() {
                    this.reload();
                }, this),
            }]
        },
        closable: false,
        layout: 'border',
        items: [{
            layout: 'vbox',
            region: 'west',
            layout: 'accordion',
            width: 180,
            items: [{
                title: 'Calendar',
                items: {
                    xtype: 'datepicker',
                    border: false,
                    handler: _.bind(function(picker, dt) {
                        this.filter = {date: dt};
                        this.reload('calendar');
                    }, this),
                }
            }, {
                title: 'Search',
                width: '100%',
                layout: 'vbox',
                items: [this.searchForm]
            },
            ]
        }, this.checkinPanel]
    });
    this.filter = {date: new Date()};
};

UICalendar.prototype.reload = function(type) {//
    var params = {};
    var render = null;
    if (this.filter.date) {
        var dt = new Date(this.filter.date.getTime());
        dt.setHours(0);
        dt.setMinutes(0);
        dt.setSeconds(0);
        params = {from: dt.getTime()};
        dt.setHours(23);
        dt.setMinutes(59);
        dt.setSeconds(59);
        params.to = dt.getTime();
        render = 'calendar';
    } else if (this.filter.query || this.filter.template) {//
        params.limit = 100;
        params.sort = 'created';
        if (this.filter.query) {
            params.query = Ext.JSON.encode({field: 'text', search: this.filter.query});
        };
        if (this.filter.template) {//Add as template
            params.filter = Ext.JSON.encode({template: this.filter.template});
        };
        if (this.filter.from) {
            params.from = this.filter.from.getTime();
        };
        if (this.filter.to) {
            params.to = this.filter.to.getTime();
        };
        render = 'search';
    } else {
        Ext.Msg.alert('Error', 'Form is empty');
        return;
    };
    this.panel.setLoading(true);
    params.sort = 'created';
    this.ui.data.loadCheckins(params, _.bind(function(err, data) {
        this.panel.setLoading(false);
        if (err) {
            Ext.Msg.alert('Error', err);
            return;
        };
        this.data = data;
        if (render == 'calendar') {//
            this.drawCalendar();
        };
        if (render == 'search') {//
            this.drawSearchResults();
        };
    }, this));
};

UICalendar.prototype.drawSearchResults = function() {//
    Ext.Array.every(this.checkinDiv.query('*'), function(item) {//
        Ext.removeNode(item);
        return true;
    });
    var cols = {};
    var labelpadding = 0;
    for (var i = 0; i < this.data.length; i++) {//
        var checkin = this.data[i];
        var dt = new Date(parseInt(checkin.checkin.created));
        var dtLabel = Ext.Date.format(dt, 'n/j/y');
        if (!cols[dtLabel]) {//Create new col
            //log('creating col', dtLabel);
            var div = this.checkinDiv.createChild({cls: 'search_col'});
            div.on('click', _.bind(function() {
                this.ui.closeCheckin();
                return false;
            }, this))
            div.createChild({cls: 'search_col_caption search_col_capt'+labelpadding, html: dtLabel});
            labelpadding++;
            if (labelpadding>2) {
                labelpadding = 0;
            };
            var place = div.createChild({cls: 'search_col_place'});
            cols[dtLabel] = {place: place};
        };
        this.ui.renderCheckin({
            checkin: checkin.checkin,
            small: true,
            template: checkin.tmpl,
            conn: checkin.conn,
            parent: cols[dtLabel].place,
            element: this.checkinPanel,
            //handler: _.bind(function() {
                //this.reload();
            //}, this),
        });
        cols[dtLabel].place.createChild({style: 'clear: both;'});
    };
};

UICalendar.prototype.enableHrDrop = function(hr, hour) {
    var dt = new Date(this.filter.date.getTime());
    dt.setHours(hour);
    dt.setMinutes(0);
    var hrdrop = new Ext.dd.DropTarget(hr.id, {
        ddGroup: 'template',
    });
    hrdrop.addToGroup('checkin');
    hrdrop.notifyDrop = _.bind(function(source, e, data) {//Dropped
        console.log('Template drop to calendar', data);
        this.ui.editCheckin(data.conn, data.template, data.checkin, {
            parent: this.checkinPanel, 
            element: hr, 
            tab: e.shiftKey || false, 
            created: dt.getTime(),
            handler: _.bind(function() {
                this.reload();
            }, this)
        });
        return true;
    }, this);
};

UICalendar.prototype.drawCalendar = function() {//
    Ext.Array.every(this.checkinDiv.query('*'), function(item) {//
        Ext.removeNode(item);
        return true;
    });
    var hourDivs = [];
    var dt = new Date();
    dt.setMinutes(0);
    var now = new Date();
    for (var i = 0; i < 24; i++) {//Create hour divs
        var hr = this.checkinDiv.createChild({cls: 'hour '+(now.getHours() == i? 'hour_now': '')});
        hr.on('click', _.bind(function() {
            this.ui.closeCheckin();
            return false;
        }, this))
        dt.setHours(i);
        hr.createChild({cls: 'hour_caption', html: Ext.Date.format(dt, 'h:i a')});
        var checkinPlace = hr.createChild({cls: 'hour_place'});
        hourDivs.push(checkinPlace);
        hr.createChild({style: 'clear: both;'});
        this.enableHrDrop(hr, i);
    };
    var types = [];
    this.checkinToolbar.removeAll();
    //this.checkinPanel.selectedCheckin = null;
    var tbarConfig = this.ui.favorites.data[this.ui.favorites.FILTER_KEY];
    for (var i = 0; i < this.data.length; i++) {//
        var checkin = this.data[i];
        var type = checkin.checkin.type;
        if (type) {
            if (types.indexOf(type) == -1) {//New type
                types.push(type);
                var item = {
                    text: type,
                    checkin_type: type,
                    enableToggle: true,
                    pressed: tbarConfig.indexOf(type) == -1,
                    handler: _.bind(function(item) {
                        this.ui.favorites.manageFavorite(item.checkin_type, !item.pressed, this.ui.favorites.FILTER_KEY);
                        this.drawCalendar();
                    }, this),
                };
                this.checkinToolbar.add(item);
            };
            if (tbarConfig.indexOf(type) != -1) {//Skip
                continue;
            };
        };
        dt = new Date(parseInt(checkin.checkin.created));
        //log('data dt', dt, checkin.checkin.created);
        var place = hourDivs[dt.getHours()];
        this.ui.renderCheckin({
            checkin: checkin.checkin,
            small: true,
            template: checkin.tmpl,
            conn: checkin.conn,
            parent: place,
            element: this.checkinPanel,
            show_min: true,
            min: dt.getMinutes(),
            handler: _.bind(function() {
                this.reload();
            }, this),
        });
    };
};

var UI = function(viewport, dataProvider) {//
    this.viewport = viewport;
    this.data = dataProvider;
    var tabs = Ext.create('widget.tabpanel', {
        region: 'center',
    });
    var header = Ext.create('widget.panel', {
        height: 30,
        region: 'north',
        bodyCls: 'header'
    });
    this.favorites = new FavManager(this);
    this.hooks = new HookManager(this);
    var templateMenu = [];
    for (var i = 0; i < this.data.size(); i++) {
        templateMenu.push({
            text: this.data.get(i).name,
            handler: _.bind(function(item) {//
                this.editTemplate(item.conn);
            }, this),
            conn: this.data.get(i),
        });
    };
    this.templates = Ext.create('widget.panel', {
        title: 'Templates',
        closable: false,
        region: 'east',
        width: 200,
        split: true,
        collapsible: true,
        layout: 'accordion',
        layoutConfig: {
            activeOnTop: true,
        },
        tbar: {
            items: [{
                text: 'New',
                menu: templateMenu,
            }, {
                text: 'Reload',
                handler: _.bind(function() {
                    this.reloadTemplates();
                }, this),
            //}, {
                //text: 'Pin',
                //handler: _.bind(function() {//
                    //this.templatesPin = !this.templatesPin;
                    //if (this.templatesPin) {
                        //log('Pin to right');
                        //this.templates.region = 'east';
                        //this.templates.width = 300;
                        //main.add(this.templates);
                    //} else {
                        //log('Do as tab');
                        //this.tabs.add(this.templates);
                    //};
                    //main.doLayout();
                //}, this),
            }, {
                text: 'Hooks',
                handler: _.bind(function() {
                    if (!this.tabs.items.contains(this.hooks.panel)) {//
                        this.tabs.add(this.hooks.panel);
                    };
                    this.tabs.setActiveTab(this.hooks.panel);
                    this.hooks.reload();
                }, this),
            }],
        },
    });
    this.statusBar = Ext.create('widget.toolbar', {
        items: ['Status:', '->', {
            text: 'Token...',
        }],
    });
    for (var i = 0; i < this.data.conn.length; i++) {//Create markers
        this.addSessionItem(this.data.conn[i]);
    };
    var main = Ext.create('widget.panel', {
        //tbar: {
            //xtype: 'toolbar',
            //items: [{
                //text: 'New template',
                //menu: templateMenu,
            //}],
        //},
        layout: 'border',
        bbar: this.statusBar,
        items: [this.favorites.panel, tabs, header, this.templates],
    });
    viewport.add(main);
    this.tabs = tabs;
    this.defaultFlow = new UICalendar(this);
    this.reloadTemplates(_.bind(function() {
        this.tabs.add(this.defaultFlow.panel);
        this.tabs.setActiveTab(this.defaultFlow.panel);
        this.defaultFlow.reload();
        this.favorites.reload();
    }, this));
};

UI.prototype.addSessionItem = function(conn) {//
    var ICON_OFF = 'img/ui/promo_red.png';
    var ICON_ON = 'img/ui/promo_green.png';
    var ICON_NET = 'img/ui/promo_orange.png';
    var item = this.statusBar.insert(1, {
        icon: ICON_OFF,
        menu: [{
            text: 'Reconnect',
            handler: _.bind(function() {//
                if (!conn.connected) {
                    this.data.setupSSE(conn);
                };
            }, this),
        }, {
            text: 'Token',
            handler: _.bind(function() {//
                this.showTokenDialog(conn);
            }, this),
        }],
        text: conn.name,
    });
    conn.sessionHandler = _.bind(function(connected) {//
        item.setIcon(connected? ICON_ON: ICON_OFF);
        if (connected) {//Subscribe
            this.favorites.subscribe();
        };
    }, this);
    conn.dataHandler = _.bind(function(obj) {
        if (obj.type == 'checkin') {//Reload favs
            this.favorites.reload();
        };
    }, this);
    conn.conn.networkHandler = _.bind(function(network) {//
        item.setIcon(network? ICON_NET: (conn.connected? ICON_ON: ICON_OFF));
    }, this);
};

UI.prototype.showTokenDialog = function(conn) {//Auth dialog
    var form = Ext.create('widget.form', {
        defaults: {
            anchor: '100%'
        },
        layout: 'anchor',
        defaultType: 'textfield',
        bodyPadding: 5,
        items: [
            {
                fieldLabel: 'Root URL',
                name: 'rootURL',
                xtype: 'displayfield',
                value: conn.url,
            }, {
                fieldLabel: 'Username',
                name: 'username',
            }, {
                fieldLabel: 'Password',
                inputType: 'password',
                name: 'password',
            }, {
                fieldLabel: 'Token',
                name: 'token',
                xtype: 'displayfield',
            }
        ],
        buttons: [{
            text: 'Get token',
            handler: _.bind(function() {//Update record
                var username = form.getValues().username;
                var password = form.getValues().password;
                win.setLoading(true);
                oauthGetAccessTokenUsername(conn.url+'/login', oauthClientID, oauthClientSecret, username, password, _.bind(function(err, token) {//
                    win.setLoading(false);
                    if (err) {
                        Ext.Msg.alert('Error', err);
                    } else {
                        form.getForm().setValues({token: token});
                    }
                }, this))
                form.getForm().setValues({token: ''});
            }, this),
        }],
    });
    var win = Ext.create('widget.window', {
        title: 'Get token ('+conn.name+')',
        width: 400,
        layout: 'fit',
        items: form
    });
    win.show();
};

UI.prototype.renderCheckin = function(config) {//Renders checkin
    var div = config.parent.createChild({cls: 'checkin selectable '+(config.cls || '')});
    if (config.ref_of) {
        config.parent.createChild({style: 'clear: both;'});
    };
    if (!config.ref_of) {//
        div.addClsOnOver('selectable_over');
    };
    var refsExpanded = false;
    var refsDiv = null;
    var refsLoaded = null;
    var expandRefs = _.bind(function() {//
        refsDiv.toggle();
        refsExpanded = !refsExpanded;
        if (config.expandData) {
            config.expandData[config.checkin.id] = refsExpanded;
        };
        if (refsLoaded) {
            return;
        };
        refsLoaded = true;
        div.addCls('checkin_loading');
        this.data.loadCheckins({ids: config.checkin.refs, sort: config.checkin.sort || 'created'}, _.bind(function(err, arr) {//
            div.removeCls('checkin_loading');
            if (err) {
                Ext.Msg.alert('Error', err);
                return;
            };
            for (var i = 0; i < arr.length; i++) {
                var template = arr[i].tmpl;
                this.renderCheckin({
                    checkin: arr[i].checkin,
                    show_text: true,
                    small: true,
                    template: template,
                    conn: arr[i].conn,
                    cls: config.cls,
                    parent: refsDiv,
                    ref_of: config.checkin.id,
                    ref_conn: config.conn,
                    element: config.element,
                    handler: config.handler,
                });
            };
        }, this))
    }, this);
    var icon = div.createChild({cls: 'checkin_icon'+(config.small? ' checkin_icon16': ''), style: 'background-image: url('+this.data.buildIcon(config.template.icon, null, config.small)+')'});
    icon.on('click', _.bind(function(e) {//
        if (e.ctrlKey || e.shiftKey) {//Edit
            this.editCheckin(config.conn, config.template, config.checkin, {
                parent: config.element, 
                title: _text,
                element: icon, 
                tab: e.shiftKey || false,
                handler: _.bind(function() {
                    if (config.handler) {
                        config.handler();
                    };
                }, this),
                ref_conn: config.ref_conn,
                ref_of: config.ref_of
            });
        } else {//Show text
            more.toggle();
            if (!config.show_text) {
                text.toggle();
            };
            if (config.show_min) {
                min.toggle();
            };
            if (refsDiv) {
                expandRefs();
            };
        };
        e.stopPropagation();
    }, this))
    if (config.checkin.link) {//Have link
        var link = icon.createChild({cls: 'link'});
        link.on('click', _.bind(function() {
            window.open(config.checkin.link);
            return false;
        }, this))
    };
    var drag = new Ext.dd.DragSource(icon.id, {
        ddGroup: 'checkin',
        dragData: config
    });
    //drag.groups = {checkin: true, template: true};
    var drop = new Ext.dd.DropTarget(div.id, {
        ddGroup: 'checkin'
    });
    drag.beforeDragEnter = _.bind(function(target, e, id) {//Drop
        //log('beforeDragEnter*', target == drop);
        if (target == drop) {
            return false;
        };
        return true;
    }, this)
    drag.beforeDragOver = _.bind(function(target, e, id) {//Drop
        //log('beforeDragOver*', target == drop);
        if (target == drop) {
            return false;
        };
        return true;
    }, this)
    drop.notifyDrop = _.bind(function(source, e, data) {//Drop
        log('On checkin dropped?');
        if (source == drag) {
            return false;
        };
        if (data.template && !data.checkin) {//We have template
            log('Template on checkin (no checkin)');
            this.editCheckin(data.conn, data.template, null, {
                parent: config.element, 
                element: div, 
                tab: e.shiftKey || false, 
                attach_to: config.checkin.id,
                handler: _.bind(function() {
                }, this)
            });
            e.stopPropagation();
            return true;
        };
        log('Checkin on checkin -> do addRef');
        config.conn.conn.addRef(config.checkin.id, data.checkin.id, _.bind(function(err) {
            if (err) {
                Ext.Msg.alert('Error', err);
                return;
            };
            config.handler();
        }, this));
        e.stopPropagation();
        return true;
    }, this)
    var _text = config.checkin.text || '';
    if (_text.indexOf('\n') != -1) {
        _text = _text.substr(0, _text.indexOf('\n'));
    };
    if (_text.length>100) {
        _text = _text.substr(0, 100)+'...';
    };
    if (!_text) {//Show date
        console.log('Checkin', config.checkin);
        _text = Ext.Date.format(new Date(parseInt(config.checkin.created)), 'n/j/y h:i a')
    };
    var _tmplData = _.clone(config.checkin);
    _tmplData.text = Ext.htmlEncode(_text).replace(/\n/g, '<br/>');
    var text = icon.createChild({cls: 'checkin_text', html: config.template._template.apply(_tmplData)}).enableDisplayMode();
    if (!config.show_text) {//
        text.hide();
    };
    var min = null;
    if (config.show_min) {//
        min = icon.createChild({cls: 'checkin_time', html: Ext.String.leftPad(config.min || 0, 2, '0')}).enableDisplayMode();
    };
    var more = icon.createChild({cls: 'checkin_more'}).enableDisplayMode().hide();
    var imgSize = 162.0;
    var loc = config.checkin.location || {};
    var path = config.checkin.path || [];
    var image_info = config.checkin.image_info;
    if (loc && loc.lat && loc.lon) {//
        more.createChild({
            html: '<a target="_blank" href="http://maps.google.com/maps?q='+loc.lat+','+loc.lon+'&z=18"><img class="map_small" src="http://maps.google.com/maps/api/staticmap?center='+loc.lat+','+loc.lon+'&zoom=15&size=160x125&sensor=true&markers=color:red|size:mid|'+loc.lat+','+loc.lon+'"/></a>'
        });
        //&markers=color:red|'+loc.lat+','+loc.lon+'
    };
    if (path && path.length>0) {//Show path
        var pathDesc = 'color:0000ff|weight:3';
        for (var i = 0; i < path.length; i++) {
            if (path[i].lat && path[i].lon) {
                pathDesc += '|'+path[i].lat+','+path[i].lon
            };
        };
        more.createChild({
            html: '<a target="_blank" href="http://maps.google.com/maps?q='+path[0].lat+','+path[0].lon+'&z=18"><img class="map_small" src="http://maps.google.com/maps/api/staticmap?size=160x125&sensor=true&path='+pathDesc+'"/></a>'
        });
    };
    if (image_info) {
        var info = infoToHash(image_info);
        var ratio = 1.0;
        var imgHeight = imgSize;
        if (info.w>0 && info.h>0) {
            ratio = info.w / imgSize;//Big
            imgHeight = info.h / ratio;//Small
        };
        more.createChild({
            html: '<a target="_blank" href="'+config.conn.url+'/download/'+config.checkin.id+'/image?access_token='+config.conn.conn.token+'"><img class="img_small" src="'+config.conn.url+'/download/'+config.checkin.id+'/image?access_token='+config.conn.conn.token+'&width='+Math.round(imgSize)+'" width="'+Math.round(imgSize)+'px" height="'+Math.round(imgHeight)+'px"/></a>'
        });
        
    };
    if (config.checkin.refs && config.checkin.refs.length>0) {//Have refs
        div.addCls('refs_mark');
        refsDiv = div.createChild({cls: 'checkin_refs', }).enableDisplayMode().hide();
        if (config.expandData && config.expandData[config.checkin.id]) {//Expanded
            expandRefs();
        };
    };
    div.createChild({style: 'clear: both;'});
};

UI.prototype.renderTemplate = function(config) {//Renders template
    var item = config.parent.createChild({cls: 'template selectable '+(config.cls || '')});
    item.addClsOnOver('selectable_over');
    var text = item.createChild({cls: 'template_text', style: 'background-image: url('+this.data.buildIcon(config.template.icon)+');'});
    var name = text.createChild({cls: 'template_name', html: config.template.name});
    var conn = text.createChild({cls: 'template_conn', html: config.conn.name});
    item.on('click', _.bind(function(e) {//
        if (e.ctrlKey) {
            this.editTemplate(config.conn, config.template);
            return;
        };
        this.editCheckin(config.conn, config.template, null, {parent: config.element, element: item, tab: e.shiftKey || false, handler: config.handler});
    }, this));
    var drag = new Ext.dd.DragSource(text.id, {
        ddGroup: 'template',
        dragData: config,
    });
    drag.addToGroup('checkin');
    var drop = new Ext.dd.DropTarget(item.id, {
        ddGroup: 'checkin',
    });
    drag.beforeDragEnter = _.bind(function(target, e, id) {//Drop start
        //log('beforeDragEnter', target == drop);
        if (target == drop) {
            return false;
        };
        return true;
    }, this)
    drag.beforeDragOver = _.bind(function(target, e, id) {//Drop move
        //log('beforeDragOver', target == drop);
        if (target == drop) {
            return false;
        };
        return true;
    }, this)
    drop.notifyDrop = _.bind(function(source, e, data) {//Dropped
        if (!data.checkin) {
            log('Template on template - no way');
            return true;
        };
        log('checkin drop to template', data.checkin.id);
        this.editCheckin(config.conn, config.template, null, {parent: config.element, element: item, tab: e.shiftKey || false, handler: config.handler, refs: [data.checkin.id]});
        e.preventDefault();
        return true;
    }, this)
};

UI.prototype.reloadTemplates = function(handler) {
    //log('reloadTemplates', this.templates.contentEl);
    this.templates.setLoading(true);
    this.data.loadTemplates(_.bind(function(err, templates, tags) {
        this.templates.setLoading(false);
        if (err) {
            Ext.Msg.alert('Error', err);
            return;
        };
        var instance = this;
        //log('templates:', templates.length, 'tags', tags.length);
        this.templates.removeAll();
        var items = [];
        for (var i = 0; i < tags.length; i++) {//
            var div = Ext.getBody().createChild({tag: 'div'});
            var tmpls = this.data.getTemplatesByTag(tags[i]);
            for (var j = 0; j < tmpls.length; j++) {
                this.renderTemplate({
                    parent: div,
                    element: this.templates,
                    template: tmpls[j].tmpl,
                    conn: tmpls[j].conn
                });
            };
            var panel = {
                title: tags[i] || 'No tags',
                multi: true,
                contentEl: div
            };
            this.templates.add(panel);
        };
        if (tags.length>0) {
            this.templates.items.get(0).expand();
        };
        if (handler) {
            handler();
        };
    }, this));
};

UI.prototype.showIconDialog = function(icon, handler) {//Show icon selection window
    var cats = _.keys(iconRegistry);
    var items = [];
    for (var i = 0; i < cats.length; i++) {//Create panels
        var html = Ext.getBody().createChild({tag: 'div'});
        var arr = iconRegistry[cats[i]];
        for (var j = 0; j < arr.length; j++) {//
            var div = html.createChild({
                cls: 'icon_32 icon_select selectable',
                style: 'background-image: url('+this.data.buildIcon(arr[j], cats[i])+')'
            });
            div.addClsOnOver('selectable_over');
            div.on('click', _.bind(function() {
                win.close();
                handler(this);
            }, arr[j]));
        };
        items.push({
            title: cats[i],
            autoScroll: true,
            contentEl: html
        });
    };
    var win = Ext.create('widget.window', {
        title: 'Select icon',
        modal: true,
        width: 400,
        height: 300,
        layout: 'fit',
        items: {
            layout: 'accordion',
            items: items,
        }
    });
    win.show();
};

UI.prototype.closeCheckin = function() {
    if (this.checkinPanel) {
        this.checkinPanel.close();
        this.checkinPanel = null;
    };
};

UI.prototype.editLocation = function(loc, path) {//
    var placeRadius = 100;
    var locIndex = 0;
    var form = Ext.create('widget.form', {
        region: 'south',
        defaults: {
            anchor: '100%'
        },
        layout: 'anchor',
        bodyPadding: 5,
        bbar: {
            items: [{
                text: 'Fix altitude',
                handler: function() {//Get altitude
                    form.setLoading(true);
                    var elevator = new google.maps.ElevationService();
                    elevator.getElevationForLocations({
                        locations: [latLon],
                    }, function(results, st) {//Elevation done
                        form.setLoading(false);
                        if (st == google.maps.ElevationStatus.OK && results[0]) {//Change elevation
                            altField.setValue(results[0].elevation);
                        };
                    });
                },
            }, {
                text: 'Finish',
                handler: function() {
                    loc.name = nameField.getValue();
                    loc.alt = altField.getValue() || 0;
                    loc.lat = latLon.lat();
                    loc.lon = latLon.lng();
                    win.close();
                },
            }, {
                text: 'Radius:',
                xtype: 'slider',
                width: 350,
                minValue: 10,
                value: placeRadius,
                maxValue: 1000,
                listeners: {
                    changecomplete: function(item) {
                        placeRadius = item.getValue();
                        refreshPlaces(latLon, placeRadius)
                    },
                },
            }]
        },
    });
    var nameField = form.add({
        xtype: 'textfield',
        fieldLabel: 'Name',
        value: loc.name
    });
    var altField = form.add({
        xtype: 'textfield',
        fieldLabel: 'Altitude',
        value: loc.alt || 0
    });
    var div = Ext.getBody().createChild({cls: 'map'});
    var panel = Ext.create('widget.panel', {
        region: 'center',
        border: false,
        contentEl: div
    });
    var win = Ext.create('widget.window', {
        title: 'Maps editor',
        modal: true,
        width: 500,
        height: 500,
        layout: 'border',
        items: [panel, form],
    });
    win.show();
    var latLon = new google.maps.LatLng(defaultLocation.lat, defaultLocation.lon);
    if (loc.lat && loc.lon) {//
        latLon = new google.maps.LatLng(loc.lat, loc.lon);
    };
    var mapOptions = {
        zoom: 18,
        center: latLon,
        mapTypeId: google.maps.MapTypeId.ROADMAP
    };
    var map = new google.maps.Map(div.dom, mapOptions);
    var marker = new google.maps.Marker({
        position: latLon,
        map: map,
        draggable: true,
        icon: 'img/ui/map_blue.png',
        animation: google.maps.Animation.DROP,
        title: 'Location'
    });
    var placesDots = [];
    var placesCircle = null;
    var placesCircle = new google.maps.Circle({
        strokeColor: "#007700",
        strokeOpacity: 0.4,
        strokeWeight: 2,
        fillColor: "#00ff00",
        fillOpacity: 0.1,
        map: map,
        center: latLon,
        radius: placeRadius
    });

    var refreshPlaces = function(latLon, radius) {//
        form.setLoading(true);
        gMapsPlaceSearch(latLon, radius, function(err, result) {//
            form.setLoading(false);
            if (err) {
                Ext.Msg.alert('Error', err);
                return;
            };
            placesCircle.setCenter(latLon);
            placesCircle.setRadius(radius);
            for (var i = 0; i < placesDots.length; i++) {//
                placesDots[i].setMap(null);
            };
            placesDots = [];
            var results = result.results || [];
            for (var i = 0; i < results.length; i++) {
                //log('Found place:', results[i].name);
                var dot = new google.maps.Marker({
                    title: results[i].name,
                    icon: 'img/ui/map_red.png',
                    map: map,
                    position: new google.maps.LatLng(results[i].geometry.location.lat, results[i].geometry.location.lng),
                });
                google.maps.event.addListener(dot, 'dblclick', _.bind(function(e) {
                    latLon = this.dot.getPosition();
                    marker.setPosition(latLon);
                    nameField.setValue(this.data.name);
                    refreshPlaces(latLon, radius);
                }, {data: results[i], dot: dot}))
                placesDots.push(dot)
            };
        });
    };
    google.maps.event.addListener(marker, 'dragend', _.bind(function() {//Marker moved
        //log('New position:', marker.getPosition().lat(), marker.getPosition().lng());
        latLon = marker.getPosition();
        refreshPlaces(latLon, placeRadius);
    }, this))
    //log('Map shown:', map, loc.lat, loc.lon);
    if (loc.acc>0) {//Draw circle
        var circle = new google.maps.Circle({
            strokeColor: "#0000ff",
            strokeOpacity: 0.4,
            strokeWeight: 2,
            fillColor: "#0000ff",
            fillOpacity: 0.2,
            map: map,
            center: latLon,
            radius: loc.acc
        });
    };
    refreshPlaces(latLon, placeRadius);
};

UI.prototype.editCheckin = function(conn, tmpl, checkin, config) {//Panel with editor
    var ch = _.clone(checkin || {});
    var doSave = _.bind(function() {//Save
        var obj = {};
        var upload = false;
        if (config.refs) {//Have refs
            obj.refs = config.refs;
        };
        if (config.attach_to) {
            obj.attach_to = config.attach_to;
        };
        for (var i = 0; i < fields.length; i++) {//
            var f = fields[i];
            var conf = editor[f];
            var el = elements[f];
            var skip = false;
            if (!el) {
                continue;
            };
            var value = null;
            if (conf.type == 'datetime') {
                var dt = el.items.get(0).getValue();
                var tm = el.items.get(1).getValue();
                if (dt && tm) {
                    dt.setHours(tm.getHours());
                    dt.setMinutes(tm.getMinutes());
                };
                value = dt.getTime();
                //log('datetime', dt, tm);
            } else if (conf.type == 'check') {//Checkbox
                if (el.isDisabled()) {
                    skip = true;
                } else {
                    value = el.getValue()? conf.value: '';
                };
            } else if (conf.type == 'location') {//Location
                value = Ext.JSON.encode(el);
            } else if (conf.type == 'file') {//Location
                skip = true;
                if (el.getValue()) {
                    upload = true;
                };
            } else {//All others
                value = el.getValue();
            };
            if (!skip) {//
                obj[f] = value;
            };
            //log('Save', f, '=', value);
        };
        panel.setLoading(true);
        var handler = _.bind(function(err, result) {//
            panel.setLoading(false);
            if (err) {
                Ext.Msg.alert('Error', err);
            } else {
                if (upload) {
                    upload = false;
                    conn.conn.upload(result.id, panel.getForm(), handler);
                    return;
                };
                if (config.tab) {//In tab
                    if (!checkin) {//
                        panel.close();
                    };
                } else {//Floating
                    this.closeCheckin();
                };
                if (config.handler) {
                    config.handler();
                };
            };
        }, this);
        if (!checkin) {//New checkin
            conn.conn.checkin(tmpl.id, obj, handler);
        } else {//Update checkin
            obj.template = tmpl.id;
            obj.id = checkin.id;
            conn.conn.updateCheckin(obj, handler);
        };
    }, this);
    var tbar = {
        items: [{
            text: 'Save',
            handler: doSave,
        }, {
            text: 'Remove',
            hidden: !checkin? true: false,
            handler: _.bind(function() {//Remove
                if (!checkin) {
                    this.closeCheckin();
                } else {
                    panel.setLoading(true);
                    conn.conn.removeCheckin(checkin, _.bind(function(err) {//Remove
                        panel.setLoading(false);
                        if (err) {
                            Ext.Msg.alert('Error', err);
                        } else {
                            if (config.tab) {//In tab
                                panel.close();
                            } else {//Floating
                                this.closeCheckin();
                            };
                            if (config.handler) {
                                config.handler();
                            };
                        };
                    }, this));
                }
            }, this),
        }, {
            text: 'Unlink',
            hidden: config.ref_of? false: true,
            handler: _.bind(function() {
                panel.setLoading(true);
                config.ref_conn.conn.removeRef(config.ref_of, checkin.id, _.bind(function(err) {//Remove
                    panel.setLoading(false);
                    if (err) {
                        Ext.Msg.alert('Error', err);
                    } else {
                        this.closeCheckin();
                        if (config.handler) {
                            config.handler();
                        };
                    };
                }, this));
            }, this),
        }, {
            text: 'Fav',
            enableToggle: true,
            pressed: this.favorites.isFavorite(checkin? checkin.id: null, this.favorites.CHECKINS_KEY),
            hidden: !checkin,
            handler: _.bind(function(item) {//
                this.favorites.manageFavorite(checkin.id, item.pressed, this.favorites.CHECKINS_KEY);
                this.closeCheckin();
            }, this),
        }, {
            text: 'To tab',
            hidden: config.tab,
            handler: _.bind(function() {//
                config.tab = true;
                this.closeCheckin();
                this.editCheckin(conn, tmpl, checkin, config);
            }, this),
        }],
    };
    var panelTitle = config.title || 'Checkin ('+tmpl.name+')';
    if (config.attach_to) {
        panelTitle += ' -> '+config.attach_to;
    };
    if (config.refs) {
        panelTitle += ' <- '+config.refs[0];
    };
    var panel = Ext.create('widget.form', {
        layout: 'anchor',
        defaults: {
            anchor: '100%',
            margin: 3,
        },
        floating: config.tab? false: true,
        resizable: config.tab? false: true,
        activeItem: 1,
        focusOnToFront: true,
        toFrontOnShow: true,
        width: 300,
        closable: true,
        title: panelTitle+':',
        shadow: true,
        tbar: config.tab? tbar: null,
        bbar: config.tab? null: tbar,
    });
    this.closeCheckin();
    var editor = _.clone(tmpl.editor || {});
    var fields = _.keys(editor);
    //Add date/time
    fields.push('created');
    editor.created = {type: 'datetime'};
    ch.created = config.created || ch.created || new Date().getTime();
    var elements = {};
    var activeItems = [];
    var saveOnEnter = _.bind(function(field, e) {//Enter
        if (e.getKey() == e.ENTER) {
            doSave();
        };
    }, this);
    if (checkin && config.tab) {//Show ID
        panel.add({value: checkin.id, xtype: 'displayfield'});
    };
    for (var i = 0; i < fields.length; i++) {//
        var f = fields[i];
        var conf = editor[f];
        panel.add({text: f, forId: f, xtype: 'label'});
        if (conf.type == 'text' || !conf.type) {//Text field
            elements[f] = panel.add({
                xtype: 'textfield',
                name: f,
                listeners: {
                    specialkey: saveOnEnter,
                },
                value: ch[f],
            });
            activeItems.push(elements[f]);
        };
        if (conf.type == 'file') {//Text field
            elements[f] = panel.add({
                xtype: 'filefield',
                name: f,
                buttonText: 'Select file',
                listeners: {
                    change: function(el, value) {
                        log('Change:', value);
                        if (elements['text'] && !elements['text'].getValue()) {
                            elements['text'].setValue(value);
                        };
                    }
                },
            });
            activeItems.push(elements[f]);
        };
        if (conf.type == 'textarea') {//Text area
            elements[f] = panel.add({
                xtype: 'textareafield',
                name: f,
                fieldCls: 'monospace',
                height: config.tab? null: 180,
                growMax: 300,
                listeners: {
                    specialkey: _.bind(function(field, e) {//Ctrl+Enter
                        if (e.getKey() == e.ENTER && e.ctrlKey) {
                            doSave();
                        };
                    }, this),
                },
                grow: config.tab || false,
                value: ch[f],
            });
            activeItems.push(elements[f]);
        };
        if (conf.type == 'check') {//Text area
            elements[f] = panel.add({
                xtype: 'checkboxfield',
                name: f,
                boxLabel: conf.label || 'check',
                checked: ch[f] == conf.value,
                listeners: {
                    specialkey: saveOnEnter,
                },
            });
            if (ch[f] && ch[f] != conf.value) {//Disable
                elements[f].setDisabled(true);
                elements[f].on('afterrender', _.bind(function(item) {//
                    console.log('ch', item);
                    item.bodyEl.on('click', _.bind(function() {
                        item.setDisabled(false);
                    }, this))
                }, this))
            };
            activeItems.push(elements[f]);
        };
        if (conf.type == 'location') {//Text area
            var loc = ch[f] || {};
            if (typeof(loc) == 'string') {
                loc = Ext.JSON.decode(loc, true) || {};
            };
            elements[f] = loc;
            panel.add({
                xtype: 'button',
                text: 'Edit...',
                location: loc,
                handler: _.bind(function(button) {
                    this.editLocation(button.location);
                }, this),
            });
        };
        if (conf.type == 'datetime') {//Text field
            var dt = new Date(parseInt(ch[f]));
            elements[f] = panel.add({
                layout: 'hbox',
                border: false,
                items: [{
                    xtype: 'datefield',
                    flex: 1,
                    listeners: {
                        specialkey: saveOnEnter,
                    },
                    value: dt,
                }, {
                    xtype: 'timefield',
                    flex: 1,
                    listeners: {
                        specialkey: saveOnEnter,
                    },
                    value: dt,
                }],
            });
            activeItems.push(elements[f].items.get(1));
        };
    };
    if (config.tab) {
        this.tabs.add(panel);
        this.tabs.setActiveTab(panel);
    } else {//
        config.parent.add(panel);
        panel.show();
        panel.alignTo(config.element, 'tl-bl?');
        this.checkinPanel = panel;
    };
    if (activeItems.length>0) {//Request focus
        activeItems[0].focus(true, 10);
    };
};

UI.prototype.editTemplate = function(conn, template) {//Creates new tab or jumps to existing
    if (template) {//Search for existing tab
        for (var i = 0; i < this.tabs.items.length; i++) {//Search tab
            var tab = this.tabs.items.get(i);
            if (tab.template_id == template.id) {
                this.tabs.setActiveTab(tab);
                return;
            };
        };
    } else {
        template = {};
    };
    var instance = this;
    Ext.define('Ext.ux.IconField', {
        extend: 'Ext.form.field.Trigger',
        alias: 'widget.iconfield',
        editable: false,
        onTriggerClick: function(e) {//
            instance.showIconDialog(null, _.bind(function(icon) {//
                this.setValue(icon);
            }, this))
        }
    });
    var form = Ext.create('widget.form', {
        tbar: {
            items: [{
                text: 'Save',
                handler: _.bind(function() {//Send form
                    if (form.getForm().isValid()) {//OK to save
                        var tmpl = form.getValues();
                        tmpl.id = template.id;
                        var json = Ext.JSON.decode(tmpl.template, true);
                        tmpl.template = json? Ext.JSON.encode(json): null;
                        var json = Ext.JSON.decode(tmpl.editor, true);
                        tmpl.editor = json? Ext.JSON.encode(json): null;
                        tab.setLoading(true);
                        conn.conn.updateTemplate(tmpl, _.bind(function(err) {
                            tab.setLoading(false);
                            if (err) {
                                Ext.Msg.alert(err);
                            } else {
                                tab.close();
                                //this.reloadTemplates();
                            };
                        }, this))
                    };
                }, this)
            }, {
                text: 'Remove',
                hidden: !template.id,
                handler: _.bind(function() {//
                    tab.setLoading(true);
                    conn.conn.removeTemplate(template, _.bind(function(err) {//
                        tab.setLoading(false);
                        if (err) {
                            Ext.Msg.alert(err);
                        } else {
                            tab.close();
                            //this.reloadTemplates();
                        };
                    }, this))
                }, this)
            }, {
                text: 'Fav',
                enableToggle: true,
                pressed: this.favorites.isFavorite(template.id, this.favorites.TEMPLATES_KEY),
                hidden: !template.id,
                handler: _.bind(function(item) {//
                    this.favorites.manageFavorite(template.id, item.pressed, this.favorites.TEMPLATES_KEY);
                }, this),
            }]
        },
        defaults: {
            anchor: '100%'
        },
        layout: 'anchor',
        defaultType: 'textfield',
        bodyPadding: 5,
        items: [{
            fieldLabel: 'ID',
            xtype: 'displayfield',
            name: 'id',
            value: template.id,
        }, {
            fieldLabel: 'Name',
            name: 'name',
            allowBlank: false,
            value: template.name,
        }, {
            fieldLabel: 'Icon',
            name: 'icon',
            xtype: 'iconfield',
            emptyText: 'No icon selected',
            allowBlank: false,
            value: template.icon,
        }, {
            fieldLabel: 'Tags',
            name: 'tags',
            value: (template.tags || []).join(' '),
        }, {
            fieldLabel: 'Display',
            name: 'display',
            value: template.display,
        }, {
            fieldLabel: 'Template',
            name: 'template',
            xtype: 'textareafield',
            fieldCls: 'monospace',
            growMax: 300,
            grow: true,
            value: Ext.JSON.encode(template.template || {})
        }, {
            fieldLabel: 'Editor',
            name: 'editor',
            xtype: 'textareafield',
            fieldCls: 'monospace',
            growMax: 300,
            grow: true,
            value: Ext.JSON.encode(template.editor || {})
        }],
    });
    var tab = Ext.create('widget.panel', {
        title: template.name || ('New template ('+conn.name+')'),
        closable: true,
        layout: 'fit',
        template_id: template.id,
        items: form
    });
    this.tabs.add(tab);
    this.tabs.setActiveTab(tab);
};

