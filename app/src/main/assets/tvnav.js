/*
 * Navegación estilo Netflix para Android TV.
 * Con las flechas se salta de elemento en elemento (cards, links, botones);
 * el elemento con foco se agranda y se resalta, y la fila scrollea sola.
 */
(function () {
  if (window.__tvNavLoaded) return;
  window.__tvNavLoaded = true;

  var SEL = 'a[href],button,input,select,textarea,summary,video,' +
    '[role="button"],[role="link"],[role="tab"],[role="menuitem"],[role="option"],' +
    '[tabindex]:not([tabindex="-1"]),[onclick]';

  var css = document.createElement('style');
  css.textContent =
    '.__tvf{outline:3px solid #fff !important;outline-offset:3px !important;' +
    'transform:scale(1.07) !important;transition:transform .15s ease,box-shadow .15s ease !important;' +
    'box-shadow:0 10px 30px rgba(0,0,0,.7) !important;border-radius:6px;}' +
    '*:focus{outline:none}' +
    '::-webkit-scrollbar{display:none}';
  (document.head || document.documentElement).appendChild(css);

  var cur = null;

  function isTyping(el) {
    if (!el) return false;
    if (el.isContentEditable || el.tagName === 'TEXTAREA') return true;
    if (el.tagName !== 'INPUT') return false;
    return ['button', 'submit', 'checkbox', 'radio', 'reset', 'image', 'range'].indexOf(el.type) < 0;
  }

  function visible(el) {
    var r = el.getBoundingClientRect();
    if (r.width < 8 || r.height < 8) return false;
    var vw = window.innerWidth, vh = window.innerHeight;
    if (r.width * r.height > vw * vh * 0.5) return false; // contenedores gigantes no
    var cs = getComputedStyle(el);
    return cs.visibility !== 'hidden' && cs.display !== 'none' && parseFloat(cs.opacity) > 0.05;
  }

  function candidates() {
    var all = Array.prototype.slice.call(document.querySelectorAll(SEL));
    var list = all.filter(function (el) { return !el.disabled && visible(el); });
    // Si un candidato está adentro de otro (ej. botón dentro de una card), gana el de afuera
    return list.filter(function (el) {
      for (var p = el.parentElement; p; p = p.parentElement) {
        if (list.indexOf(p) >= 0) return false;
      }
      return true;
    });
  }

  function overlap(a1, a2, b1, b2) { return Math.min(a2, b2) - Math.max(a1, b1); }

  function pickInitial() {
    var vh = window.innerHeight;
    var list = candidates().filter(function (el) {
      var r = el.getBoundingClientRect();
      return r.bottom > 0 && r.top < vh;
    });
    list.sort(function (a, b) {
      var ra = a.getBoundingClientRect(), rb = b.getBoundingClientRect();
      return (ra.top - rb.top) || (ra.left - rb.left);
    });
    return list[0] || null;
  }

  function best(dir) {
    var f = cur.getBoundingClientRect();
    var fc = { x: f.left + f.width / 2, y: f.top + f.height / 2 };
    var bestEl = null, bestScore = Infinity;
    candidates().forEach(function (el) {
      if (el === cur || el.contains(cur) || cur.contains(el)) return;
      var r = el.getBoundingClientRect();
      var c = { x: r.left + r.width / 2, y: r.top + r.height / 2 };
      var main, cross, weight;
      if (dir === 'right' || dir === 'left') {
        if (dir === 'right' ? c.x <= fc.x + 2 : c.x >= fc.x - 2) return;
        main = dir === 'right' ? r.left - f.right : f.left - r.right;
        cross = overlap(f.top, f.bottom, r.top, r.bottom) > 0 ? 0 : Math.abs(c.y - fc.y);
        weight = 4; // en horizontal, priorizá quedarte en la misma fila
      } else {
        if (dir === 'down' ? c.y <= fc.y + 2 : c.y >= fc.y - 2) return;
        main = dir === 'down' ? r.top - f.bottom : f.top - r.bottom;
        cross = overlap(f.left, f.right, r.left, r.right) > 0 ? 0 : Math.abs(c.x - fc.x);
        weight = 1.5;
      }
      var score = Math.max(0, main) + cross * weight + Math.hypot(c.x - fc.x, c.y - fc.y) * 0.05;
      if (score < bestScore) { bestScore = score; bestEl = el; }
    });
    return bestEl;
  }

  function setFocus(el) {
    if (cur) cur.classList.remove('__tvf');
    cur = el;
    if (!el) return;
    el.classList.add('__tvf');
    el.scrollIntoView({ block: 'center', inline: 'center', behavior: 'smooth' });
    if (!isTyping(el)) { try { el.focus({ preventScroll: true }); } catch (e) {} }
  }

  function move(dir) {
    if (!cur || !document.contains(cur) || !visible(cur)) {
      setFocus(pickInitial());
      return;
    }
    var next = best(dir);
    if (next) setFocus(next);
    else if (dir === 'up' || dir === 'down') {
      window.scrollBy({ top: (dir === 'down' ? 1 : -1) * window.innerHeight * 0.6, behavior: 'smooth' });
    }
  }

  window.__tvOk = function () {
    var ae = document.activeElement;
    if (isTyping(ae)) { // ya escribiendo: OK manda el formulario
      if (ae.form) { ae.form.requestSubmit ? ae.form.requestSubmit() : ae.form.submit(); }
      return;
    }
    if (!cur || !document.contains(cur)) { setFocus(pickInitial()); return; }
    if (isTyping(cur) || cur.tagName === 'SELECT') {
      cur.focus();
      cur.click();
      if (window.TV) window.TV.showKeyboard();
      return;
    }
    if (cur.tagName === 'VIDEO') {
      if (cur.paused) cur.play(); else cur.pause();
      return;
    }
    cur.click();
  };

  window.__tvBack = function () {
    var ae = document.activeElement;
    if (isTyping(ae)) { ae.blur(); return true; }
    return false;
  };

  var KEYS = { ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right' };
  window.addEventListener('keydown', function (e) {
    if (document.fullscreenElement) return; // en el reproductor, las flechas son del video
    var dir = KEYS[e.key];
    if (!dir) return;
    var ae = document.activeElement;
    if (isTyping(ae)) {
      if (dir === 'left' || dir === 'right') return; // mover el cursor del texto
      ae.blur();
    }
    e.preventDefault();
    e.stopPropagation();
    move(dir);
  }, true);

  // Si se hace click con el mouse/touch, el foco sigue a ese elemento
  window.addEventListener('click', function (e) {
    var el = e.target && e.target.closest ? e.target.closest(SEL) : null;
    if (el && el !== cur && candidates().indexOf(el) >= 0) setFocus(el);
  }, true);

  // Arranca con algo seleccionado, y re-selecciona si el elemento desaparece (cambios de página en SPA)
  setInterval(function () {
    if (!cur || !document.contains(cur)) {
      var el = pickInitial();
      if (el) setFocus(el);
    }
  }, 1000);
})();
