// Service Worker: 페이지가 닫혀 있어도 브라우저가 백그라운드에서 깨워서 실행함.
// 반드시 사이트 루트(/sw.js)에 둬야 scope 가 전체(/)가 됨.

self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));

// 설치 가능 조건 충족용 fetch 핸들러 (캐싱은 안 하고 그냥 네트워크로 통과)
self.addEventListener('fetch', () => {});

// ★ 서버가 보낸 푸시 도착
self.addEventListener('push', (event) => {
  let data = { title: '알림', body: '', url: '/' };
  try {
    data = { ...data, ...event.data.json() };
  } catch (e) {
    if (event.data) data.body = event.data.text();
  }

  // waitUntil 로 감싸야 알림 띄우기 전에 SW 가 종료되지 않음
  event.waitUntil(
    self.registration.showNotification(data.title, {
      body: data.body,
      icon: '/icons/icon-192.png',
      badge: '/icons/icon-192.png',
      data: { url: data.url },
    })
  );
});

// ★ 알림 클릭 → 이미 열린 창 있으면 포커스, 없으면 새로 열기
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = event.notification.data?.url || '/';

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((wins) => {
      for (const w of wins) {
        if (new URL(w.url).pathname === url && 'focus' in w) return w.focus();
      }
      return self.clients.openWindow(url);
    })
  );
});
