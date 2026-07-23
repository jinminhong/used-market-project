// 택배 배송조회 연동 준비용 클라이언트. 실제 API 키가 없으면 하드코딩된 목록/널 값으로 폴백한다.
// 해야 할 일은 docs/delivery-api-todo.md 참고.

const SWEETTRACKER_BASE_URL = "https://info.sweettracker.co.kr/api/v1";
const API_KEY = import.meta.env.VITE_SWEETTRACKER_KEY;

// 정확한 코드는 실제 SweetTracker 문서(https://tracking.sweettracker.co.kr)로 재검증 필요.
export const CARRIERS = [
  { carrierId: "04", carrierName: "CJ대한통운" },
  { carrierId: "05", carrierName: "한진택배" },
  { carrierId: "08", carrierName: "롯데택배" },
  { carrierId: "01", carrierName: "우체국택배" },
  { carrierId: "06", carrierName: "로젠택배" },
];

export async function getCarrierList() {
  if (!API_KEY) {
    console.warn("[delivery] VITE_SWEETTRACKER_KEY가 설정되지 않아 하드코딩된 택배사 목록을 사용합니다.");
    return CARRIERS;
  }
  try {
    const response = await fetch(`${SWEETTRACKER_BASE_URL}/companylist?t_key=${API_KEY}`);
    if (!response.ok) throw new Error(`택배사 목록 조회 실패 (${response.status})`);
    const data = await response.json();
    return (data.Company ?? []).map((company) => ({ carrierId: company.Code, carrierName: company.International ?? company.Name }));
  } catch (error) {
    console.warn("[delivery] 택배사 목록 조회 실패, 하드코딩된 목록으로 폴백합니다.", error);
    return CARRIERS;
  }
}

export async function trackShipment(carrierId, trackingNumber) {
  if (!API_KEY) {
    console.warn("[delivery] VITE_SWEETTRACKER_KEY가 설정되지 않아 배송조회를 건너뜁니다.");
    return null;
  }
  try {
    const response = await fetch(
      `${SWEETTRACKER_BASE_URL}/trackingInfo?t_key=${API_KEY}&t_code=${carrierId}&t_invoice=${trackingNumber}`
    );
    if (!response.ok) throw new Error(`배송조회 실패 (${response.status})`);
    return await response.json();
  } catch (error) {
    console.warn("[delivery] 배송조회 실패", error);
    return null;
  }
}
