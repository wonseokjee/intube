package com.ssafy.interview.db.repository.interview;

import com.ssafy.interview.db.entitiy.interview.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 인터뷰 신청자 모델 관련 디비 쿼리 생성을 위한 JPA Query Method 인터페이스 정의.
 */
@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, Long>, ApplicantRepositoryCustom { // 인터뷰 생성 Method

//    List<Applicant> findByInterviewTime_Id(Long interview_time_id);
}