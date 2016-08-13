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
		$(".panel-activatable").map(function() {
			var panel = $(this);
			var refresh = function(){
				var radio = this.type == "radio";
				if(radio && this.form.currentRadios === undefined) {
					this.form.currentRadios = {};
				}
				if(this.checked) {
					panel.find(".panel-body").removeClass("hidden");
					if(radio) {
						var rds = this.form.currentRadios;
						if(rds[this.name] !== undefined){
							$(rds[this.name]).trigger("change");
						}
						rds[this.name] = this;
					}
				} else {
					panel.find(".panel-body").addClass("hidden");
				}
			};
			panel.find(".panel-heading [type=\"checkbox\"]").map(refresh);
			panel.find(".panel-heading [type=\"checkbox\"]").change(refresh);
			panel.find(".panel-heading [type=\"radio\"]").map(refresh);
			panel.find(".panel-heading [type=\"radio\"]").change(refresh);
			return this.id;
		});
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
