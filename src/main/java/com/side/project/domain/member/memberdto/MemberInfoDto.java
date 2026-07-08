package com.side.project.domain.member.memberdto;

import com.side.project.domain.item.Item;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MemberInfoDto {

    private String loginId;

    private String nickName;

    private String name;

    private List<Item> itemList = new ArrayList<>();
}
