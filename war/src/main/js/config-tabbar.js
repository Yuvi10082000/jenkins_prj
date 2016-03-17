var $ = require('jquery-detached').getJQuery();
var jenkinsLocalStorage = require('./util/jenkinsLocalStorage.js');
var configMetadata = require('./widgets/config/model/ConfigTableMetaData.js');

$(function() {
    // Horrible ugly hack...
    // We need to use Behaviour.js to wait until after radioBlock.js Behaviour.js rules
    // have been applied, otherwise row-set rows become visible across sections.
    var done = false;
    
    Behaviour.specify(".dd-handle", 'config-drag-start',1000,fixDragEvent); // jshint ignore:line
    Behaviour.specify(".block-control", 'row-set-block-control', 1000, function() { // jshint ignore:line
        if (done) {
            return;
        }
        done = true;
        // Only do job configs for now.
        var configTables = $('.job-config.tabbed');
        if (configTables.size() > 0) {
            var tabBarShowPreferenceKey = 'config:usetabs';
            var tabBarShowPreference = jenkinsLocalStorage.getGlobalItem(tabBarShowPreferenceKey, "yes");
            
            fixDragEvent(configTables);
            
            var tabBarWidget = require('./widgets/config/tabbar.js');
            if (tabBarShowPreference === "yes") {
                configTables.each(function() {
                    var configTable = $(this);
                    var tabBar = tabBarWidget.addTabs(configTable);

                    // We want to merge some sections together.
                    // Merge the "Advanced" section into the "General" section.
                    var generalSection = tabBar.getSection('config_general');
                    if (generalSection) {
                        generalSection.adoptSection('config_advanced_project_options');
                    }

                    addFinderToggle(tabBar);
                    tabBar.onShowSection(function() {
                        // Hook back into hudson-behavior.js
                        fireBottomStickerAdjustEvent();
                    });
                    tabBar.deactivator.click(function() {
                        jenkinsLocalStorage.setGlobalItem(tabBarShowPreferenceKey, "no");
                        require('window-handle').getWindow().location.reload();
                    });
                    $('.jenkins-config-widgets .find-container input').focus(function() {
                        fireBottomStickerAdjustEvent();
                    });

                    if (tabBar.hasSections()) {
                        var tabBarLastSectionKey = 'config:' + tabBar.configForm.attr('name') + ':last-tab';
                        var tabBarLastSection = jenkinsLocalStorage.getPageItem(tabBarLastSectionKey, tabBar.sections[0].id);
                        tabBar.onShowSection(function() {
                            jenkinsLocalStorage.setPageItem(tabBarLastSectionKey, this.id);
                        });
                        tabBar.showSection(tabBarLastSection);
                    }
                    watchScroll(tabBar);
                    $(window).on('scroll',function(){watchScroll(tabBar);});
                });
                
            } else {
                configTables.each(function() {
                    var configTable = $(this);
                    var activator = tabBarWidget.addTabsActivator(configTable);
                    configMetadata.markConfigTableParentForm(configTable);
                    activator.click(function() {
                        jenkinsLocalStorage.setGlobalItem(tabBarShowPreferenceKey, "yes");
                        require('window-handle').getWindow().location.reload();
                    });
                });
            }
        }
    });
    
    
});

function addFinderToggle(configTableMetadata) {
    var findToggle = $('<div class="find-toggle" title="Find"></div>');
    var finderShowPreferenceKey = 'config:showfinder';
    
    findToggle.click(function() {
        var findContainer = $('.find-container', configTableMetadata.configWidgets);
        if (findContainer.hasClass('visible')) {
            findContainer.removeClass('visible');
            jenkinsLocalStorage.setGlobalItem(finderShowPreferenceKey, "no");
        } else {
            findContainer.addClass('visible');
            $('input', findContainer).focus();
            jenkinsLocalStorage.setGlobalItem(finderShowPreferenceKey, "yes");
        }
    });
    
    if (jenkinsLocalStorage.getGlobalItem(finderShowPreferenceKey, "yes") === 'yes') {
        findToggle.click();
    }
}

function watchScroll(tabControl){
  var $window = $(window);
  var $tabBox= tabControl.configWidgets;
  var $tabs = $tabBox.find('.tab');
  var $table= tabControl.configTable;
  var $jenkTools = $('#breadcrumbBar');
  var winScoll = $window.scrollTop(); 
  var categories = tabControl.sections;
  var jenkToolOffset = ($jenkTools.height() + $jenkTools.offset().top);

	// reset tabs to start...  
  $tabs.find('.active').removeClass('active');
  
  function getCatTop($cat){
    return($cat.length > 0)?
        $cat.offset().top - jenkToolOffset
        :0;
  }
	// calculate the top and height of each section to know where to switch the tabs...
  $.each(categories,function(i,cat){
    var $cat = $(cat.headerRow);
    var $nextCat = (i+1 <categories.length)? 
        $(categories[i+1].headerRow):
          $cat;
    // each category enters the viewport at its distance down the page, less the height of the toolbar, which hangs down the page...
    // or it is zero if the category doesn't match or was removed...
    var catTop = getCatTop($cat);
    // height of this one is the top of the next, less the top of this one.
    var catHeight = getCatTop($nextCat) - catTop;

		// the trigger point to change the tab happens when the scroll position passes below the height of the category...
		// ...but we want to wait to advance the tab until the existing category is 75% off the top...
    if(winScoll < (catTop + (0.75 * catHeight))){
      var $thisTab = $($tabs.get(i));
      var $nav = $thisTab.closest('.tabBar');
      $nav.find('.active').removeClass('active');
      $thisTab.addClass('active');
      return false;
    }
  });

  if(winScoll > $('#page-head').height() - 5 ){  
    $tabBox.width($tabBox.width()).css({
      'position':'fixed',
      'top':($jenkTools.height() - 5 )+'px'});
    $table.css({'margin-top':$tabBox.outerHeight()+'px'});
    
  }
  else{
    $tabBox.add($table).removeAttr('style');
  }
}

function fireBottomStickerAdjustEvent() {
    Event.fire(window, 'jenkins:bottom-sticker-adjust'); // jshint ignore:line
}
// YUI Drag widget does not like to work on elements with a relative position.
// This tells the element to switch to static position at the start of the drag, so it can work.
function fixDragEvent(handle){
    var isReady = false;
    var $handle = $(handle);
    var $chunk = $handle.closest('.repeated-chunk');
    $handle.add('#ygddfdiv')
    	.mousedown(function(){
    	    isReady = true; 
    	})
    	.mousemove(function(){
    	    if(isReady && !$chunk.hasClass('dragging')){
    		$chunk.addClass('dragging');
    	    }
    	}).mouseup(function(){
    	    isReady = false;
    	    $chunk.removeClass('dragging');
    	});
}
