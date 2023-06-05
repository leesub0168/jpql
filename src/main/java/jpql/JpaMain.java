package jpql;

import javax.persistence.*;
import java.util.List;

public class JpaMain {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");
        PersistenceUnitUtil persistenceUnitUtil = emf.getPersistenceUnitUtil();
        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {

            JpaMain jpaMain = new JpaMain();

            jpaMain.case_if(em);

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }

    }

    private void case_if(EntityManager em) {
        Team team = new Team();
        team.setName("teamA");
        em.persist(team);

        Member member = new Member();
        member.setUsername("관리자");
        member.setAge(10);
        member.changeTeam(team);
        member.setType(MemberType.ADMIN);
        em.persist(member);

        em.flush();
        em.clear();

        String query = "select " +
                            "case when m.age <= 10 then '학생요금' " +
                            "     when m.age >= 60 then '경로요금' " +
                            "     else '일반요금'" +
                            "end " +
                        "from Member m";
        List<String> resultList = em.createQuery(query, String.class)
                .getResultList();

        for (String s : resultList) {
            System.out.println("s = " + s);
        }

        String query2 = "select coalesce(m.username, '이름 없는 회원') from Member m";
        String query3 = "select nullif(m.username, '관리자') from Member m"; // 이름이 관리자면 null로 반환
        List<String> resultList1 = em.createQuery(query3, String.class).getResultList();

        for (String s : resultList1) {
            System.out.println("s1 = " + s);
        }

    }

    private void type(EntityManager em) {
        Team team = new Team();
        team.setName("teamA");
        em.persist(team);

        Member member = new Member();
        member.setUsername("teamA");
        member.setAge(10);
        member.changeTeam(team);
        member.setType(MemberType.ADMIN);
        em.persist(member);

        em.flush();
        em.clear();

        String query = "select m.username, 'HELLO', true, m.type from Member m " +
                        "where m.type = :userType and m.username is not null" +
                        " and m.age between 1 and 100";

        List<Object[]> resultList = em.createQuery(query)
                .setParameter("userType", MemberType.ADMIN)
                .getResultList();

        for (Object[] objects : resultList) {
            System.out.println("objects = " + objects[0]);
            System.out.println("objects = " + objects[1]);
            System.out.println("objects = " + objects[2]);
            System.out.println("objects = " + objects[3]);
        }

    }

    public void basic(EntityManager em) {
        Member member = new Member();
        member.setUsername("test");
        em.persist(member);

        /**
         * 반환 타입이 명확할때는 TypedQuery
         * 명확하지 않을때는 Query
         * */
        TypedQuery<Member> query = em.createQuery("select m from Member m", Member.class);
        Query query1 = em.createQuery("select m.username, m.age from Member m");

        /** 결과가 없으면 빈 리스트 반환 */
        List<Member> resultList = query.getResultList();

        /** 결과가 정확히 하나가 나와야함.
         *  결과가 없으면 -> NoResultException
         *  결과가 두개 이상 -> NonUniqueResultException
         * */
        Member result = query.getSingleResult();

        /** 파라미터 바인딩 */
        Member singleResult = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "test")
                .getSingleResult();

    }

    public void projection(EntityManager em) {
        Member member = new Member();
        member.setUsername("test");
        member.setAge(10);
        em.persist(member);

        em.flush();
        em.clear();

        /** 엔티티 프로젝션 */
        TypedQuery<Team> query = em.createQuery("select m.team from Member m", Team.class);
        TypedQuery<Team> query1 = em.createQuery("select t from Member m join Team t", Team.class);

        /** 임베디드 타입 프로젝션 */
        List<Address> addresses = em.createQuery("select o.address from Order o", Address.class)
                                .getResultList();

        /** 스칼라 타입 프로젝션 - 타입이 다른 여러 필드를 조회할 때 */
        List<Object[]> resultList = em.createQuery("select distinct m.username, m.age from Member m", Object[].class)
                                .getResultList();


        List<MemberDTO> resultList1 = em.createQuery("select new jpql.MemberDTO(m.username, m.age) from Member m", MemberDTO.class)
                                .getResultList();
        MemberDTO memberDTO = resultList1.get(0);
        System.out.println("memberDTO name = " + memberDTO.getName());
        System.out.println("memberDTO age = " + memberDTO.getAge());

    }

    public void paging(EntityManager em) {
        for (int i = 0; i < 100; i++) {
            Member member = new Member();
            member.setUsername("test" + i);
            member.setAge(i);
            em.persist(member);
        }

        em.flush();
        em.clear();

        List<Member> result = em.createQuery("select m from Member m order by m.age desc", Member.class)
                        .setFirstResult(1)
                        .setMaxResults(10)
                        .getResultList();

        System.out.println("result size = " + result.size());
        for (Member member1 : result) {
            System.out.println("member = " + member1);
        }
    }

    public void join(EntityManager em) {
        Team team = new Team();
        team.setName("teamA");
        em.persist(team);

        Member member = new Member();
        member.setUsername("teamA");
        member.setAge(10);
        member.changeTeam(team);
        em.persist(member);

        em.flush();
        em.clear();

        String inner_join_query = "select m from Member m inner join m.team t";
        String left_join_query = "select m from Member m left join m.team t";
        String theta_join_query = "select m from Member m, Team t where m.username = t.name";
        List<Member> result = em.createQuery(theta_join_query, Member.class)
                .getResultList();

        System.out.println("result size = " + result.size());

        /** JPA 2.1부터  ON 절을 활용한 조인 지원
         * 1. 조인 대상 필터링
         * 2. 연관관계 없는 엔티티 외부 조인 (하이버네이트 5.1부터)
         * */
        String query1 = "select m from Member m left join m.team t on t.name = 'teamA'";
        String query2 = "select m from Member m left join Team t on m.username = t.name";
        List<Member> resultList1 = em.createQuery(query1, Member.class)
                .getResultList();

        List<Member> resultList2 = em.createQuery(query2, Member.class)
                .getResultList();

    }
}
