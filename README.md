Requires:
1. kafka running on localhost:9092
2. jdk 17
3. maven (or run it out of an IDE like IntelliJ IDEA)


Run `mvn clean install && java -cp target/rebalance-example-1.0-SNAPSHOT.jar RebalanceExceptionsDemo`

It is not deterministic but you may see exceptions across all 3 consumers, what we are looking for is
that `consumer3` which commits very near the max.poll.interval.ms should be ejected from the group causing
a rebalance.
```
consumer2:5
consumer2:7
consumer1:9
consumer2:11
consumer3: Failure! Thread dying: CommitFailedException: Offset commit cannot be completed since the consumer is not part of an active group for auto partition assignment; it is likely that the consumer was kicked out of the group.
consumer2:13
consumer2:15
consumer1: Failure! Thread dying: RebalanceInProgressException: Offset commit cannot be completed since the consumer is undergoing a rebalance for auto partition assignment. You can try completing the rebalance by calling poll() and then retry the operation.
consumer2:19
consumer2:21
consumer2: Failure! Thread dying: CommitFailedException: Commit cannot be completed since the group has already rebalanced and assigned the partitions to another member. This means that the time between subsequent calls to poll() was longer than the configured max.poll.interval.ms, which typically implies that the poll loop is spending too much time message processing. You can address this either by increasing max.poll.interval.ms or by reducing the maximum size of batches returned in poll() with max.poll.records.
all consumer threads are dead :(

```
