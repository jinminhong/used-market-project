package com.side.project.domain.member.memberdto;

import com.side.project.domain.item.Item;

import java.util.ArrayList;
import java.util.List;

public class MemberMyInfoDto {

    private String loginId;

    private String nickName;

    private String name;

    private List<Item> itemList = new ArrayList<>();
}
