package com.n0thappy.spring.handler;

import com.n0thappy.spring.model.Question;
import com.n0thappy.spring.service.EnglishWordsSyncService;
import com.n0thappy.spring.util.ThreadPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * @author shuqinghua
 * @version Id: EnglishLearnningHandler.java, v 0.1 2021/8/30 7:54 下午 shuqinghua Exp $$
 */
@Component
public class EnglishLearningHandler {

    @Autowired
    private EnglishWordsSyncService syncService;

    public List<String> findEnglishLinesSync(String keyword, int questionsNum) {
        List<Question> questions = syncService.getQuestionsByKeyword(keyword, questionsNum);
        return questions.stream().map(question -> syncService.getAnswersByQuestion(question))
                .flatMap(List::stream)
                .map(answer -> syncService.getKeylineByAnswer(answer))
                .collect(Collectors.toList());
    }

    public List<String> findEnglishLinesParallelAsync(String keyword, int questionsNum) {
        List<Question> questions = syncService.getQuestionsByKeyword(keyword, questionsNum);
        // 自定义parallelStream使用的线程池
        ForkJoinPool forkJoinPool = ThreadPoolUtil.getForkJoinPool();
        return forkJoinPool.submit(() -> questions.parallelStream().map(question -> syncService.getAnswersByQuestion(question))
                .flatMap(List::stream)
                .map(answer -> syncService.getKeylineByAnswer(answer))
                .collect(Collectors.toList())).join();
    }

    public List<String> findEnglishLinesAsync(String keyword, int questionsNum) {
        List<Question> questions = syncService.getQuestionsByKeyword(keyword, questionsNum);
        List<CompletableFuture<List<String>>> linesFuture = questions.stream().map(question -> CompletableFuture.supplyAsync(() -> syncService.getAnswersByQuestion(question), ThreadPoolUtil.getEnglishLearningPool()))
                .map(future -> future.thenCompose(answers -> CompletableFuture.supplyAsync(() -> syncService.getKeylineByAnswerAsync(answers), ThreadPoolUtil.getEnglishLearningPool())))
                .collect(Collectors.toList());

        return linesFuture.stream().map(CompletableFuture::join).flatMap(List::stream)
                .collect(Collectors.toList());
    }

}
