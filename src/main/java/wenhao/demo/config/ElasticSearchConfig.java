package wenhao.demo.config;

import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ComponentScan("wenhao.demo")
public class ElasticSearchConfig {
    @Bean
    public RestHighLevelClient client() throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http")));

        GetIndexRequest request = new GetIndexRequest();
        request.indices("plan");
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        if(!exists) {
            CreateIndexRequest creatIndexRequest = new CreateIndexRequest("plan");
            try {
                creatIndexRequest.mapping("{\n" +
                        "  \"mappings\": {\n" +
                        "    \"properties\": {\n" +
                        "      \"objectId\" : {\n" +
                        "        \"type\": \"keyword\"\n" +
                        "      },\n" +
                        "      \"plan_service\" : {\n" +
                        "        \"type\": \"join\",\n" +
                        "        \"relations\" : {\n" +
                        "          \"plan\" : [\"membercostshare\", \"planservice\"],\n" +
                        "          \"planservice\" : [\"service\", \"planservice_membercostshare\"]\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "  \n" +
                        "}", XContentType.JSON);
            } catch (IllegalArgumentException e) {}
            ActionListener<CreateIndexResponse> listener =
                    new ActionListener<CreateIndexResponse>() {

                        @Override
                        public void onResponse(CreateIndexResponse createIndexResponse) {

                        }

                        @Override
                        public void onFailure(Exception e) {
                            e.printStackTrace();
                        }
                    };
            client.indices().createAsync(creatIndexRequest, RequestOptions.DEFAULT, listener);
        }
        return client;
    }
}
