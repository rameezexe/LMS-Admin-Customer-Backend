package com.lms.member.repository;

import com.lms.member.entity.Member;
import com.lms.member.entity.MemberStatus;
import com.lms.member.entity.MembershipType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByMembershipNumber(String membershipNumber);

    Page<Member> findByNameContainingIgnoreCase(String name, Pageable pageable);

    long countByStatus(MemberStatus status);

    long countByMembershipType(MembershipType membershipType);

    boolean existsByEmail(String email);

    java.util.List<Member> findByStatus(MemberStatus status);
}
