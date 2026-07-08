package com.side.project.domain.member.memberdto;

import com.side.project.domain.member.Address;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class MemberUpdateDto {
    private String name;

    private String password;

    private String nickname;

    private Address address;
}
