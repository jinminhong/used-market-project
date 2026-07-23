package com.side.project.domain.member;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Address {

    private String zonecode;

    private String roadAddress;

    private String jibunAddress;

    private String detailAddress;

    protected Address(){

    }

    public Address(String zonecode, String roadAddress, String jibunAddress, String detailAddress) {
        this.zonecode = zonecode;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.detailAddress = detailAddress;
    }
}
