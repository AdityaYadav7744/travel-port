(function(){
  function qs(sel, el){return (el||document).querySelector(sel)}
  function qsa(sel, el){return Array.prototype.slice.call((el||document).querySelectorAll(sel))}
  document.addEventListener('DOMContentLoaded', function(){
    qsa('.tp-carousel').forEach(function(carousel){
      var viewport = qs('.tp-carousel__viewport', carousel);
      var slides = qsa('.tp-carousel__slide', viewport);
      if(!slides.length) return;
      var idx = 0;
      function show(i){
        idx = (i+slides.length)%slides.length;
        viewport.style.transform = 'translateX(' + (-idx*100) + '%)';
      }
      var prev = qs('.tp-carousel__prev', carousel);
      var next = qs('.tp-carousel__next', carousel);
      prev && prev.addEventListener('click', function(){ show(idx-1); });
      next && next.addEventListener('click', function(){ show(idx+1); });
      // per-slide CTA buttons: go to next
      qsa('.tp-carousel__cta', carousel).forEach(function(btn){
        btn.addEventListener('click', function(e){
          e.preventDefault();
          show(idx+1);
        });
      });
      // auto-scroll
      var t = parseFloat(carousel.getAttribute('data-scroll-time'));
      if(!isNaN(t) && t>0){
        var interval = setInterval(function(){ show(idx+1); }, t*1000);
        carousel.addEventListener('mouseenter', function(){ clearInterval(interval); });
        carousel.addEventListener('mouseleave', function(){ interval = setInterval(function(){ show(idx+1); }, t*1000); });
      }
    });
  });
})();
