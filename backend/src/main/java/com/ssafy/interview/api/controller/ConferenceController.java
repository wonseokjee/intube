package com.ssafy.interview.api.controller;

import com.ssafy.interview.api.request.conference.*;
import com.ssafy.interview.api.response.conference.ConferenceInRes;
import com.ssafy.interview.api.service.conference.ConferenceService;
import com.ssafy.interview.api.service.user.AuthService;
import com.ssafy.interview.common.model.response.BaseResponseBody;
import com.ssafy.interview.db.entitiy.conference.Conference;
import com.ssafy.interview.db.entitiy.conference.ConferenceHistory;
import com.ssafy.interview.db.entitiy.interview.Question;
import io.openvidu.java.client.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@Api(value = "Conference API", tags = {"Conference"})
@RestController
@RequestMapping("/conference")
public class ConferenceController {

    @Value("${OPENVIDU_URL}")
    private String OPENVIDU_URL;

    @Value("${OPENVIDU_SECRET}")
    private String OPENVIDU_SECRET;

    private OpenVidu openvidu;

    @Autowired
    ConferenceService conferenceService;
    @Autowired
    AuthService authService;

    @PostConstruct
    public void init() {
        this.openvidu = new OpenVidu(OPENVIDU_URL, OPENVIDU_SECRET);
    }

    @PostMapping("/sessions")
    public ResponseEntity<String> initializeSession(@RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {
        SessionProperties properties = SessionProperties.fromJson(params).build();
        Session session = openvidu.createSession(properties);
        return new ResponseEntity<>(session.getSessionId(), HttpStatus.OK);
    }

    @PostMapping("/sessions/{sessionId}/connections")
    public ResponseEntity<String> createConnection(@PathVariable("sessionId") String sessionId,
                                                   @RequestBody(required = false) Map<String, Object> params)
            throws OpenViduJavaClientException, OpenViduHttpException {
        Session session = openvidu.getActiveSession(sessionId);
        if (session == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        ConnectionProperties properties = ConnectionProperties.fromJson(params).build();
        Connection connection = session.createConnection(properties);
        return new ResponseEntity<>(connection.getToken(), HttpStatus.OK);
    }

    @PostMapping("/start")
    @ApiOperation(value = "Conference ??? ??????")
    @ApiResponses({
            @ApiResponse(code = 200, message = "??????"),
            @ApiResponse(code = 404, message = "????????? ??????"),
            @ApiResponse(code = 500, message = "?????? ??????")
    })
    public ResponseEntity<ConferenceInRes> startConference(@RequestParam(value="interviewTimeID") Long interviewTimeID,
                                                           @ApiIgnore Authentication authentication) {
        String userEmail = authService.getEmailByAuthentication(authentication);
        // ?????? interviewID??? Conference Table??? ????????? ?????? ???????????? ConferenceID??? ??????
        Optional<Conference> conference = conferenceService.isConferenceByHost(interviewTimeID);
        if(conference.isEmpty()) {   // ?????? interviewID??? Conference Table??? ????????? ?????? ConferenceID??? ??????
            // [Conference Table] ????????? Conference ?????? ?????? ?????? ??????
            conference = Optional.ofNullable(conferenceService.startConference(interviewTimeID));
        }
        System.out.println(conference.get());
        // [Conference History Table] ???????????? Conference ?????? ?????? -> ?????? ?????? ??????
        ConferenceHistory history = conferenceService.createConferenceHistory(conference.get().getId(), userEmail, 1);
        return ResponseEntity.status(200).body(ConferenceInRes.of(conference.get().getId(),  history.getId()));
    }

    @PostMapping("/end")
    @ApiOperation(value = "Conference ??? ??????")
    @ApiResponses({
            @ApiResponse(code = 200, message = "??????"),
            @ApiResponse(code = 404, message = "????????? ??????"),
            @ApiResponse(code = 500, message = "?????? ??????")
    })
    public ResponseEntity<? extends BaseResponseBody> endConference(@RequestParam(value = "historyID") Long historyID,
                                                                    @RequestParam(value = "conferenceID") Long conferenceID,
                                                                    @RequestParam(value = "interviewTimeID") Long interviewTimeID) {
        // [Conference Table]
        conferenceService.endConference(conferenceID);
        // [Conference History Table] ???????????? ???????????? ??? ????????? ???, Conference??? ????????? ??? ??????
        conferenceService.updateConferenceHistory(historyID, 0);
        // [Applicant Table] interview_time_id ??? ????????? applicant??? ????????? 3?????? ??????
//        conferenceService.modifyApplicantState(interviewTimeID);
        conferenceService.modifyInterviewTimeState(interviewTimeID);
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, "Success"));
    }

    @PostMapping("/in")
    @ApiOperation(value = "Conference ?????? ????????? ??????")
    @ApiResponses({
            @ApiResponse(code = 200, message = "??????"),
            @ApiResponse(code = 404, message = "????????? ??????"),
            @ApiResponse(code = 500, message = "?????? ??????")
    })
    public ResponseEntity<ConferenceInRes> inConference(@RequestParam(value = "interviewTimeID") Long interviewTimeID,
                                             @ApiIgnore Authentication authentication) {
        String userEmail = authService.getEmailByAuthentication(authentication);
        // [Conference Table] ???????????? ??????????????? Conference??? ?????? ?????? ????????? ?????? -> ?????? ???????????? ????????????, ????????? ????????? ?????? ?????? ??????
        Optional<Conference> conference = conferenceService.isConferenceByUser(interviewTimeID);
        // [Conference History Table]
        ConferenceHistory history = conferenceService.createConferenceHistory(conference.get().getId(), userEmail, 1);
        return ResponseEntity.status(200).body(ConferenceInRes.of(conference.get().getId(), history.getId()));
    }

    @PutMapping("/out")
    @ApiOperation(value = "Conference ????????? ???????????? ????????? ??????")
    @ApiResponses({
            @ApiResponse(code = 200, message = "??????"),
            @ApiResponse(code = 404, message = "????????? ??????"),
            @ApiResponse(code = 500, message = "?????? ??????")
    })
    public ResponseEntity<? extends BaseResponseBody> outConference(@RequestParam(value = "historyID") Long historyID) {
        // [Conference History Table]
        conferenceService.updateConferenceHistory(historyID, 0);
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, "Success"));
    }

//    @GetMapping("/info")
//    @ApiOperation(value = "Conference ?????? ?????? ?????? ??????")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "??????"),
//            @ApiResponse(code = 404, message = "????????? ??????"),
//            @ApiResponse(code = 500, message = "?????? ??????")
//    })
//    public ResponseEntity<ConferenceInfoRes> getConferenceInfo(@RequestParam(value = "interviewID") Long interviewID,
//                                                               @RequestParam(value = "conferenceID") Long conferenceID) {
//        // [Interview Table] + [Conference Table] + [User table]
//        ConferenceInfoRes conferenceInfo = conferenceService.getInfoConference(interviewID, conferenceID);
//        return ResponseEntity.status(200).body(conferenceInfo);
//    }

//    @GetMapping("/user")
//    @ApiOperation(value = "?????? Conference ?????? ???????????? ????????? ??????")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "??????"),
//            @ApiResponse(code = 404, message = "????????? ??????"),
//            @ApiResponse(code = 500, message = "?????? ??????")
//    })
//    public ResponseEntity<List<User>> getUserInConference(@RequestParam(value = "conferenceID") Long conferenceID) {
//        // [Conference History Table]
//        List<User> users = conferenceService.userInConference(conferenceID);
//        return ResponseEntity.status(200).body(users);
//    }

//    @PostMapping("/question")
//    @ApiOperation(value = "Conference ?????? ??? ????????? ?????? ??????")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "??????"),
//            @ApiResponse(code = 404, message = "????????? ??????"),
//            @ApiResponse(code = 500, message = "?????? ??????")
//    })
//    public ResponseEntity<? extends BaseResponseBody> createQuestionInConference(@RequestBody QuestionCreateInReq questionInfo) {
//        // [Question Table]
//        conferenceService.createQuestionInConference(questionInfo);
//        return ResponseEntity.status(200).body(BaseResponseBody.of(200, "Success"));
//    }

    @GetMapping("/question")
    @ApiOperation(value = "?????? Interview??? ?????? ?????? ??????")
    @ApiResponses({
            @ApiResponse(code = 200, message = "??????"),
            @ApiResponse(code = 404, message = "????????? ??????"),
            @ApiResponse(code = 500, message = "?????? ??????")
    })
    public ResponseEntity<List<Question>> getQuestionInConference(@RequestParam(value = "interviewID") Long interviewID) {
        // [Question Table]
        List<Question> questions = conferenceService.questionAllInConference(interviewID);
        return ResponseEntity.status(200).body(questions);
    }

    @PostMapping("/dialog/question")
    @ApiOperation(value = "Conference ?????? ??? ?????? ?????? ?????? ??????")
    @ApiResponses({
            @ApiResponse(code = 200, message = "??????"),
            @ApiResponse(code = 404, message = "????????? ??????"),
            @ApiResponse(code = 500, message = "?????? ??????")
    })
    public ResponseEntity<? extends BaseResponseBody> recordQuestionInConference(@RequestBody RecordQuestionInReq questionInfo) {
        // [Dialog Table]
        conferenceService.recordQuestionInConference(questionInfo);
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, "Success"));
    }

    @PostMapping("/dialog/user")
    @ApiOperation(value = "Conference ?????? ??? ?????? ?????? ??????")
    @ApiResponses({
            @ApiResponse(code = 200, message = "??????"),
            @ApiResponse(code = 404, message = "????????? ??????"),
            @ApiResponse(code = 500, message = "?????? ??????")
    })
    public ResponseEntity<? extends BaseResponseBody> recordDialogInConference(@RequestBody RecordDialogInReq dialogInfo,
                                                                               @ApiIgnore Authentication authentication) {
        String userEmail = authService.getEmailByAuthentication(authentication);
        // [Dialog Table]
        conferenceService.recordDialogInConference(userEmail, dialogInfo);
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, "Success"));
    }

//    @PostMapping("/kick")
//    @ApiOperation(value = "Conference ?????? ??? ???????????? ???????????? ????????????")
//    public ResponseEntity<? extends BaseResponseBody> kickUserInConference(@RequestParam(value = "historyID") Long historyID) {
//        // [Conference History Table] historyID??? ?????? ????????? ?????? ???????????? ??????
//        conferenceService.updateConferenceHistory(historyID, 3);
//        return ResponseEntity.status(200).body(BaseResponseBody.of(200, "Success"));
//    }

    @PutMapping("/kick")
    @ApiOperation(value = "Conference ?????? ??? ???????????? ???????????? ????????????")
    @ApiResponses({
            @ApiResponse(code = 200, message = "??????"),
            @ApiResponse(code = 404, message = "????????? ??????"),
            @ApiResponse(code = 500, message = "?????? ??????")
    })
    public ResponseEntity<? extends BaseResponseBody> kickUserInConference(@RequestBody KickUserInReq kickInfo) {
        // [Conference History Table] conferenceID??? userEmail??? ???????????? ?????? ???????????? ?????????????????? ????????? ???, ??? ?????? ?????? ????????? ???????????? ??????
        conferenceService.kickConferenceHistory(kickInfo);
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, "Success"));
    }

//    @GetMapping("/startInfo")
//    @ApiOperation(value = "Conference ?????? ?????? ??????")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "??????"),
//            @ApiResponse(code = 404, message = "????????? ??????"),
//            @ApiResponse(code = 500, message = "?????? ??????")
//    })
//    public ResponseEntity<LocalDateTime> getStartTimeInConference(@RequestParam(value = "conferenceID") Long conferenceID) {
//        LocalDateTime time = conferenceService.getStartTimeInConference(conferenceID);
//        return ResponseEntity.status(200).body(time);
//    }

    @GetMapping("/startInfo")
    @ApiOperation(value = "Conference ?????? ?????? ??????")
    @ApiResponses({
            @ApiResponse(code = 200, message = "??????"),
            @ApiResponse(code = 404, message = "????????? ??????"),
            @ApiResponse(code = 500, message = "?????? ??????")
    })
    public ResponseEntity<String> getStartTimeInConference(@RequestParam(value = "conferenceID") Long conferenceID) {
        String time = conferenceService.getStartTimeInConference(conferenceID)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return ResponseEntity.status(200).body(time);
    }
}