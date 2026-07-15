package com.side.project.domain.member.memberdto;

import com.side.project.domain.member.Address;
import lombok.Data;

@Data
public class MemberInfoDto {

    private Long memberId;

    private String loginId;

    private String nickName;

    private String name;

    private Address address;

    public MemberInfoDto(Long memberId, String loginId, String nickName, String name, Address address) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.nickName = nickName;
        this.name = name;
        this.address = address;
    }
}
