// Initialize all modules by requiring them. Also makes sure they get bundled (see gulpfile.js).
var $jq = require('jquery-detached').getJQuery();
var jenkinsLocalStorage = require('./util/jenkinsLocalStorage.js');
var itemTypes = [
    {
      order : 0,
      id : 'basic',
      display : 'Basic items',
      description : 'Traditional Jenkins items, typically job like entities for Jenkins to process in accordance with its configuration settings.',
      instances : [
          'hudson.model.FreeStyleProject', 'hudson.maven.MavenModuleSet', 'org.jenkinsci.plugins.workflow.job.WorkflowJob',
          'com.infradna.hudson.plugins.backup.BackupProject', 'hudson.model.ExternalJob', 'com.cloudbees.jenkins.plugins.longrunning.LongRunningProject',
          'hudson.matrix.MatrixProject'
      ]
    },
    {
      order : 1,
      id : 'folders',
      display : 'Folders and containers',
      description : 'Folders and other item types that themselves contain child items.',
      remainders:'basic',
      instances : [
          'com.cloudbees.hudson.plugins.folder.Folder', 
          'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject',
          'jenkins.branch.OrganizationFolder.com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMNavigator',
          'jenkins.branch.OrganizationFolder.org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator'
      ]
    },/*
       * { id:'pipelines', display:'Pipelines and processes',
       * description:'Complex pipelines and jobs that run through multiple
       * stages beyond the steps fascilitated by simple jobs.', instances:[
       * 'org.jenkinsci.plugins.workflow.job.WorkflowJob',
       * 'com.cloudbees.plugins.flow.BuildFlow',
       * 'org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject',
       * 'jenkins.branch.OrganizationFolder.com.cloudbees.jenkins.plugins.bitbucket.BitbucketSCMNavigator',
       * 'jenkins.branch.OrganizationFolder.org.jenkinsci.plugins.github_branch_source.GitHubSCMNavigator' ] },
       */
    {
      order : 7,
      id : 'copy',
      display : 'Copy existing item',
      placement:'special-single',
      minToShow:0,
      description : 'Use an existing item as a starting point for creating a new item configuration.',
      instances : [
        'copy'
      ]
    },
    {
      order : 3,
      id : 'templates',
      display : 'Template creators',
      description : 'Items that allow for the parameerized creation of reusable core system items or components of those items.',
      remainders:'basic',
      instances : [
          'com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobTemplate', 
          'com.cloudbees.hudson.plugins.modeling.impl.folder.FolderTemplate',
          'com.cloudbees.hudson.plugins.modeling.impl.auxiliary.AuxModel', 
          'com.cloudbees.hudson.plugins.modeling.impl.builder.BuilderTemplate',
          'com.cloudbees.hudson.plugins.modeling.impl.publisher.PublisherTemplate'

      ]
    }, {
      order : -4,
      id : 'user-templates',
      display : 'Custom templates',
      placement:'special-group',
      minToShow:1,
      parents : [
          {
            id : 'folderTemplate',
            display : 'Folder template',
            description : 'A template for creating parameterized folders',
            parent : 'com.cloudbees.hudson.plugins.modeling.impl.jobTemplate.JobTemplate'
          }, {
            id : 'jobTemplate',
            display : 'Job template',
            description : 'A template for creating parameterized jobs',
            parent : 'com.cloudbees.hudson.plugins.modeling.impl.folder.FolderTemplate'
          }
      ],
      instances : function() {
      }
    }
];

$jq(function() {
  var $ = $jq;
  var defaultMinToShow = 2;
  var $root = $jq('#main-panel');
  var $form = $root.find('form[name="createItem"]').addClass('jenkins-config');
  var $table = $form.find('table.createItem');
  var $newView = $jq('<div class="new-view" />').insertBefore($form);
  var $tabs = $('<div class="jenkins-config-widgets" />').appendTo($newView);
  var $categories = $('<div class="categories" />').appendTo($newView);
  var sectionsToShow = [];

  function sortItemsByOrder(itemTypes) {
    function sortByOrder(a, b) {
      var aOrder = a.order;
      var bOrder = b.order;
      return ( (aOrder < bOrder) ? -1 : ( (aOrder > bOrder) ? 1 : 0));
    }

    return itemTypes.sort(sortByOrder);
  }

  function getCategory(i, elem) {
    var category = {
      id : 'j-add-item-type-' + elem.id,
      display : elem.display,
      description : elem.description,
      minToShow : elem.minToShow,
      remainders : elem.remainders,
      items : []
    };
    if (typeof i === 'string')
      elem = i;
    var $ = $jq;
    if (!elem.instances)
      return;
    if ($.isFunction(elem.instances))
      elem.instances();
    else
      $.each(elem.instances, function(i, elem) {
        category.items.push(getInstancesOfCategory(i, elem));
      });

    return category;
  }

  function getInstancesOfCategory(i, elem) {
    if (typeof i === 'string')
      elem = i;
    var $r = $form.find('input[type="radio"][value="' + elem + '"]');
    var $tr = $r.closest('tr');
    var $desc = $tr.next();
    var $error = $tr.nextUntil('.validation-error-area');
    var inputObj = {
      display : $tr.find('label').text(),
      description : $desc.find('.setting-main').html(),
      $r : $r,
      $error : $error,
      icon : elem + '.png'
    }
    if ($tr.length === 1)
      return inputObj;
    else
      return null;

  }

  function makeModel(itemTypes) {
    var categories = [];
    sortItemsByOrder(itemTypes);
    $jq.each(itemTypes, function(i, elem) {
      categories.push(getCategory(i, elem));
    });
    return categories;
  }

  function cleanHref(id,reverse){
    if(reverse){
      var gotHash = (id.indexOf('#') === 0)? 
         '#j-add-item-type-'+ id.substring(1):
           'j-add-item-type-'+ id;
      return gotHash;
    }
    else
      return id.replace('j-add-item-type-','');
  }
  function checkCatCount(elem){
    var minToShow = (typeof elem.minToShow === 'number')?elem.minToShow: 9999999;
    return ($.isArray(elem.items) && elem.items.length >= Math.min(minToShow,defaultMinToShow));
  }
  var data = makeModel(itemTypes);
  
  
  
  
  
  
  
  

  
  function resetActiveTab($this){
    var $nav = $this.closest('.nav');
    $nav.find('.active').removeClass('active');
    $this.addClass('active');
  }  
  
  function cleanLayout(){
    // Do a little shimmy-hack to force legacy code to resize correctly and set tab state.
    $('html,body').animate({scrollTop: 1}, 1);
    $('html,body').animate({scrollTop: 0}, 10);

    setTimeout(fireBottomStickerAdjustEvent,410);
    setTimeout(setTabIndex,100);

  }
  
  function hideAllTabsIfUnnecesary(sectionsToShow){
    if(sectionsToShow.length < 2){
      $tabs.find('.tab').hide();
      $categories.find('.category-header').hide();
    }
      
  }
  
  function setTabIndex(){
    $('footer a').attr('tabindex',10);
    $('#page-head a').attr('tabindex',5);
    $tabs.find('input, a').attr('tabindex',0);
    $categories.find('input[type="radio"]').attr('tabindex',0);
    $('#bottom-sticker').find('button').attr('tabindex',0);  
    $categories.find('input[type="text"]').attr('tabindex',1);
    $categories.find('a').attr('tabindex',1);
  }
  
  function watchScroll(e,elem,b,c){
    var $window = $(window);
    var toolbarFromTop = $tabs.offset().top;
    var $jenkTools = $('#breadcrumbBar')
    var winScoll = $window.scrollTop();
    var jenkToolOffset = $jenkTools.height() + $jenkTools.offset().top + 15;
 
    $tabs.find('.active').removeClass('active');
    $.each(data,function(i,cat){
      var domId = '#'+cat.id;
      var $cat = $(domId);
      var catHeight = ($cat.length > 0)?
          $cat.offset().top + $cat.outerHeight() - (jenkToolOffset + 100):
            0;
      if(winScoll < catHeight){
        var $thisTab = $tabs.find(['[href="',cleanHref(domId),'"]'].join(''));
        resetActiveTab($thisTab);
        return false;
      }
    });
    
    if(winScoll > $('#page-head').height() - 5 ){  
      $tabs.width($tabs.width()).css({
        'position':'fixed',
        'top':($jenkTools.height() - 5 )+'px'});
      $categories.css({'margin-top':$tabs.outerHeight()+'px'});
      
    }
    else{
      $tabs.add($categories).removeAttr('style');
    }
  }

  
  function drawTab(i,elem){
    if(!elem) elem = i;
    var $tab = $(['<li><a class="tab" href="#',cleanHref(elem.id),'">',elem.display,'</a></li>'].join(''))
      .click(function(e){
        //e.preventDefault(e);
        var $this = $(this).children('a');
        
        var tab = $this.attr('href');
        var scrollTop = $(cleanHref(tab,true)).offset().top - ($newView.children('.jenkins-config-widgets').height() + 15);
        
        setTimeout(function(){resetActiveTab($this)},510);
        
        $('html,body').animate({
          scrollTop: scrollTop
        }, 500);
      });
    return $tab;
  }
  
  function drawTabs(data){
    $('body').addClass('add-item');
    setTimeout(function(){$('body').addClass('hide-side');},200);
    $('#main-panel').addClass('container');
    var $navBox = $('<nav class="navbar navbar-default navbar-static form-config tabBarFrame"/>');
    var $nav = $('<ul class="nav navbar-nav tabBar config-section-activators" />');
    
    $.each(data,function(i,elem){
      if(!checkCatCount(elem)) return;
      // little bit hacky here... need to keep track if I have tabs to show, so if there is just 1, I can hide it later....
      else if (elem.minToShow !== 0) sectionsToShow.push(elem.id);
      
      $nav.append(drawTab(elem));
    });
    $(window).on('scroll',watchScroll);
    $navBox.append($nav);
    $tabs.prepend($navBox);
    
    cleanLayout();
  }
  
  function drawName() {
    var $name = $('<div class="j-add-item-name" />');

    var $input = $('<input type="text" placeholder="New item name..." />')
      .change(function(){
        $form.find('input[name="name"]').val($(this).val());
        updateOk($form[0]);
      })
      .appendTo($name);
    
    $tabs.prepend($name);
    setTimeout(function(){$input.focus();},100);
  }
  
  function drawItems(data) {
    var $ = $jq;

    $.each(data, function(i, elem) {
      var $category = $('<div/>').addClass('category jenkins-config hide-cat').attr('id', elem.id);
      var $items = $('<ul/>').addClass('j-item-options').appendTo($category);
      var $newTarget;
      
      if(checkCatCount(elem)){
        var $catHeader = $('<div class="category-header" />').prependTo($category);
        $([
            '<h2>', elem.display, '</h2>'
        ].join('')).appendTo($catHeader);
        $([
            '<p>', elem.description, '</p>'
        ].join('')).appendTo($catHeader);
        
        $category.removeClass('hide-cat');
      }
      else if(elem.remainders){
        $newTarget = $('#'+cleanHref(elem.remainders,true)).find('.j-item-options');
      }

      $.each(elem.items, function(i, elem) {
        if (!elem)
          return;
        var $item = $([
            '<li><label><input name="add-item-display-radio" type="radio" /> <span class="label">', elem.display, '</span></label></li>'
        ].join('')).append([
            '<div class="desc">', elem.description, '</div>'
        ].join('')).append([
            '<div class="icn"><img src="', elem.icon, '" /></div>'
        ].join(''));
        
        function setSelectState(e){
          var $this = $(this).closest('li');
          //if this is a hyperlink, don't move the selection.
          if($this.find('a:focus').length === 1) return false;
          $this.closest('.categories').find('.active').removeClass('active');
          $this.addClass('active');
          elem.$r.attr('checked', 'checked');
          $this.find('input[type="radio"]').attr('checked', 'checked');
          updateOk($form[0]);
          
          $('html, body').animate({
            scrollTop:$this.offset().top - 200
          },50);
          
        }
        
        $item.click(setSelectState);
        // so keyboard tabs will work...
        $item.find('input[type="radio"]').focus(setSelectState);
        
        
        if($newTarget && $newTarget.length === 1)
          $newTarget.append($item);
        else
          $items.append($item);
      });
      
      $categories.append($category);

    });
    
    hideAllTabsIfUnnecesary(sectionsToShow);
    
    $root.prepend($newView);
    
    
    
  }
  drawTabs(data);
  drawName();
  drawItems(data);

});

function fireBottomStickerAdjustEvent() {
  Event.fire(window, 'jenkins:bottom-sticker-adjust'); // jshint ignore:line
}
