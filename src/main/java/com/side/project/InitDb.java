package com.side.project;

import com.side.project.domain.item.Category;
import com.side.project.domain.item.Item;
import com.side.project.domain.item.ItemStatus;
import com.side.project.domain.member.Address;
import com.side.project.domain.member.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitDb implements ApplicationRunner {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Member member1 = new Member("asd", "hong", "1234", "hong" ,new Address("서울","도로","우리집"));
        Member member2 = new Member("lee", "lee", "1234", "lee" ,new Address("서울","도로","우리집"));

        em.persist(member1);
        em.persist(member2);

        em.flush();


        Item item1 = new Item("item1" , "good",10000, ItemStatus.SELLING, Category.OUTER ,member1);
        Item item2 = new Item("item2" , "bad",20000, ItemStatus.SELLING, Category.BOTTOM, member2);

        em.persist(item1);
        em.persist(item2);
    }
}