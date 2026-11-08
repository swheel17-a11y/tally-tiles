// Star Quest offline cache. Bump VERSION whenever index.html changes so phones pick up the update.
const VERSION='star-quest-v27';
const THREE_URL='https://cdn.jsdelivr.net/npm/three@0.128.0/build/three.min.js';
const CORE=['./','index.html','manifest.webmanifest','icon-192.png','icon-512.png','apple-touch-icon.png'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(VERSION).then(c=>c.addAll(CORE).then(()=>c.add(THREE_URL).catch(()=>{}))).then(()=>self.skipWaiting()))});
self.addEventListener('activate',e=>{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==VERSION).map(k=>caches.delete(k)))).then(()=>self.clients.claim()))});
self.addEventListener('fetch',e=>{
  if(e.request.method!=='GET')return;
  const url=new URL(e.request.url);
  // fonts: use the cached copy when there is one, otherwise fetch and remember it
  if(url.hostname.endsWith('googleapis.com')||url.hostname.endsWith('gstatic.com')||url.hostname==='cdn.jsdelivr.net'){
    e.respondWith(caches.open(VERSION).then(c=>c.match(e.request).then(hit=>hit||fetch(e.request).then(r=>{c.put(e.request,r.clone());return r}).catch(()=>hit))));
    return;
  }
  if(url.origin!==location.origin)return;
  // game files: network first so updates arrive, cache when offline
  e.respondWith(fetch(e.request).then(r=>{if(r.ok){const copy=r.clone();caches.open(VERSION).then(c=>c.put(e.request,copy))}return r})
    .catch(()=>caches.match(e.request,{ignoreSearch:true}).then(h=>h||caches.match('index.html'))));
});
