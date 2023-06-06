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

            jpaMain.fetch_join(em);

            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }

    }

    private void fetch_join(EntityManager em) {
        Team team = new Team();
        team.setName("teamA");
        em.persist(team);

        Team team2 = new Team();
        team2.setName("teamB");
        em.persist(team2);

        Member member = new Member();
        member.setUsername("회원1");
        member.setAge(10);
        member.changeTeam(team);
        member.setType(MemberType.ADMIN);
        em.persist(member);

        Member member2 = new Member();
        member2.setUsername("회원2");
        member2.setAge(10);
        member2.changeTeam(team);
        member2.setType(MemberType.ADMIN);
        em.persist(member2);

        Member member3 = new Member();
        member3.setUsername("회원3");
        member3.setAge(10);
        member3.changeTeam(team2);
        member3.setType(MemberType.ADMIN);
        em.persist(member3);

        em.flush();
        em.clear();

        /** 지연 로딩이 되어있어도, fetch 조인이 우선. */
        String query = "select m from Member m join fetch m.team";
        /** 일대다 관계는 데이터를 조회하면 결과 갯수가 뻥튀기됨. 다대일은 상관 X */
        String query2 = "select t from Team t join fetch t.members";
        /** 중복된 데이터를 제거하고 싶으면 distinct 사용 */
        String query3 = "select distinct t from Team t join fetch t.members";

        /** 일반 조인은 결과를 반환할 때 연관 관계를 고려하지 않음.
         *  Team을 조회하면, Team만 가져오고, 그 이후에 Team의 멤버를 필요로 하면 그때 다시 조회쿼리가 나가게됨
         *
         *  페치 조인은 연관된 엔티티도 함께 조회된다.(즉시로딩) - 페치조인은 객체그래프를 한번에 조회
         * */
        String just_join = "select t from Team t join t.members m ";
        String fetch_join = "select t from Team t join fetch t.members";

        /** 컬렉션을 페치 조인하면 페이징 api를 사용할 수 없다. 사용시 동작은 하나 jpa에서 결과값을 다 가져온 후
         *  메모리에서 페이징 하는 식으로 처리하기 때문에 위험.
         *
         *  JPA 에서는 객체 그래프 탐색시 전체를 다 가져오는 것을 전제로 한다.
         *  예를 들어 Team의 members를 조회해온다면, 전체 멤버를 조회해와야지 멤버중 5명만 조회하는 것은 의미에 맞지않다.
         *  만약 멤버 5명만 필요하다면 Member 쪽에서 5명을 조회하는 방식으로 바꿔야 한다.
         * */
        List<Team> result = em.createQuery("select t from Team t", Team.class).getResultList();

        for (Team t : result) {
            System.out.println("team = " + t.getName() + ", members = " + t.getMembers().size());
            for (Member m : t.getMembers()) {
                System.out.println("-> member : " + m);
            }
        }


    }

    public void associate(EntityManager em) {
        Team team = new Team();
        team.setName("teamA");
        em.persist(team);

        Member member = new Member();
        member.setUsername("관리자1");
        member.setAge(10);
        member.changeTeam(team);
        member.setType(MemberType.ADMIN);
        em.persist(member);

        Member member2 = new Member();
        member2.setUsername("관리자2");
        member2.setAge(10);
        member2.changeTeam(team);
        member2.setType(MemberType.ADMIN);
        em.persist(member2);

        em.flush();
        em.clear();

        /** 단일값 연관 관계 - 묵시적 내부 조인 발생, 탐색 O 실무사용 권장 X */
        String query1 = "select m.team.name from Member m";

        /** 컬렉션 값 연관 관계 - 묵시적 내부 조인 발생, 탐색 X */
        String query2 = "select t.members from Team t";

        /** 컬렉션 값 연관 관계 - 명시적 조인을 사용하면 탐색 가능 */
        String query3 = "select m.username from Team t join t.members m";

        /** 가급적 묵시적 조인 대신에 명시적 조인을 사용해야 한다.
         *  SQL 튜닝면에서도 명시적 조인이 더 용이하며,
         *  묵시적 조인은 조인이 일어나는 상황을 한눈에 파악하기 어렵다
         * */

        List<String> resultList = em.createQuery(query3, String.class).getResultList();

        System.out.println("resultList = " + resultList);
    }

    public void function(EntityManager em) {
        Team team = new Team();
        team.setName("teamA");
        em.persist(team);

        Member member = new Member();
        member.setUsername("관리자1");
        member.setAge(10);
        member.changeTeam(team);
        member.setType(MemberType.ADMIN);
        em.persist(member);

        Member member2 = new Member();
        member2.setUsername("관리자2");
        member2.setAge(10);
        member2.changeTeam(team);
        member2.setType(MemberType.ADMIN);
        em.persist(member2);

        em.flush();
        em.clear();
        /**
         * JPQL 기본함수
         * CONCAT, SUBSTRING, TRIM, LOWER, UPPER, LENGTH, LOCATE
         * ABS, SQRT, MOD
         * SIZE, INDEX
         *
         * 사용자 정의 함수
         * function(이름, 대상 필드)
         * */
        String query = "select concat('a','b') from Member m";
        String query2 = "select size(t.members) from Team t";
        String query3 = "select function('group_concat', m.username) from Member m";
        List<String> resultList = em.createQuery(query3, String.class)
                .getResultList();

        for (String s : resultList) {
            System.out.println("s = " + s);
        }


    }

    public void case_if(EntityManager em) {
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

    public void type(EntityManager em) {
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
