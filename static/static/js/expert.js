(function() {
	var modal = undefined;
	
	function showModal(content){
		var HTML = `<div class="modal fade" id="confirmation-modal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
  <div class="modal-dialog" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&#x1F5D9;</span></button>
        <h4 class="modal-title" id="myModalLabel">&nbsp;</h4>
      </div>
      <div class="modal-body">
        Body
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default btn-cancel" data-dismiss="modal">&#x1F5D9;</button>
        <button type="button" class="btn btn-danger btn-confirm">&checkmark;</button>
      </div>
    </div>
  </div>
</div>`;
		if(modal === undefined) {
			modal = $(HTML);
			modal.action = function(){};
			modal.appendTo("body");
			$(modal.get(0)).find(".modal-footer .btn-confirm").click(function(){
				modal.action();
				modal.modal("hide");
			});
		}
		var m = $(modal.get(0));
		m.find(".modal-body").text($(content).attr("data-confirm"));
		var reply = $(content).attr("data-reply").split(",");
		m.find(".modal-footer .btn-cancel").text(reply[0]);
		m.find(".modal-footer .btn-confirm").text(reply[1]);
		modal.action=function(){
			content.confirmed=true;
			$(content).click();
		};
		modal.modal("show");
	}
	function initConfirm() {
		$(".btn-confirm").click(function(){
			if(this.confirmed === true){
				return true;
			}
			showModal(this);
			return false;
		});
	}
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
		initConfirm();
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
