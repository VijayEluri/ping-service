function collapseAll(){toggle(jQuery(".x"),false,true)}function expandAll(){toggle(jQuery(".x"),true,true)}function toggle(c,a,b){toggleHierarchy(c,a,b);if(a){c.removeClass("c-c");c.addClass("c-e")}else{c.removeClass("c-e");c.addClass("c-c")}}function toggleHierarchy(c,d,b){var a=c.attr("class").split(" ")[0];jQuery("."+a).each(function(){if(c[0]==this){return}var e=jQuery(this);if(d){e.show()}else{e.hide()}if(b){toggle(e,d,b)}else{if((d&&!e.is(".c-c"))||!d){toggleHierarchy(e,d)}}})}jQuery(document).ready(function(){jQuery(".cubics td[rowspan]").click(function(){var b=jQuery(this);var a=b.is(".c-c");toggle(b,a,false)})});function collapseOne(){jQuery(".c-e:not('.c-ne.c-t')").filter(function(){var a=jQuery(this).next();return a.is(".c-c")||a.is(".c-t")||a.is(".c-ne")}).each(function(){toggle(jQuery(this),false,false)})}function expandOne(){jQuery(".c-c:not('.c-ne.c-t')").filter(function(){var a=jQuery(this).attr("class").split(" ");return a[0]=="x"||(jQuery("#i"+a[1]).is(".c-e"))}).each(function(){toggle(jQuery(this),true,false)})};