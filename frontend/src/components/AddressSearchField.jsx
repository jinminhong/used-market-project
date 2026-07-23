import { Input } from "./ui/input.jsx";
import { Button } from "./ui/button.jsx";
import { useSession } from "../context/SessionContext.jsx";

/**
 * 다음(Daum) 우편번호 서비스 팝업으로 zonecode/roadAddress/jibunAddress를 채우고,
 * detailAddress(상세주소)만 사용자가 직접 입력하는 재사용 필드.
 * 부모는 flat한 form state를 그대로 쓰고, onSearch로 검색 결과 3개 필드를 한 번에 병합받는다.
 */
export default function AddressSearchField({ zonecode, roadAddress, detailAddress, onSearch, onDetailChange, disabled }) {
  const { setNotice } = useSession();

  function openPostcode() {
    if (!window.daum?.Postcode) {
      setNotice("주소 검색 서비스를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
      return;
    }
    new window.daum.Postcode({
      oncomplete(data) {
        onSearch({
          zonecode: data.zonecode,
          roadAddress: data.roadAddress,
          jibunAddress: data.jibunAddress,
        });
      },
    }).open();
  }

  return (
    <div className="address-search-field">
      <div className="field-with-action">
        <Input value={zonecode} placeholder="우편번호" readOnly />
        <Button type="button" variant="outline" size="sm" onClick={openPostcode} disabled={disabled}>주소 검색</Button>
      </div>
      <Input value={roadAddress} placeholder="도로명 주소" readOnly />
      <Input name="detailAddress" value={detailAddress} onChange={onDetailChange} placeholder="상세주소 (동/호수 등)" />
    </div>
  );
}
