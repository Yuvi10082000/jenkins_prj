import pluginManagerAvailable from './templates/plugin-manager/available.hbs'

var debounce = null;

function applyFilter(searchQuery) {
    clearTimeout(debounce);
    // debounce reduces number of server side calls while typing
    debounce = setTimeout(function () {
        view.availablePlugins(searchQuery.toLowerCase().trim(), 50, function (pluginsObj) {
            var plugins = JSON.parse(pluginsObj.responseObject());
            var pluginsTable = document.getElementById('plugins');
            var tbody = pluginsTable.querySelector('tbody');
            var selectedPlugins = []
            var admin = pluginsTable.dataset.hasadmin === 'true';
            
            function clearOldResults() {
                if (!admin) {
                    tbody.innerHTML = '';
                } else {
                    var rows = tbody.querySelectorAll('tr');
                    if (rows) {
                        selectedPlugins = []
                        rows.forEach(function (row) {
                            var input = row.querySelector('input');
                            if (input.checked === true) {
                                var pluginName = input.name.split('.')[1]
                                selectedPlugins.push(pluginName)
                            } else {
                                row.remove();  
                            }
                        })
                    }
                }
            }
            
            clearOldResults()
            var rows = pluginManagerAvailable({
                plugins: plugins.filter(plugin => !selectedPlugins.includes(plugin.name)),
                admin 
            });

            tbody.insertAdjacentHTML('beforeend', rows);
        })
    }, 150);
}

document.addEventListener("DOMContentLoaded", function () {
    var filterInput = document.getElementById('filter-box');
    filterInput.addEventListener('input', function (e) {
        applyFilter(e.target.value)
    });

    applyFilter(filterInput.value);

    setTimeout(function () {
        layoutUpdateCallback.call();
    }, 350)

});
