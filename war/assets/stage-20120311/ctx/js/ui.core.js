(function(c){c.ui={plugin:{add:function(e,f,h){var g=c.ui[e].prototype;for(var d in h){g.plugins[d]=g.plugins[d]||[];g.plugins[d].push([f,h[d]])}},call:function(d,f,e){var h=d.plugins[f];if(!h){return}for(var g=0;g<h.length;g++){if(d.options[h[g][0]]){h[g][1].apply(d.element,e)}}}},cssCache:{},css:function(d){if(c.ui.cssCache[d]){return c.ui.cssCache[d]}var f=c('<div class="ui-gen">').addClass(d).css({position:"absolute",top:"-5000px",left:"-5000px",display:"block"}).appendTo("body");c.ui.cssCache[d]=!!((!(/auto|default/).test(f.css("cursor"))||(/^[1-9]/).test(f.css("height"))||(/^[1-9]/).test(f.css("width"))||!(/none/).test(f.css("backgroundImage"))||!(/transparent|rgba\(0, 0, 0, 0\)/).test(f.css("backgroundColor"))));try{c("body").get(0).removeChild(f.get(0))}catch(g){}return c.ui.cssCache[d]},disableSelection:function(d){c(d).attr("unselectable","on").css("MozUserSelect","none")},enableSelection:function(d){c(d).attr("unselectable","off").css("MozUserSelect","")},hasScroll:function(h,f){var d=/top/.test(f||"top")?"scrollTop":"scrollLeft",g=false;if(h[d]>0){return true}h[d]=1;g=h[d]>0?true:false;h[d]=0;return g}};var b=c.fn.remove;c.fn.remove=function(){c("*",this).add(this).triggerHandler("remove");return b.apply(this,arguments)};function a(e,f,g){var d=c[e][f].getter||[];d=(typeof d=="string"?d.split(/,?\s+/):d);return(c.inArray(g,d)!=-1)}c.widget=function(e,d){var f=e.split(".")[0];e=e.split(".")[1];c.fn[e]=function(j){var h=(typeof j=="string"),i=Array.prototype.slice.call(arguments,1);if(h&&a(f,e,j)){var g=c.data(this[0],e);return(g?g[j].apply(g,i):undefined)}return this.each(function(){var k=c.data(this,e);if(h&&k&&c.isFunction(k[j])){k[j].apply(k,i)}else{if(!h){c.data(this,e,new c[f][e](this,j))}}})};c[f][e]=function(i,h){var g=this;this.widgetName=e;this.widgetBaseClass=f+"-"+e;this.options=c.extend({},c.widget.defaults,c[f][e].defaults,h);this.element=c(i).bind("setData."+e,function(l,j,k){return g.setData(j,k)}).bind("getData."+e,function(k,j){return g.getData(j)}).bind("remove",function(){return g.destroy()});this.init()};c[f][e].prototype=c.extend({},c.widget.prototype,d)};c.widget.prototype={init:function(){},destroy:function(){this.element.removeData(this.widgetName)},getData:function(d){return this.options[d]},setData:function(d,e){this.options[d]=e;if(d=="disabled"){this.element[e?"addClass":"removeClass"](this.widgetBaseClass+"-disabled")}},enable:function(){this.setData("disabled",false)},disable:function(){this.setData("disabled",true)}};c.widget.defaults={disabled:false};c.ui.mouse={mouseInit:function(){var d=this;this.element.bind("mousedown."+this.widgetName,function(f){return d.mouseDown(f)});if(c.browser.msie){this._mouseUnselectable=this.element.attr("unselectable");this.element.attr("unselectable","on")}this.started=false},mouseDestroy:function(){this.element.unbind("."+this.widgetName);(c.browser.msie&&this.element.attr("unselectable",this._mouseUnselectable))},mouseDown:function(g){(this._mouseStarted&&this.mouseUp(g));this._mouseDownEvent=g;var f=this,h=(g.which==1),d=(typeof this.options.cancel=="string"?c(g.target).parents().add(g.target).filter(this.options.cancel).length:false);if(!h||d||!this.mouseCapture(g)){return true}this._mouseDelayMet=!this.options.delay;if(!this._mouseDelayMet){this._mouseDelayTimer=setTimeout(function(){f._mouseDelayMet=true},this.options.delay)}if(this.mouseDistanceMet(g)&&this.mouseDelayMet(g)){this._mouseStarted=(this.mouseStart(g)!==false);if(!this._mouseStarted){g.preventDefault();return true}}this._mouseMoveDelegate=function(i){return f.mouseMove(i)};this._mouseUpDelegate=function(i){return f.mouseUp(i)};c(document).bind("mousemove."+this.widgetName,this._mouseMoveDelegate).bind("mouseup."+this.widgetName,this._mouseUpDelegate);return false},mouseMove:function(d){if(c.browser.msie&&!d.button){return this.mouseUp(d)}if(this._mouseStarted){this.mouseDrag(d);return false}if(this.mouseDistanceMet(d)&&this.mouseDelayMet(d)){this._mouseStarted=(this.mouseStart(this._mouseDownEvent,d)!==false);(this._mouseStarted?this.mouseDrag(d):this.mouseUp(d))}return !this._mouseStarted},mouseUp:function(d){c(document).unbind("mousemove."+this.widgetName,this._mouseMoveDelegate).unbind("mouseup."+this.widgetName,this._mouseUpDelegate);if(this._mouseStarted){this._mouseStarted=false;this.mouseStop(d)}return false},mouseDistanceMet:function(d){return(Math.max(Math.abs(this._mouseDownEvent.pageX-d.pageX),Math.abs(this._mouseDownEvent.pageY-d.pageY))>=this.options.distance)},mouseDelayMet:function(d){return this._mouseDelayMet},mouseStart:function(d){},mouseDrag:function(d){},mouseStop:function(d){},mouseCapture:function(d){return true}};c.ui.mouse.defaults={cancel:null,distance:1,delay:0}})(jQuery);