const assetVersion='20261008-4';
document.querySelectorAll('link[rel="stylesheet"]').forEach(link=>{
  const raw=link.getAttribute('href')||'';
  if(/(^|\/)assets\/styles\.css(?:\?|$)/.test(raw)) link.setAttribute('href',`/assets/styles.css?v=${assetVersion}`);
  if(/(^|\/)assets\/mobile\.css(?:\?|$)/.test(raw)) link.setAttribute('href',`/assets/mobile.css?v=${assetVersion}`);
});
const faviconVersion='20261008-1';
const upsertHeadLink=(selector,attributes)=>{let link=document.querySelector(selector);if(!link){link=document.createElement('link');document.head.appendChild(link);}Object.entries(attributes).forEach(([key,value])=>link.setAttribute(key,value));return link;};
upsertHeadLink('link[rel~="icon"][type="image/svg+xml"]',{rel:'icon',type:'image/svg+xml',sizes:'any',href:`/favicon.svg?v=${faviconVersion}`});
upsertHeadLink('link[rel~="icon"][sizes="96x96"]',{rel:'icon',type:'image/png',sizes:'96x96',href:`/favicon-96x96.png?v=${faviconVersion}`});
upsertHeadLink('link[rel~="icon"][sizes="32x32"]',{rel:'icon',type:'image/png',sizes:'32x32',href:`/favicon-32x32.png?v=${faviconVersion}`});
upsertHeadLink('link[rel="shortcut icon"]',{rel:'shortcut icon',type:'image/x-icon',href:`/favicon.ico?v=${faviconVersion}`});
upsertHeadLink('link[rel="apple-touch-icon"]',{rel:'apple-touch-icon',sizes:'180x180',href:`/apple-touch-icon.png?v=${faviconVersion}`});
upsertHeadLink('link[rel="mask-icon"]',{rel:'mask-icon',href:`/safari-pinned-tab.svg?v=${faviconVersion}`,color:'#80652e'});
if(!document.querySelector('link[href="assets/mobile.css"],link[href="/assets/mobile.css"]'))upsertHeadLink('link[data-regent-mobile]',{rel:'stylesheet',href:'/assets/mobile.css','data-regent-mobile':'true'});
document.querySelectorAll('.brand-mark').forEach(el=>{el.setAttribute('aria-hidden','true');el.textContent='RC';});

const currentPage=(location.pathname.split('/').pop()||'index.html').toLowerCase();
const publicNav=[['what-we-do.html','What We Do'],['how-we-work.html','How We Work'],['ventures.html','Ventures'],['for-businesses.html','Businesses'],['for-founders.html','Founders'],['about.html','About'],['start-a-venture.html','Discuss a Venture']];
const nav=document.querySelector('.site-header .nav-links');
if(nav&&!document.body.classList.contains('admin-shell')){
  nav.innerHTML=publicNav.map(([href,label],i)=>`<a href="${href}"${currentPage===href?' aria-current="page"':''}${i===publicNav.length-1?' class="nav-cta"':''}>${label}</a>`).join('');
  nav.setAttribute('aria-label','Primary navigation');
}
document.querySelectorAll('.brand-copy small').forEach(el=>{if(!document.body.classList.contains('admin-shell'))el.textContent='Venture Development';});
document.querySelectorAll('.brand[href="index.html"]').forEach(el=>el.setAttribute('href','/'));

const footer=document.querySelector('.site-footer');
if(footer&&!document.body.classList.contains('admin-shell')){
  footer.innerHTML=`<div class="container"><div class="footer-grid"><div class="footer-brand"><a class="brand" href="/" aria-label="The Regent Collection home"><span class="brand-mark" aria-hidden="true">RC</span><span class="brand-copy"><strong>THE REGENT COLLECTION</strong><small>Venture Development</small></span></a><p>A London-based company building new ventures.</p></div><div class="footer-col"><h4>Explore</h4><a href="what-we-do.html">What We Do</a><a href="how-we-work.html">How We Work</a><a href="ventures.html">Ventures</a><a href="venture-assessment.html">Venture Assessment</a></div><div class="footer-col"><h4>Company</h4><a href="about.html">About</a><a href="contact.html">Contact</a><a href="legal.html">Corporate information</a><a href="privacy.html">Privacy</a><a href="terms.html">Website terms</a><a href="regent-mile-legal.html">Regent Mile legal centre</a></div><div class="footer-col"><h4>Registered details</h4><span>THE REGENT COLLECTION LTD.</span><span>Company no. 14693979</span><span>4th Floor, Silverstream House<br>45 Fitzroy Street<br>London W1T 6EB</span><a href="mailto:enquiries@regentcompany.co.uk">enquiries@regentcompany.co.uk</a><a href="https://www.linkedin.com/company/107404038/" target="_blank" rel="noopener noreferrer">LinkedIn</a><a href="https://www.instagram.com/the.regent.co" target="_blank" rel="noopener noreferrer">Instagram</a></div></div><div class="footer-bottom"><span>© <span data-year></span> The Regent Collection Ltd. All rights reserved.</span><span>London · United Kingdom</span></div></div>`;
}

const menuButton=document.querySelector('.menu-toggle');
const setMenuState=(open)=>{if(!menuButton||!nav)return;nav.classList.toggle('open',open);menuButton.setAttribute('aria-expanded',String(open));menuButton.setAttribute('aria-label',open?'Close navigation':'Open navigation');document.body.classList.toggle('menu-open',open);};
const closeMenu=()=>setMenuState(false);
if(menuButton&&nav){if(!nav.id)nav.id='primary-navigation';menuButton.setAttribute('aria-controls',nav.id);setMenuState(false);menuButton.addEventListener('click',()=>setMenuState(menuButton.getAttribute('aria-expanded')!=='true'));nav.querySelectorAll('a').forEach(link=>link.addEventListener('click',closeMenu));document.addEventListener('click',event=>{if(menuButton.getAttribute('aria-expanded')!=='true')return;if(!nav.contains(event.target)&&!menuButton.contains(event.target))closeMenu();});document.addEventListener('keydown',event=>{if(event.key==='Escape')closeMenu();});window.addEventListener('resize',()=>{if(window.innerWidth>1080)closeMenu();});}
document.querySelectorAll('[data-year]').forEach(el=>el.textContent=new Date().getFullYear());
