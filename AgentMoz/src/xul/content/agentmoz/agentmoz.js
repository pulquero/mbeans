var timerID = null;
var mbeanServer = null;
var refreshing = false;

function init() {
   document.getElementById("agentmoz-iframe").setAttribute("src", pathToURL(getComponentsPath())+"/agentmoz-applet.html");
   timerID = setInterval("checkAgentLoaded()", 1000);
}

function pathToURL(path) {
   return "file:/"+path.replace(/\\/g, "/");
}

function getComponentsPath() {
   var service = Components.classes["@mozilla.org/file/directory_service;1"].getService();
   service.QueryInterface(Components.interfaces.nsIProperties);
   return service.get("ComsD", Components.interfaces.nsIFile).path;
}

function checkAgentLoaded() {
   var mbsSet = Packages.javax.management.MBeanServerFactory.findMBeanServer(null);
   if(!mbsSet.isEmpty()) {
      clearInterval(timerID);
      mbeanServer = mbsSet.get(0);
      refresh();
   }
}

function refresh() {
   if(refreshing || mbeanServer == null)
      return;
   refreshing = true;
   var tree = document.getElementById("mbeanserver-tree");
   tree.parentNode.replaceChild(createDomainTree(), tree);
   document.getElementById("mbean-count").setAttribute("value", mbeanServer.getMBeanCount());
   refreshing = false;
}

function createDomainTree() {
   var tree = document.createElement("treechildren");
   tree.id = "mbeanserver-tree";
   var domains = mbeanServer.getDomains();
   for(var i in domains) {
      var item = document.createElement("treeitem");
      item.setAttribute("container", "true");
      item.setAttribute("open", "true");
      tree.appendChild(item);
      var row = document.createElement("treerow");
      item.appendChild(row);
      var cell = document.createElement("treecell");
      cell.setAttribute("label", domains[i]);
      row.appendChild(cell);
      var names = mbeanServer.queryNames(Packages.javax.management.ObjectName.getInstance(domains[i]+":*"), null);
      var childTree = document.createElement("treechildren");
      item.appendChild(childTree);
      for(var iter = names.iterator(); iter.hasNext(); ) {
         childTree.appendChild(createMBeanItem(iter.next()));
      }
   }
   return tree;
}

function createMBeanItem(name) {
   var item = document.createElement("treeitem");
   item.setAttribute("container", "true");
   var row = document.createElement("treerow");
   item.appendChild(row);
   var cell = document.createElement("treecell");
   cell.setAttribute("label", name.getCanonicalKeyPropertyListString());
   row.appendChild(cell);
   var info = mbeanServer.getMBeanInfo(name);
   var childTree = document.createElement("treechildren");
   item.appendChild(childTree);
   childTree.appendChild(createClassNameItem(info));
   childTree.appendChild(createAttributesItem(name,info.getAttributes()));
   childTree.appendChild(createOperationsItem(name,info.getOperations()));
   return item;
}

function createClassNameItem(info) {
   var item = document.createElement("treeitem");
   var row = document.createElement("treerow");
   item.appendChild(row);
   var cell = document.createElement("treecell");
   cell.setAttribute("label", "class name: "+info.getClassName());
   row.appendChild(cell);
   return item;
}

function createAttributesItem(name,attrs) {
   var item = document.createElement("treeitem");
   item.setAttribute("container", "true");
   var row = document.createElement("treerow");
   item.appendChild(row);
   var cell = document.createElement("treecell");
   cell.setAttribute("label", "Attributes");
   row.appendChild(cell);
   item.appendChild(createAttributesTree(name,attrs));
   return item;
}

function createAttributesTree(name,attrInfos) {
   var attrNames = infosToNames(attrInfos);
   var attrList;
   var len;
   if(name.getKeyProperty("service") != "JavaScript") {
      attrList = mbeanServer.getAttributes(name, attrNames);
      len = attrList.size();
   } else {
      attrList = null;
      len = attrNames.length;
   }
   var tree = document.createElement("treechildren");
   for(var i=0; i<len; i++) {
      var item = document.createElement("treeitem");
      tree.appendChild(item);
      var row = document.createElement("treerow");
      item.appendChild(row);
      var cell = document.createElement("treecell");
      if(attrList != null) {
         var attr = attrList.get(i);
         cell.setAttribute("label", attr.getName()+" = "+attr.getValue());
      } else {
         cell.setAttribute("label", attrNames[i]);
      }
      row.appendChild(cell);
   }
   return tree;
}

function createOperationsItem(name,ops) {
   var item = document.createElement("treeitem");
   item.setAttribute("container", "true");
   var row = document.createElement("treerow");
   item.appendChild(row);
   var cell = document.createElement("treecell");
   cell.setAttribute("label", "Operations");
   row.appendChild(cell);
   item.appendChild(createOperationsTree(name,ops));
   return item;
}

function createOperationsTree(name,opInfos) {
   var tree = document.createElement("treechildren");
   for(var i in opInfos) {
      var item = document.createElement("treeitem");
      tree.appendChild(item);
      var row = document.createElement("treerow");
      item.appendChild(row);
      var cell = document.createElement("treecell");
      cell.setAttribute("label", opInfos[i].getName());
      row.appendChild(cell);
   }
   return tree;
}

function infosToNames(infos) {
   var names = java.lang.reflect.Array.newInstance(java.lang.String, infos.length);
   for(var i in infos) {
      names[i] = infos[i].getName();
   }
   return names;
}

function destroy() {
   if(timerID != null) {
      clearInterval(timerID);
      timerID = null;
   }
   mbeanServer = null;
}
