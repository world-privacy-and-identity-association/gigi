(function() {
	function explodeMenu(e) {
		if (e.className == 'menu hidden') {
			e.className = 'menu';
		} else {
			e.className = 'menu hidden';
		}
	}

	function initMenu() {
		var Nodes = document.getElementsByTagName('ul');
		var max = Nodes.length;
		for (var i = 0; i < max; i++) {
			var nodeObj = Nodes.item(i);
			if (nodeObj.className.indexOf("menu") > -1 && nodeObj.id != "recom") {
				nodeObj.previousSibling.previousSibling.onclick = (function(node) {
					return function() {
						explodeMenu(node);
					};
				})(nodeObj);
			}
		}
	}

	(function(oldLoad) {
		if (oldLoad == undefined) {
			window.onload = init;
		} else {
			window.onload = function() {
				initMenu();
				oldLoad();
			}
		}
	})(window.onload);

})();
