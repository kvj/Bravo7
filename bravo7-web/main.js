Ext.onReady(function() {//
    var viewport = Ext.create('Ext.container.Viewport', {
        renderTo: Ext.getBody(),
        layout: 'fit',
        items: [],
    });
    var sessionManager = new SessionManager(viewport);
    sessionManager.show();
});

var SessionManager = function(root) {
    this.root = root;
    this.loginPrefix = '/login';
    this.tokenPrefix = '/token';
    Ext.define('Session', {
        extend: 'Ext.data.Model',
        fields: [
            {name: 'id', type: 'int'},
            {name: 'name', type: 'string'},
            {name: 'rootURL', type: 'string'},
            //{name: 'loginURL', type: 'string'},
            //{name: 'tokenURL', type: 'string'},
            {name: 'active', type: 'boolean', defaultValue: false},
            {name: 'accessToken', type: 'string'},
        ],
    });
    this.store = new Ext.data.Store({
        model: 'Session',
        autoLoad: true,
        autoSync: true,
        proxy: {
            type: 'localstorage',
            id: 'sessions',
        },
    });
    //setTimeout(_.bind(this.load, this), 1000);
};

SessionManager.prototype.edit = function(session, handler) {//Shows edit window
    console.log('edit', session);
    var form = Ext.create('widget.form', {
        defaults: {
            anchor: '100%'
        },
        layout: 'anchor',
        defaultType: 'textfield',
        bodyPadding: 5,
        items: [
            {
                fieldLabel: 'Name',
                name: 'name',
                allowBlank: false,
            }, {
                fieldLabel: 'Root URL',
                name: 'rootURL',
                allowBlank: false,
            }, {
                //fieldLabel: 'Login URL',
                //name: 'loginURL',
                //allowBlank: false,
            //}, {
                //fieldLabel: 'Token URL',
                //name: 'tokenURL',
                //allowBlank: false,
            //}, {
                fieldLabel: 'Username',
                name: 'username',
            }, {
                fieldLabel: 'Password',
                inputType: 'password',
                name: 'password',
            }
        ],
        buttons: [{
            text: 'Save',
            handler: _.bind(function() {//Update record
                var username = form.getValues().username;
                var password = form.getValues().password;
                form.getForm().updateRecord(session);
                win.close();
                handler(true, username, password);
            }, this),
        }, {
            text: 'Remove',
            handler: _.bind(function() {//
                this.store.remove(session);
                win.close();
                handler(false);
            }, this),
        }],
    });
    var win = Ext.create('widget.window', {
        title: 'Session',
        width: 400,
        //height: 'auto',
        layout: 'fit',
        items: form
    });
    win.show();
    form.loadRecord(session);
};

SessionManager.prototype.afterUpdate = function(row, username, password, handler) {//
    console.log('afterUpdate', row, username, password);
    var _handler = _.bind(function(err, token) {//
        console.log('afterUpdate result', err, token);
        if (token) {
            row.set('accessToken', token);
            handler(null);
        } else {
            handler('Error obtaining token: '+err);
        }
    }, this);
    var url = row.get('rootURL');
    if (username && password) {
        oauthGetAccessTokenUsername(url+this.loginPrefix, oauthClientID, oauthClientSecret, username, password, _handler);
    } else {
        this.getAccessToken(url+this.loginPrefix, url+this.tokenPrefix, oauthClientID, oauthClientSecret, _handler);
    }
};

SessionManager.prototype.load = function() {
    var dataProvider = new DataProvider();
    var arr = [];
    for (var i = 0; i < this.store.count(); i++) {
        var user = this.store.getAt(i);
        if (user.get('active')) {//only those are active
            arr.push({url:user.get('rootURL') , token: user.get('accessToken'), name: user.get('name')})
        };
    };
    //log('load users:', arr.length);
    this.window.setLoading(true);
    dataProvider.startConnections(arr, _.bind(function(err) {
        this.window.setLoading(false);
        if (err) {
            Ext.Msg.alert('Error', err);
        } else {//Start main UI
            this.window.close();
            new UI(this.root, dataProvider);
        };
    }, this))
};

SessionManager.prototype.show = function() {//Shows window
    var grid = Ext.create('widget.grid', {
        border: false,
        columns: [{
            width: 20,
            getClass: function(v, data, r) {
                return 'session_icon '+(r.get('active')? 'session_active': 'session_passive');
            },
            handler: function(grid, rowIndex, colIndex) {
                var rec = grid.getStore().getAt(rowIndex);
                rec.set('active', rec.get('active')? false: true);
                //rec.save();
            },
            xtype: 'actioncolumn',
            dataIndex: 'active',
        }, {
            header: 'Session', 
            xtype: 'templatecolumn', 
            tpl: '{name} - {rootURL}', 
            flex: 1
        }],
        store: this.store,
    });
    grid.on('itemdblclick', _.bind(function(grid, row) {
        this.edit(row, _.bind(function(updated, username, password) {//
            if (updated) {//
                this.afterUpdate(row, username, password, function(err) {
                    if (err) {
                        Ext.Msg.alert('Error', err);
                    }
                });
            };
        }, this));
    }, this));
    var continueButton = Ext.create('widget.button', {
        text: 'Continue',
        handler: _.bind(function() {//Removes session
            this.load();
        }, this),
    });
    var autoStartTimeout = 3;
    var autoStartTimeoutID = setInterval(_.bind(function() {//
        if (autoStartTimeout>0) {
            autoStartTimeout--;
        };
        if (autoStartTimeout == 0 && autoStartTimeoutID) {
            clearInterval(autoStartTimeoutID);
            autoStartTimeoutID = null;
            this.load();
        };
        continueButton.setText('Continue '+autoStartTimeout);
    }, this), 1000);
    this.window = Ext.create('Ext.window.Window', {
        title: 'Sessions',
        width: 400,
        modal: true,
        closable: false,
        height: 400,
        layout: 'fit',
        items: grid,
        buttons: [
            continueButton,
            {
                text: 'Test',
                handler: _.bind(function() {//Run tests
                    extJSTest(_.bind(function() {//
                        var user = this.store.getAt(0);
                        if (!user || !user.get('accessToken')) {
                            Ext.Msg.alert('Error', 'No session');
                            return;
                        };
                        testDataManager(user.get('rootURL'), user.get('accessToken'));
                    }, this), 'Bravo 7 tests');
                }, this),
            },
            {
                text: 'New session',
                handler: _.bind(function() {//Start new session
                    var row = this.store.add({})[0];
                    row.set('active', true);
                    this.edit(row, _.bind(function(updated, username, password) {//
                        if (updated) {//Obtain token
                            this.afterUpdate(row, username, password, _.bind(function(err) {
                                if (err) {
                                    Ext.Msg.alert('Error', err);
                                    this.store.remove(row);
                                }
                            }, this));
                        };
                    }, this));
                }, this)
            },
        ],
    });
    this.window.show();
    this.window.getTargetEl().on('click', _.bind(function() {
        if (autoStartTimeoutID) {//
            clearInterval(autoStartTimeoutID);
            autoStartTimeoutID = null;
        };
    }, this))
};

SessionManager.prototype.getAccessToken = function(loginURL, tokenURL, clientID, clientSecret, handler) {
    var url = oauthGetLoginURL(loginURL, clientID, clientSecret);
    var codeText = Ext.create('Ext.form.field.Text', {
        region: 'center',
        blankText: 'Enter code here',
    });
    var iframePanel = Ext.create('Ext.panel.Panel', {
        region: 'center',
        html: '<iframe src="'+loginURL+'"/>'
    });
    var win = Ext.create('Ext.window.Window', {
        title: 'OAuth login',
        modal: true,
        width: 300,
        height: 300,
        layout: 'border',
        tbar: {
            xtype: 'panel',
            layout: 'border',
            height: 'auto',
            border: 2,
            rbar: {
                xtype: 'button',
                text: 'Verify code',
                handler: _.bind(function() {//
                    oauthGetAccessToken(codeText.getValue(), tokenURL, clientID, clientSecret, _.bind(function(err, token) {
                        if (!err) {
                            win.close();
                        };
                        handler(err, token);
                    }, this));
                }, this),
            },
            items: codeText,
        },
        items: iframePanel,
    });
    win.show();
    win.setWidth(win.width);
};

