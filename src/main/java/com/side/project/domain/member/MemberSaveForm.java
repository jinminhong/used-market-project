package com.side.project.domain.member;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class MemberSaveForm {

    @NotEmpty
    private String loginId;

    @NotEmpty
    private String name;

    @NotEmpty
    private String password;

    @NotEmpty
    private String nickname;
}
