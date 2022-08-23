package com.example.demo.src.survey;

import com.example.demo.config.BaseException;
import com.example.demo.config.BaseResponse;

import com.example.demo.config.BaseResponseStatus;
import com.example.demo.src.survey.model.*;

import com.example.demo.src.user.UserDao;
import com.example.demo.src.user.UserProvider;
import com.example.demo.src.user.model.*;
import com.example.demo.utils.JwtService;
import com.example.demo.utils.SHA256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.demo.config.BaseResponseStatus.*;

@Service
public class SurveyService {
    final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final SurveyDao surveyDao;
    private final SurveyProvider surveyProvider;
    private final JwtService jwtService;


    public SurveyService(SurveyDao surveyDao, SurveyProvider surveyProvider, JwtService jwtService) {
        this.surveyDao = surveyDao;
        this.surveyProvider = surveyProvider;
        this.jwtService = jwtService;

    }
    /*
    설문조사 등록
    */
    public PostSurveyRes createSurvey(int userIdx, PostSurveyReq postSurveyReq) throws BaseException{

        try{
            int surveyIdx = surveyDao.insertSurvey(userIdx, postSurveyReq.getSurveyIntroduction(), postSurveyReq.getSurveyTitle(),
                    postSurveyReq.getSurveyCategoryIdx(), postSurveyReq.getDeadlineAt(),
                    postSurveyReq.getPreferGender(), postSurveyReq.getPreferAge(), postSurveyReq.getSurveyTime(),
                    postSurveyReq.getHashtag(), postSurveyReq.getSurveyPointValue(), postSurveyReq.getCouponIdx());

            for (PostSurveyQuestionReq question : postSurveyReq.getSurveyQuestion()){
                int questionIdx = surveyDao.insertSurveyQuestion(surveyIdx, question);
                for(PostSurveyQuestionOptionReq option : question.getQuestionOption()){
                    surveyDao.insertSurveyQuestionOption(questionIdx, option);
                }
            }
            surveyDao.setSurveyTime(surveyIdx); //설문조사 소요 시간 설정
            return new PostSurveyRes(surveyIdx);
        }
        catch (Exception exception) {
            System.out.println(exception);
            throw new BaseException(DATABASE_ERROR);
        }


    }

    public void deleteSurvey(int surveyIdx) throws BaseException{

        try{
            int result = surveyDao.deleteSurvey(surveyIdx);
            if(result == 0){
                throw new BaseException(DELETE_FAIL_POST);
            }
        }
        catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }

    }

    public BaseResponse<String> createSurveyAnswer(int userIdx, int surveyIdx, PostSurveyAnswerReq postSurveyAnswerReq)throws BaseException{
        try{

            List<PostSurveyQuestionAnswerReq>surveyQuestionAnswerReqs = postSurveyAnswerReq.getSurveyQuestionAnswerReqs();

            //주관식, 체크박스 분류해서 저장
            for(int i=0; i<surveyQuestionAnswerReqs.size(); i++){
                int questionIdx = surveyQuestionAnswerReqs.get(i).getQuestionIdx();
                String questionAnswer = surveyQuestionAnswerReqs.get(i).getQuestionAnswer();
                String questionType = surveyDao.getQuestionType(questionIdx);
                if(questionType.equals("Essay")){
                    surveyDao.insertSurveyEssayAnswer(userIdx,questionIdx,questionAnswer);
                } else if(questionType.equals("Checkbox")){
                    String[] str = questionAnswer.substring(1,questionAnswer.length()-1).split(",");
                    for(int j=0; j<str.length; j++){
                        int checkedOptionAnswerIdx =  Integer.parseInt(str[j]);
                        surveyDao.insertSurveyCheckboxAnswer(userIdx,questionIdx,checkedOptionAnswerIdx);
                    }
                }
            }
            //설문조사 참여자 목록 추가
            surveyDao.insertSurveyParticipant(userIdx, surveyIdx);
            String result = "설문조사 답변 등록 성공";
            surveyDao.countParticipant(surveyIdx); //설문조사 참여자 수 증가
            return new BaseResponse<>(result);
        }
        catch (Exception exception) {
            throw new BaseException(DATABASE_ERROR);
        }
    }


}
