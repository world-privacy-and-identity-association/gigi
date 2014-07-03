(function() {
	function explodeMenu(e) {
		if (document.getElementById(e).className == 'menu hidden') {
			document.getElementById(e).className = 'menu';
		} else {
			document.getElementById(e).className = 'menu hidden';
		}
	}

	function initMenu() {
		var Nodes = document.getElementsByTagName('ul');
		var max = Nodes.length;
		for (var i = 0; i < max; i++) {
			var nodeObj = Nodes.item(i);
			if (nodeObj.className.indexOf("menu") > -1 && nodeObj.id != "recom") {
				nodeObj.previousSibling.previousSibling.onclick = (function(nid) {
					return function() {
						explodeMenu(nid);
					};
				})(nodeObj.id);
			}
		}
	}
	(function(oldLoad) {
		if (oldLoad == undefined) {
			window.onload = initMenu;
		} else {
			window.onload = function() {
				initMenu();
				oldLoad();
			}
		}
	})(window.onload);

})();