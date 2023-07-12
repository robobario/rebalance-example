import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;

class RebalanceExceptionsDemo {

    public static void main(String[] args) throws Exception {
        String topic = "test-" + UUID.randomUUID();
        Map<String, Object> adminProps = Map.of(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        try (Admin admin = Admin.create(adminProps)) {
            admin.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all().get(10, TimeUnit.SECONDS);
        }

        Map<String, Object> producerProps = Map.of(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps, new StringSerializer(), new StringSerializer());
        AtomicLong toSend = new AtomicLong(1);
        ScheduledExecutorService producerService = Executors.newScheduledThreadPool(1);
        producerService.scheduleWithFixedDelay(() -> {
            producer.send(new ProducerRecord<>(topic, null, String.valueOf(toSend.incrementAndGet())));
        }, 0, 500, TimeUnit.MILLISECONDS);

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 1000);
        consumerProps.put(GROUP_ID_CONFIG, UUID.randomUUID().toString());
        consumerProps.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");
        consumerProps.put(ENABLE_AUTO_COMMIT_CONFIG, false);

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        consume(consumerProps, executorService, "consumer1", topic);
        consume(consumerProps, executorService, "consumer2", topic);
        consume(consumerProps, executorService, "consumer3", topic);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);
        producerService.shutdownNow();
        System.out.println("all consumer threads are dead :(");
    }

    private static void consume(Map<String, Object> consumerProps, ExecutorService executorService, final String consumerName, String topic) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        consumer.subscribe(List.of(topic));
        executorService.submit(() -> {
            try {
                while (true) {
                    pollAndPrint(consumer, consumerName);
                    Thread.sleep(1000);
                    consumer.commitSync();
                }
            }
            catch (Exception e) {
                System.out.println("Failure! Thread dying: " + e.getMessage());
            }
        });
    }

    private static void pollAndPrint(KafkaConsumer<String, String> consumer, String consumerName) {
        ConsumerRecords<String, String> poll = consumer.poll(Duration.ofMillis(100));
        poll.forEach(r -> {
            System.out.println(consumerName + ":" + r.value());
        });
    }
}