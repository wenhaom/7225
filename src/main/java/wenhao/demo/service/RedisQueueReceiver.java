package wenhao.demo.service;


import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class RedisQueueReceiver implements MessageListener {
    private RestHighLevelClient client;

    public static final String INDEX = "plan";
    private ActionListener<DeleteResponse> deleteListener;
    private ActionListener<IndexResponse> indexListener;
    private ActionListener<UpdateResponse> updateListener;

    @Autowired
    public RedisQueueReceiver(RestHighLevelClient client) {
        this.client = client;
        deleteListener = new ActionListener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {
                System.out.println(deleteResponse.toString());
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        };

        indexListener = new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                System.out.println(indexResponse.toString());
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        };

        updateListener = new ActionListener<UpdateResponse>() {
            @Override
            public void onResponse(UpdateResponse updateResponse) {
                System.out.println(updateResponse.toString());
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
            }
        };
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String json = message.toString();
        JSONObject obj = new JSONObject(json);

        String document = null;
        if(obj.has("document"))
            document = obj.getJSONObject("document").toString();
        String object_id = obj.getString("id");
        String parent_id = null;
        if(obj.has("parent_id"))
            parent_id = obj.getString("parent_id");
        String request_type = obj.getString("request");

        if(request_type.equals("post")) {
            sendIndex(object_id, parent_id, document);
        }
        else if(request_type.equals("patch")) {
            boolean exist = sendExist(object_id);
            if(exist) {
                sendUpdate(object_id, parent_id, document);
            }
            else {
                sendIndex(object_id, parent_id, document);
            }
        }
        else {
            sendDelete(object_id);
        }
    }

    private void sendIndex(String object_id, String parent_id, String document) {
        IndexRequest indexRequest = new IndexRequest(INDEX)
                .id(object_id)
                .source(document, XContentType.JSON);
        if(parent_id != null) {
            indexRequest.routing(parent_id);
        }
        client.indexAsync(indexRequest, RequestOptions.DEFAULT, indexListener);
    }

    private void sendDelete(String object_id) {
        DeleteRequest deleteRequest = new DeleteRequest(INDEX, object_id);
        client.deleteAsync(deleteRequest, RequestOptions.DEFAULT, deleteListener);
    }

    private boolean sendExist(String object_id) {
        GetRequest getRequest = new GetRequest(INDEX, object_id)
                .fetchSourceContext(new FetchSourceContext(false))
                .storedFields("_none_");
        try {
            return client.exists(getRequest, RequestOptions.DEFAULT);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sendUpdate(String object_id, String parent_id, String document) {
        UpdateRequest updateRequest = new UpdateRequest(INDEX, object_id)
                .doc(document, XContentType.JSON);
        client.updateAsync(updateRequest, RequestOptions.DEFAULT, updateListener);
    }
}