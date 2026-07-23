import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { useSession } from "./SessionContext.jsx";

const ChatSocketContext = createContext(null);

export function ChatSocketProvider({ children }) {
  const { member, useMock } = useSession();
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const [connectionError, setConnectionError] = useState(null);

  useEffect(() => {
    if (!member || useMock) {
      clientRef.current?.deactivate();
      clientRef.current = null;
      setConnected(false);
      setConnectionError(null);
      return undefined;
    }

    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws-chat`,
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        setConnectionError(null);
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => setConnectionError(frame.headers?.message || "채팅 서버 오류가 발생했습니다."),
      onWebSocketError: () => setConnectionError("채팅 서버에 연결할 수 없습니다."),
    });
    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
      if (clientRef.current === client) clientRef.current = null;
      setConnected(false);
    };
  }, [member, useMock]);

  const subscribeRoom = useCallback((roomId, onMessage) => {
    const client = clientRef.current;
    if (!client || !client.connected) return () => {};
    const subscription = client.subscribe(`/topic/chat/rooms/${roomId}`, (message) => {
      try {
        onMessage(JSON.parse(message.body));
      } catch {
        // 페이로드 파싱 실패는 무시
      }
    });
    return () => subscription.unsubscribe();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [connected]);

  const sendMessage = useCallback((roomId, content) => {
    const client = clientRef.current;
    if (!client || !client.connected) {
      return Promise.reject(new Error("채팅 서버에 연결되어 있지 않습니다. 잠시 후 다시 시도해주세요."));
    }
    client.publish({
      destination: `/app/chat/rooms/${roomId}/messages`,
      body: JSON.stringify({ content }),
    });
    return Promise.resolve();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [connected]);

  const value = { connected, connectionError, subscribeRoom, sendMessage };

  return <ChatSocketContext.Provider value={value}>{children}</ChatSocketContext.Provider>;
}

export function useChatSocket() {
  const context = useContext(ChatSocketContext);
  if (!context) throw new Error("useChatSocket must be used within a ChatSocketProvider");
  return context;
}
