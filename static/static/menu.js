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
	function showExpert(a)
	{
	  b=document.getElementsByClassName("expert");
	  for(i=0;b.length>i;i++)
	  {
	    if(!a) {b[i].setAttribute("class","expert experthidden"); }
	    else {b[i].setAttribute("class","expert");}
	  }
	  b=document.getElementsByClassName("expertoff");
	  for(i=0;b.length>i;i++)
	  {
	   b[i].setAttribute("class","");
	  }

	}
	function init(){
		initMenu();
		showExpert(false);
		var expert = document.getElementById("expertbox");
		if(expert !== null) {
			expert.onchange = (function(expert){return function(){showExpert(expert.checked)}})(expert);
		}
	}
	(function(oldLoad) {
		if (oldLoad == undefined) {
			window.onload = init;
		} else {
			window.onload = function() {
				init();
				oldLoad();
			}
		}
	})(window.onload);

})();
