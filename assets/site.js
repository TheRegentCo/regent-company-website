const faviconVersion = '20261003-2';

const addHeadLink = (attributes) => {
  const link = document.createElement('link');
  Object.entries(attributes).forEach(([key, value]) => link.setAttribute(key, value));
  document.head.appendChild(link);
  return link;
};

// Cross-browser favicon set: SVG for modern browsers, PNG/ICO fallbacks,
// Apple touch icon, and Safari pinned-tab support on macOS.
if (!document.querySelector('link[rel~="icon"][type="image/svg+xml"]')) {
  addHeadLink({ rel: 'icon', type: 'image/svg+xml', sizes: 'any', href: `/favicon.svg?v=${faviconVersion}` });
}
if (!document.querySelector('link[rel~="icon"][type="image/png"]')) {
  addHeadLink({ rel: 'icon', type: 'image/png', sizes: '32x32', href: `/favicon-32x32.png?v=${faviconVersion}` });
}
if (!document.querySelector('link[rel="shortcut icon"]')) {
  addHeadLink({ rel: 'shortcut icon', type: 'image/x-icon', href: `/favicon.ico?v=${faviconVersion}` });
}
if (!document.querySelector('link[rel="apple-touch-icon"]')) {
  addHeadLink({ rel: 'apple-touch-icon', sizes: '180x180', href: `/apple-touch-icon.png?v=${faviconVersion}` });
}
if (!document.querySelector('link[rel="mask-icon"]')) {
  addHeadLink({ rel: 'mask-icon', href: `/safari-pinned-tab.svg?v=${faviconVersion}`, color: '#171714' });
}

if (!document.querySelector('link[href="assets/mobile.css"], link[href="/assets/mobile.css"]')) {
  const mobileStyles = document.createElement('link');
  mobileStyles.rel = 'stylesheet';
  mobileStyles.href = 'assets/mobile.css';
  document.head.appendChild(mobileStyles);
}

const menuButton = document.querySelector('.menu-toggle');
const nav = document.querySelector('.nav-links');

const setMenuState = (open) => {
  if (!menuButton || !nav) return;
  nav.classList.toggle('open', open);
  menuButton.setAttribute('aria-expanded', String(open));
  menuButton.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation');
  document.body.classList.toggle('menu-open', open);
};

const closeMenu = () => setMenuState(false);

if (menuButton && nav) {
  if (!nav.id) nav.id = 'primary-navigation';
  menuButton.setAttribute('aria-controls', nav.id);
  setMenuState(false);

  menuButton.addEventListener('click', () => {
    const open = menuButton.getAttribute('aria-expanded') !== 'true';
    setMenuState(open);
  });

  nav.querySelectorAll('a').forEach(link => link.addEventListener('click', closeMenu));

  document.addEventListener('click', event => {
    if (menuButton.getAttribute('aria-expanded') !== 'true') return;
    if (!nav.contains(event.target) && !menuButton.contains(event.target)) closeMenu();
  });

  document.addEventListener('keydown', event => {
    if (event.key === 'Escape') closeMenu();
  });

  window.addEventListener('resize', () => {
    if (window.innerWidth > 900) closeMenu();
  });
}

if ('IntersectionObserver' in window) {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('in');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: .12 });

  document.querySelectorAll('.reveal').forEach(el => observer.observe(el));
} else {
  document.querySelectorAll('.reveal').forEach(el => el.classList.add('in'));
}

document.querySelectorAll('[data-year]').forEach(el => el.textContent = new Date().getFullYear());
