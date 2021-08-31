#java8异步编程

看了《java8实战》这本数，学习了java8的异步编程的相关知识，自己想了一个业务场景，通过异步编程来实现，记录一下实战过程中遇到的问题。

业务场景是我之前背英语单词时的一个想法，背单词时遇到一个英语单词不知道真实的使用场景，可以通过这个关键字去quora上搜问题，然后获取其中16个问题，每个问题下取靠前的三个答案，每个答案里面找到包含关键词的一句话作为关键行，然后把所有的关键行打包返回，这样就可以看到这个单词的真实的使用场景了。

这个业务场景下就可以用到异步编程，上面的几个步骤可以抽象成三个方法,getQuestionsByKeyword,getAnswersByQuestion,getKeylineByAnswer;其中后面两个方法可以异步处理，通过Thread.sleep()的方式来模拟方法执行时间。


```java
public List<Question> getQuestionsByKeyword(String keyword, int questionsNum){
    return IntStream.range(1, questionsNum+1).mapToObj(i -> new Question()).collect(Collectors.toList());
}

public List<Answer> getAnswersByQuestion(Question question) {
    List<Answer> answers = IntStream.range(1, 4).mapToObj(i -> new Answer()).collect(Collectors.toList());
    delay(200);
    return answers;
}

public String getKeylineByAnswer(Answer answer) {
    String keyline = UUID.randomUUID().toString();
    delay(100);
    return keyline;
}

```


一般直接想到的处理流程可能如下:
```java
List<Question> questions = syncService.getQuestionsByKeyword(keyword, questionsNum);
questions.stream().map(question -> syncService.getAnswersByQuestion(question))
        .flatMap(List::stream)
        .map(answer -> syncService.getKeylineByAnswer(answer))
        .collect(Collectors.toList());
```

这样写的话，就完全是单线程处理，会非常耗时，上面的例子，我questionsNum设置的是16，耗时为:8236ms; 现在机器大部分都是多核的，我们要充分的压榨机器的性能，应该使用多线程来处理,一个小小的改动如下:
```java
List<Question> questions = syncService.getQuestionsByKeyword(keyword, questionsNum);
questions.parallelStream().map(question -> syncService.getAnswersByQuestion(question))
        .flatMap(List::stream)
        .map(answer -> syncService.getKeylineByAnswer(answer))
        .collect(Collectors.toList());
```
这里的改动是: stream() -> parallelStream()；把流改成了并行流,再执行一次看看耗时: 1049ms; 和上面的同步处理比起来耗时只有同步的1/8;

并行流的耗时最多只能降到这里了吗？还有其他优化的余地吗？我们不妨再想想。

parallelStream既然是并行，那么它用到了几个线程呢？

parallelStream默认使用的线程池为 ForkJoinPool.commonPool(), 线程池大小为系统的cpu核心数,为Runtime.getRuntime().availableProcessors(); 我的电脑是8核的，所以是8个线程,questionsNum设置的是16，所以这8个线程需要处理两轮，如果是16线程的话一轮就可以处理完了,可以节省一半的时间

那么如何自定义本次parallelStream使用的线程池呢?代码如下:

```java
List<Question> questions = syncService.getQuestionsByKeyword(keyword, questionsNum);
ForkJoinPool forkJoinPool = new ForkJoinPool(20);
return forkJoinPool.submit(() -> questions.parallelStream().map(question -> syncService.getAnswersByQuestion(question))
        .flatMap(List::stream)
        .map(answer -> syncService.getKeylineByAnswer(answer))
        .collect(Collectors.toList())).join();
```
再执行一次，耗时为:536ms, 和使用默认线程池比起来，的确节省了一半的时间。

既然增加了线程数就可以减少耗时，那么是不是线程数越多越好呢，实际上也不是。这就带来了一个新的问题，线程池设置多大比较合适？
首先业务有 I/O密集型和CPU密集型两种:

- I/O密集型指的是执行发出去了，cpu不需要计算，只要等待指令执行的结果就行了，比如http请求，数据库交互等，本文章中的例子就是I/O密集型。此类型，线程数和核心数无关，理论上是越大越好，但是太大了也占用系统内存，还带来了线程切换的损耗。默认每个线程1M,但是操作系统会以延迟分配的方式分配内存,因此实际使用的内存要低得多，通常每个线程堆栈占用80至200KB。
- CPU密集型指的是需要消耗cpu资源去计算的,如果cpu一直再使用的话，其他线程需要cpu空闲时才能使用，所以此种情况，线程数和cpu相关，一般线程数为核心数+1
  好了，针对并行流parallelStream来说，我只能优化到这一步了，如果还有进一步优化的方案，欢迎在评论区指出



除了并行流，书中还介绍了另一种异步编程方式: CompletableFuture; CompletableFuture是java8引入的，实现了Future,也对Future做了增强。

本次业务代码里用到的CompletableFutrue的方法主要有3个: CompletableFuture.supplierAsync, thenCompose, join

- CompletableFuture.supplierAsync是工厂方法，获取CompletableFuture实例的，参数为(Supplier<U> supplier, Executor executor), supplier是生产者型函数式接口，ecexutor为自定义线程池,如果不指定线程池，那么和parallelStream一样默认使用ForkJoinPool,大小为核心数
- thenCompose允许你对两个 CompletionStage 进行流水线操作，第一个操作完成时，将其结果作为参数传递给第二个操作
- join和get方法一样，获取future里的值，如果此时异步线程还没结束，此处会阻塞，直到有值，区别在于join不会抛异常
  代码如下，这里就不介绍使用默认线程池的方案了，和并行流类似，直接介绍使用自定义线程池的方案
```java
  // 自定义线程池
  ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("englishLearning-pool-%d").build();
  Executor executor =  new ThreadPoolExecutor(20, 100, 60, TimeUnit.SECONDS,
        new LinkedBlockingDeque<>(100),
        factory,
        new ThreadPoolExecutor.DiscardOldestPolicy());

List<Question> questions = syncService.getQuestionsByKeyword(keyword, questionsNum);
List<CompletableFuture<List<String>>> linesFuture = questions.stream()
        .map(question -> CompletableFuture.supplyAsync(() -> syncService.getAnswersByQuestion(question), ThreadPoolUtil.getEnglishLearningPool()))
        .map(future -> future.thenCompose(answers -> CompletableFuture.supplyAsync(() -> syncService.getKeylineByAnswerAsync(answers), ThreadPoolUtil.getEnglishLearningPool())))
        .collect(Collectors.toList());

return linesFuture.stream().map(CompletableFuture::join).flatMap(List::stream)
        .collect(Collectors.toList());
```
先解释一下线程池创建过程中几个参数的作用:
```java
public ThreadPoolExecutor(int corePoolSize,    // 核心线程数
                          int maximumPoolSize,  // 池里允许的最大线程数
                          long keepAliveTime,   // 核心线程以外的其他线程的空闲时间
                          TimeUnit unit,     // 时间单位
                          BlockingQueue<Runnable> workQueue,  // 队列，当线程数大于核心线程，小于最大线程，且队列满了时，创建新线程
                          ThreadFactory threadFactory,    // 线程工厂
                          RejectedExecutionHandler handler)  // 拒绝策略
```
上面代码里面有两个Stream,之所以没有合并成一个,是因为Stream的延迟特性会引起顺序执行。书中对此原理的解释图为:
<img><img>

代码里有一点需要注意，当调用syncService.getKeylineByAnswer(Answer answer)方法时报错，提示需要的参数为 Answer, 而提供的是List<Answer>,  这里本来想把 Stream<CompletableFutrue<List<Answer>>> 拍平为Stream<CompletableFuture<Answer>> 的，希望有类似于flatmap的方法，结果没找到，无奈之下只能采取另外一个方案，增加一个方法syncService.getKeylineByAnswer(List<Answer> answers), 方法如下:

```java
public List<String> getKeylineByAnswerAsync(List<Answer> answers) {
    List<CompletableFuture<String>> collect = answers.stream()
            .map(answer -> CompletableFuture.supplyAsync(() -> getKeylineByAnswer(answer), ThreadPoolUtil.getEnglishLearningPool()))
            .collect(Collectors.toList());
    return collect.stream().map(CompletableFuture::join).collect(Collectors.toList());
}
```
最后执行一下代码，耗时为:344ms,比优化后的并行流耗时更短

总结一下，java8中有两种异步编程方式,一种是并行流parallelStream,一种是CompletableFuture。日常使用中应该根据实际业务情况来选用，比如如果是计算密集型，没有I/O,推荐使用parallelStream,因为实现简单，便于理解，效率也可能是最高的; 如果是I/O密集型，推荐使用CompletableFuture,灵活性更好，可以定制线程池参数。
