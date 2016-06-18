(function() {
	function init(){
		var elems = document.getElementsByTagName("time");
		for(var i = 0; i < elems.length; i++){
			elems[i].setAttribute("title", elems[i].textContent);
			elems[i].removeChild(elems[i].firstChild);
			var t = elems[i].getAttribute("datetime");
			elems[i].appendChild(document.createTextNode(new Date(t).toLocaleString(undefined, {timeZoneName: "short",
				year: "numeric",
				month: "2-digit",
				day: "2-digit",
				hour: "2-digit",
				minute: "2-digit",
				second: "2-digit"})
				));
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
