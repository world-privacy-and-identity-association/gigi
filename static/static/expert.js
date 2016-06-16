(function() {
	function showExpert(isExpert)
	{
	  var elements = document.getElementsByClassName("expert");
	  for(var i = 0; elements.length > i; i++)
	  {
	    if(!isExpert) {
	    	elements[i].setAttribute("class","expert experthidden");
	    } else {
	    	elements[i].setAttribute("class","expert");
	    }
	  }
	}
	function init(){
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
