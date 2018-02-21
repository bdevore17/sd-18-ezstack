package org.ezstack.denormalizer.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.samza.operators.functions.SinkFunction;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemStream;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.TaskCoordinator;
import org.ezstack.denormalizer.model.WritableResult;

import java.util.Map;

public class ElasticsearchIndexer implements SinkFunction<WritableResult> {

    private static final ObjectMapper _mapper = new ObjectMapper();

    private static String getESDeletePath(String index, String type, int version) {
        return new String()
                .concat(index)
                .concat("/")
                .concat(type)
                .concat("/")
                .concat("DELETE")
                .concat("/")
                .concat(Integer.toString(version));
    }

    @Override
    public void apply(WritableResult resultMsg, MessageCollector messageCollector, TaskCoordinator taskCoordinator) {

        switch (resultMsg.getOpCode()) {
            case UPDATE:
                messageCollector.send(new OutgoingMessageEnvelope(
                        new SystemStream("elasticsearch", resultMsg.getTable() + "/" + resultMsg.getTable()),
                        resultMsg.getDocument().getKey(), _mapper.convertValue(resultMsg.getDocument(), Map.class)));
                break;
            case DELETE:
                messageCollector.send(new OutgoingMessageEnvelope(
                        new SystemStream("elasticsearch",
                                getESDeletePath(resultMsg.getTable(), resultMsg.getTable(), resultMsg.getDocument().getVersion())),
                        resultMsg.getDocument().getKey(), _mapper.convertValue(resultMsg.getDocument(), Map.class)));
                break;
        }

    }
}
