package com.side.project.domain.member;

import com.side.project.domain.item.Item;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "members_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false, unique = true, length = 50)
    private String nickName;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String password;

    @OneToMany(mappedBy = "member")
    private List<Item> itemList = new ArrayList<>();

    public Member(String loginId, String name, String password , String nickName) {
        this.loginId = loginId;
        this.name = name;
        this.password = password;
        this.nickName = nickName;
    }
}
