Requires:
1. kafka running on localhost:9092
2. jdk 17
3. maven (or run it out of an IDE like IntelliJ IDEA)


Run `mvn clean install && java -cp target/rebalance-example-1.0-SNAPSHOT.jar RebalanceExceptionsDemo`

You should see exceptions across all 3 consumers, what we are looking for is that `consumer1` takes 
more than the max.poll.interval.ms to process it's record and therefore commitSync fails. This then
appears to result in a RebalanceInProgressException in the other consumers.

Here are the results using CooperativeStickyAssignor
```
consumer3: assigned [test-24e4323f-7c37-414d-ad8a-1cb30e8fe50c-2]
consumer1: assigned [test-24e4323f-7c37-414d-ad8a-1cb30e8fe50c-0]
consumer2: assigned [test-24e4323f-7c37-414d-ad8a-1cb30e8fe50c-1]
consumer2:5
consumer2:7
consumer1:9
consumer2:11
consumer2:13
consumer2:15
consumer3:17
consumer2:19
consumer2:21
consumer2:25
consumer2:27
consumer1: Failure! Thread dying: CommitFailedException: Offset commit cannot be completed since the consumer is not part of an active group for auto partition assignment; it is likely that the consumer was kicked out of the group.
consumer3:33
consumer3:37
consumer2:39
consumer2: Failure! Thread dying: RebalanceInProgressException: Offset commit cannot be completed since the consumer is undergoing a rebalance for auto partition assignment. You can try completing the rebalance by calling poll() and then retry the operation.
consumer3: Failure! Thread dying: RebalanceInProgressException: Offset commit cannot be completed since the consumer is undergoing a rebalance for auto partition assignment. You can try completing the rebalance by calling poll() and then retry the operation.
all consumer threads are dead :(
```

If you switch to RangeAssignor then it appears only the consumer that is taking too long to process records throws an exception, the other consumers see a revoke+assign and continue consuming.
```
consumer2: assigned [test-dfb111cd-99c6-4000-98da-855f23a1b426-1]
consumer3: assigned [test-dfb111cd-99c6-4000-98da-855f23a1b426-2]
consumer1: assigned [test-dfb111cd-99c6-4000-98da-855f23a1b426-0]
consumer2:5
consumer2:7
consumer1:9
consumer2:11
consumer2:13
consumer2:15
consumer3:17
consumer2:19
consumer2:21
consumer2:25
consumer2:27
consumer1: Failure! Thread dying: CommitFailedException: Offset commit cannot be completed since the consumer is not part of an active group for auto partition assignment; it is likely that the consumer was kicked out of the group.
consumer3:33
consumer3:37
consumer2:39
consumer2: revoked [test-dfb111cd-99c6-4000-98da-855f23a1b426-1]
consumer3: revoked [test-dfb111cd-99c6-4000-98da-855f23a1b426-2]
consumer2: assigned [test-dfb111cd-99c6-4000-98da-855f23a1b426-0, test-dfb111cd-99c6-4000-98da-855f23a1b426-1]
consumer3: assigned [test-dfb111cd-99c6-4000-98da-855f23a1b426-2]
consumer3:41
consumer2:9
consumer2:23
consumer2:29
consumer2:31
consumer2:35
consumer2:43
consumer2:45
consumer2:47
consumer2:49
consumer3:51
consumer2:53
consumer2:55
consumer2:57
consumer2:59
consumer2:61
consumer2:63
consumer2:65
```
