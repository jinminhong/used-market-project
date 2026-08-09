// Access Token은 XSS로 인한 탈취 위험을 낮추기 위해 localStorage가 아닌 모듈 메모리에만 보관한다.
// React 트리 밖(ChatSocketContext 등)에서도 최신 토큰을 읽어야 하므로 Context state 대신 모듈 싱글턴으로 둔다.
let accessToken = null;
let onSessionExpired = () => {};

export const tokenStore = {
  get: () => accessToken,
  set: (token) => {
    accessToken = token;
  },
  setOnSessionExpired: (callback) => {
    onSessionExpired = callback;
  },
  notifyExpired: () => onSessionExpired(),
};
