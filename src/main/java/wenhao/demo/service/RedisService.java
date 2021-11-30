package wenhao.demo.service;

import org.json.JSONObject;

public interface RedisService {
    public JSONObject getPlan(final String key);
    public void postPlan(JSONObject object);
    public void updatePlan(final String key, final String value);
    public void patchPlan(String key, JSONObject newObject);
    public JSONObject deletePlan(final String key);
    public boolean validate(JSONObject object);
    public boolean exist(String key);
    public void enqueue(String key, JSONObject object, String requestType);
    public void enqueue(String key);

}
