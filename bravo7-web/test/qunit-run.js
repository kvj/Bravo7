var extJSTest = function(test, title) {//Opens new window with test results
    var win = Ext.create('widget.window', {
        title: 'Testing',
        maximized: true,
        layout: 'fit',
        items: {
            html: '<div style="overflow: auto; width: 100%; height: 100%;"><h1 id="qunit-header">'+(title || 'No title')+'</h1><h2 id="qunit-banner"/><h2 id="qunit-userAgent"/><ol id="qunit-tests"/><p id="qunit-testresult"/></div>',
        }
    });
    win.show();
    test();
};

var runOrTest = function(run, test, title) {//Run app or tests
    if (_.endsWith(document.location.toString(), '?test')) {//Run tests
        $(document.body).append($('<h1 id="qunit-header"/>').text(title || ''));
        $(document.body).append('<h2 id="qunit-banner"/>');
        $(document.body).append('<h2 id="qunit-userAgent"/>');
        $(document.body).append('<ol id="qunit-tests"/>');
        if (test) {
            test();
        };
    } else {//Normal run
        if (run) {
            run();
        };
    };
};
