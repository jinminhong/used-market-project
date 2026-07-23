import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useSession } from "../context/SessionContext.jsx";
import { getCarrierList } from "../api/delivery.js";

export default function ShippingForm() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const { api, run, loading } = useSession();
  const [carriers, setCarriers] = useState([]);
  const [form, setForm] = useState({ trackingCompany: "", trackingNumber: "" });
  const fetchedRef = useRef(false);

  useEffect(() => {
    if (fetchedRef.current) return; // StrictMode 개발 모드 재실행 스킵(중복 요청 방지)
    fetchedRef.current = true;
    (async () => {
      const list = await getCarrierList();
      setCarriers(list);
    })();
  }, []);

  function change(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    await run(async () => {
      if (!form.trackingCompany || !form.trackingNumber.trim()) {
        throw new Error("택배사와 운송장번호를 입력해주세요.");
      }
      await api.saveTracking(orderId, form);
      navigate("/profile/sales/shipping");
    }, "운송장을 등록했습니다.");
  }

  return (
    <main className="form-page">
      <section className="form-panel">
        <p>Shipping</p>
        <h1>운송장 등록</h1>
        <form onSubmit={submit}>
          <select name="trackingCompany" value={form.trackingCompany} onChange={change}>
            <option value="">택배사 선택</option>
            {carriers.map((carrier) => (
              <option key={carrier.carrierId} value={carrier.carrierName}>{carrier.carrierName}</option>
            ))}
          </select>
          <input name="trackingNumber" value={form.trackingNumber} onChange={change} placeholder="운송장번호" />
          <button disabled={loading}>저장</button>
        </form>
        <button className="text-button" type="button" onClick={() => navigate("/profile/sales/shipping")}>취소</button>
      </section>
    </main>
  );
}
