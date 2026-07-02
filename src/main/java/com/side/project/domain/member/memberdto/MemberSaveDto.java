package com.side.project.domain.member.memberdto;

import com.side.project.domain.member.Address;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class MemberSaveDto {

    @NotEmpty
    private String loginId;

    @NotEmpty
    private String name;

    @NotEmpty
    private String password;

    @NotEmpty
    private String nickname;

    private Address address;
}
