import { createContext, useContext, useMemo, useRef, useState } from "react";
import { createApi } from "../api/client.js";

const NOTICE_DURATION = 3000;
const SessionContext = createContext(null);

export function SessionProvider({ children }) {
  const [useMock, setUseMock] = useState(false);
  const [member, setMember] = useState(null);
  const [notice, setNoticeState] = useState("");
  const [loading, setLoading] = useState(false);
  const api = useMemo(() => createApi(useMock), [useMock]);
  const noticeTimerRef = useRef(null);

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

  function logout() {
    setMember(null);
    setNotice("");
  }

  const value = { useMock, setUseMock, member, setMember, notice, setNotice, loading, setLoading, api, run, logout };

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) throw new Error("useSession must be used within a SessionProvider");
  return context;
}
