import { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";
import { createApi } from "../api/client.js";
import { normalizeMemberInfo } from "../api/normalize.js";

const NOTICE_DURATION = 3000;
const SessionContext = createContext(null);

export function SessionProvider({ children }) {
  const [useMock, setUseMock] = useState(false);
  const [member, setMember] = useState(null);
  const [initializing, setInitializing] = useState(true);
  const [notice, setNoticeState] = useState("");
  const [loading, setLoading] = useState(false);
  const api = useMemo(() => createApi(useMock), [useMock]);
  const noticeTimerRef = useRef(null);
  const initFetchedRef = useRef(false);

  useEffect(() => {
    if (initFetchedRef.current) return; // StrictMode 개발 모드 재실행 스킵(중복 요청 방지)
    initFetchedRef.current = true;
    (async () => {
      try {
        const info = await createApi(false).getMyInfo();
        setMember(normalizeMemberInfo(info));
      } catch {
        // 세션 없음(비로그인) — 조용히 무시
      } finally {
        setInitializing(false);
      }
    })();
  }, []);

  function setNotice(message) {
    if (noticeTimerRef.current) {
      clearTimeout(noticeTimerRef.current);
      noticeTimerRef.current = null;
    }
    setNoticeState(message);
    if (message) {
      noticeTimerRef.current = setTimeout(() => {
        setNoticeState("");
        noticeTimerRef.current = null;
      }, NOTICE_DURATION);
    }
  }

  async function run(fn, message = "") {
    setLoading(true);
    setNotice("");
    try {
      await fn();
      if (message) setNotice(message);
    } catch (error) {
      setNotice(error.message || "요청 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  }

  async function logout() {
    if (!useMock) {
      try {
        await api.logout();
      } catch {
        // 서버 로그아웃 실패해도 프론트 상태는 정리한다
      }
    }
    setMember(null);
    setNotice("");
  }

  const value = { useMock, setUseMock, member, setMember, initializing, notice, setNotice, loading, setLoading, api, run, logout };

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) throw new Error("useSession must be used within a SessionProvider");
  return context;
}
