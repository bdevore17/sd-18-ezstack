package org.ezstack.ezapp.datastore.api;

import java.util.Map;

public interface DataReader {

    Map<String, Object> getDocument(String table, String key);

    /**
     *
     * @param retentionTimeInMillis how long a query result should be held
     * @param batchSize the amount of records that should be batched at a time
     * @param query
     * @return
     *
     */
    QueryResult getDocuments(long retentionTimeInMillis, int batchSize, Query query);

    QueryResult getDocuments(long retentionTimeInMillis, Query query);

    QueryResult getDocuments(int batchSize, Query query);

    QueryResult getDocuments(Query query);
}
