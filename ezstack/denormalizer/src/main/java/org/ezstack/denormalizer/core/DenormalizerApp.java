package org.ezstack.denormalizer.core;

import com.google.common.collect.ImmutableSet;
import org.apache.samza.application.StreamApplication;
import org.apache.samza.config.Config;
import org.apache.samza.operators.KV;
import org.apache.samza.operators.MessageStream;
import org.apache.samza.operators.StreamGraph;
import org.apache.samza.serializers.KVSerde;
import org.apache.samza.serializers.StringSerde;
import org.ezstack.denormalizer.model.*;
import org.ezstack.denormalizer.serde.JsonSerdeV3;
import org.ezstack.ezapp.datastore.api.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class DenormalizerApp implements StreamApplication {

    private static final Logger log = LoggerFactory.getLogger(DenormalizerApp.class);

    public void init(StreamGraph streamGraph, Config config) {

        MessageStream<Update> updates = streamGraph.getInputStream("documents",
                new JsonSerdeV3<>(Update.class));

        MessageStream<DocumentChangePair> documents = updates.flatMap(new DocumentResolver("document-resolver"));

        MessageStream<DocumentMessage> bootstrapperMessages = streamGraph.getInputStream("bootstrapped-document-messages",
                new JsonSerdeV3<>(DocumentMessage.class));

        ElasticsearchIndexer elasticsearchIndexer = new ElasticsearchIndexer("elasticsearch");

        documents.map(changePair -> new WritableResult(changePair.getNewDocument(),
                changePair.getNewDocument().getTable(), WritableResult.Action.INDEX))
                .sink(elasticsearchIndexer);

        documents.flatMap(new DocumentMessageMapper(checkNotNull(config.get("denormalizer.zkHosts")),
                "/rules", TombstoningPolicy.BASED_ON_RULE))
                .partitionBy(DocumentMessage::getPartitionKey, v -> v, KVSerde.of(new StringSerde(), new JsonSerdeV3<>(DocumentMessage.class)), "partition")
                .map(KV::getValue)
                .merge(ImmutableSet.of(bootstrapperMessages))
                .flatMap(new DocumentJoiner("join-store")).sink(elasticsearchIndexer);
    }
}