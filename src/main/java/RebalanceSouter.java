import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;

public class RebalanceSouter implements ConsumerRebalanceListener {

    private final String name;

    public RebalanceSouter(String name){
        this.name = name;
    }
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        System.out.println(name + ": revoked " + partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        System.out.println(name + ": assigned " + partitions);
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        System.out.println(name + ": lost " + partitions);
    }
}
