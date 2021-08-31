package com.n0thappy.spring.service;

import com.n0thappy.spring.model.Answer;
import com.n0thappy.spring.model.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author shuqinghua
 * @version Id: EnglishWordsServiceImpl.java, v 0.1 2021/8/30 7:25 下午 shuqinghua Exp $$
 */
@Component
public class EnglishWordsAsyncService {

    @Autowired
    private EnglishWordsSyncService service;

    private static final Random random = new Random();


    @Async
    public Future<List<Answer>> getAnswersByQuestion(Question question) {
        CompletableFuture<List<Answer>> future = new CompletableFuture<>();
        future.complete(service.getAnswersByQuestion(question));
        return future;
    }

    @Async
    public Future<String> getKeylineByAnswer(Answer answer) {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.complete(service.getKeylineByAnswer(answer));
        return future;
    }


}
