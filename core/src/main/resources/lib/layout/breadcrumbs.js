var breadcrumbs = (function() {
    /**
     * This component actually renders the menu.
     *
     * @type {YAHOO.widget.Menu}
     */
    var menu;

    /**
     * Used for fetching the content of the menu asynchronously from the server
     */
    var xhr;

    function makeMenuHtml(icon,displayName) {
        return (icon!=null ? "<img src='"+icon+"' width=24 height=24 style='margin: 2px;' alt=''> " : "")+displayName;
    }

    Event.observe(window,"load",function(){
      menu = new YAHOO.widget.Menu("breadcrumb-menu", {position:"dynamic", hidedelay:1000});
    });

    jenkinsRules["#breadcrumbs LI"] = function (e) {
        // when the mouse hovers over LI, activate the menu
        $(e).observe("mouseover", function () {
            function showMenu(items) {
                menu.hide();
                menu.cfg.setProperty("context", [e, "tl", "bl"]);
                menu.clearContent();
                menu.addItems(items);
                menu.render("breadcrumb-menu-target");
                menu.show();
            }

            if (xhr)
                xhr.options.onComplete = function () {
                };   // ignore the currently pending call

            if (e.items) {// use what's already loaded
                showMenu(e.items());
            } else {// fetch menu on demand
                xhr = new Ajax.Request(e.firstChild.getAttribute("href") + "contextMenu", {
                    onComplete:function (x) {
                        var a = x.responseText.evalJSON().items;
                        a.each(function (e) {
                            e.text = makeMenuHtml(e.icon, e.displayName);
                        });
                        e.items = function() { return a };
                        showMenu(a);
                    }
                });
            }

            return false;
        });
    };

    /**
     * @namespace breadcrumbs
     * @class ContextMenu
     * @constructor
     */
    var ContextMenu = function () {
        this.items = [];
    };
    ContextMenu.prototype = {
        /**
         * Creates a menu item.
         *
         * @return {breadcrumbs.MenuItem}
         */
        "add" : function (url,icon,displayName) {
            this.items.push({ url:url, text:makeMenuHtml(icon,displayName) });
            return this;
        }
    };

    return {
        /**
         * Activates the context menu for the specified breadcrumb element.
         *
         * @param {String|HTMLElement} li
         *      The LI tag to which you associate the menu (or its ID)
         * @param {Function|breadcrumbs.ContextMenu} menu
         *      Pass in the configured menu object. If a function is given, this function
         *      is called each time a menu needs to be displayed. This is convenient for dynamically
         *      populating the content.
         */
        "attachMenu" : function (li,menu) {
            $(li).items =  (typeof menu=="function") ? menu : function() { return menu.items };
        },

        "ContextMenu" : ContextMenu
    };
})();
