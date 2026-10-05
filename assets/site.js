const faviconVersion='20261005-2';
const addHeadLink=(attributes)=>{const link=document.createElement('link');Object.entries(attributes).forEach(([key,value])=>link.setAttribute(key,value));document.head.appendChild(link);return link;};
if(!document.querySelector('link[rel~="icon"][type="image/svg+xml"]'))addHeadLink({rel:'icon',type:'image/svg+xml',sizes:'any',href:`/favicon.svg?v=${faviconVersion}`});
if(!document.querySelector('link[rel~="icon"][type="image/png"]'))addHeadLink({rel:'icon',type:'image/png',sizes:'32x32',href:`/favicon-32x32.png?v=${faviconVersion}`});
if(!document.querySelector('link[rel="shortcut icon"]'))addHeadLink({rel:'shortcut icon',type:'image/x-icon',href:`/favicon.ico?v=${faviconVersion}`});
if(!document.querySelector('link[rel="apple-touch-icon"]'))addHeadLink({rel:'apple-touch-icon',sizes:'180x180',href:`/apple-touch-icon.png?v=${faviconVersion}`});
if(!document.querySelector('link[rel="mask-icon"]'))addHeadLink({rel:'mask-icon',href:`/safari-pinned-tab.svg?v=${faviconVersion}`,color:'#171714'});
if(!document.querySelector('link[href="assets/mobile.css"],link[href="/assets/mobile.css"]'))addHeadLink({rel:'stylesheet',href:'/assets/mobile.css'});

const currentPage=(location.pathname.split('/').pop()||'index.html').toLowerCase();
const publicNav=[['what-we-do.html','What We Do'],['how-we-work.html','How We Work'],['ventures.html','Ventures'],['for-businesses.html','Businesses'],['for-founders.html','Founders'],['about.html','About'],['start-a-venture.html','Discuss a Venture']];
const nav=document.querySelector('.site-header .nav-links');
if(nav&&!document.body.classList.contains('admin-shell')){
  nav.innerHTML=publicNav.map(([href,label],i)=>`<a href="${href}"${currentPage===href?' aria-current="page"':''}${i===publicNav.length-1?' class="nav-cta"':''}>${label}</a>`).join('');
  nav.setAttribute('aria-label','Primary navigation');
}
document.querySelectorAll('.brand-copy small').forEach(el=>{if(!document.body.classList.contains('admin-shell'))el.textContent='Venture Development';});

const footer=document.querySelector('.site-footer');
if(footer&&!document.body.classList.contains('admin-shell')){
  footer.innerHTML=`<div class="container"><div class="footer-grid"><div class="footer-brand"><a class="brand" href="index.html"><span class="brand-mark">RC</span><span class="brand-copy"><strong>THE REGENT COLLECTION</strong><small>Venture Development</small></span></a><p>A London-based company developing commercial ventures.</p></div><div class="footer-col"><h4>Explore</h4><a href="what-we-do.html">What We Do</a><a href="how-we-work.html">How We Work</a><a href="ventures.html">Ventures</a><a href="venture-assessment.html">Venture Assessment</a></div><div class="footer-col"><h4>Company</h4><a href="about.html">About</a><a href="contact.html">Contact</a><a href="legal.html">Corporate information</a><a href="privacy.html">Privacy</a><a href="terms.html">Website terms</a><a href="regent-mile-legal.html">Regent Mile legal centre</a></div><div class="footer-col"><h4>Registered details</h4><span>THE REGENT COLLECTION LTD.</span><span>Company no. 14693979</span><span>4th Floor, Silverstream House<br>45 Fitzroy Street<br>London W1T 6EB</span><a href="mailto:enquiries@regentcompany.co.uk">enquiries@regentcompany.co.uk</a><a href="https://www.linkedin.com/company/107404038/" target="_blank" rel="noopener noreferrer">LinkedIn</a><a href="https://www.instagram.com/the.regent.co" target="_blank" rel="noopener noreferrer">Instagram</a></div></div><div class="footer-bottom"><span>© <span data-year></span> The Regent Collection Ltd. All rights reserved.</span><span>London · United Kingdom</span></div></div>`;
}

const menuButton=document.querySelector('.menu-toggle');
const setMenuState=(open)=>{if(!menuButton||!nav)return;nav.classList.toggle('open',open);menuButton.setAttribute('aria-expanded',String(open));menuButton.setAttribute('aria-label',open?'Close navigation':'Open navigation');document.body.classList.toggle('menu-open',open);};
const closeMenu=()=>setMenuState(false);
if(menuButton&&nav){if(!nav.id)nav.id='primary-navigation';menuButton.setAttribute('aria-controls',nav.id);setMenuState(false);menuButton.addEventListener('click',()=>setMenuState(menuButton.getAttribute('aria-expanded')!=='true'));nav.querySelectorAll('a').forEach(link=>link.addEventListener('click',closeMenu));document.addEventListener('click',event=>{if(menuButton.getAttribute('aria-expanded')!=='true')return;if(!nav.contains(event.target)&&!menuButton.contains(event.target))closeMenu();});document.addEventListener('keydown',event=>{if(event.key==='Escape')closeMenu();});window.addEventListener('resize',()=>{if(window.innerWidth>1080)closeMenu();});}
if('IntersectionObserver'in window){const observer=new IntersectionObserver(entries=>entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('in');observer.unobserve(entry.target);}}),{threshold:.1});document.querySelectorAll('.reveal').forEach(el=>observer.observe(el));}else document.querySelectorAll('.reveal').forEach(el=>el.classList.add('in'));
document.querySelectorAll('[data-year]').forEach(el=>el.textContent=new Date().getFullYear());
