package com.n0thappy.spring.service;

import com.n0thappy.spring.model.Answer;
import com.n0thappy.spring.model.Question;
import com.n0thappy.spring.util.ThreadPoolUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author shuqinghua
 * @version Id: EnglishWordsSyncService.java, v 0.1 2021/8/30 8:04 下午 shuqinghua Exp $$
 */
@Component
public class EnglishWordsSyncService {

    private static final Random random = new Random();

    public List<Question> getQuestionsByKeyword(String keyword, int questionsNum){
        return IntStream.range(1, questionsNum+1).mapToObj(i -> new Question()).collect(Collectors.toList());
    }

    public List<Answer> getAnswersByQuestion(Question question) {
        List<Answer> answers = IntStream.range(1, 4).mapToObj(i -> new Answer()).collect(Collectors.toList());
//        delay(200+random.nextInt(200));
        delay(200);
        return answers;
    }

    public String getKeylineByAnswer(Answer answer) {
        String keyline = UUID.randomUUID().toString();
//        delay(100+random.nextInt(100));
        delay(100);
        return keyline;
    }

    public List<String> getKeylineByAnswerAsync(List<Answer> answers) {
        // 必须分成两个CompletableFuture来处理，否则会造成顺序执行，而非并行执行
        List<CompletableFuture<String>> collect = answers.stream().map(answer -> CompletableFuture.supplyAsync(() -> getKeylineByAnswer(answer), ThreadPoolUtil.getEnglishLearningPool()))
                .collect(Collectors.toList());
        return collect.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    public void delay(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
