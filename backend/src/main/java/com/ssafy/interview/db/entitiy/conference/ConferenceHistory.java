package com.ssafy.interview.db.entitiy.conference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ssafy.interview.db.entitiy.BaseEntity;
import com.ssafy.interview.db.entitiy.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class ConferenceHistory extends BaseEntity {

    int action;         // 0 방에서 나감 1 방에 참여 중 
    @CreatedDate
    LocalDateTime start_time;
    @LastModifiedDate
    LocalDateTime end_time;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="conference_id")
    private Conference conference;


}
