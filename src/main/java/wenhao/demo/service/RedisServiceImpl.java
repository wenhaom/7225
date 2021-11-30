package wenhao.demo.service;

import wenhao.demo.dao.RedisDao;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Service
public class RedisServiceImpl implements RedisService {

    @Autowired
    RedisDao redisDao;

    public static final String SEP = "____";
    public static final String TYPE = "objectType";
    public static final String ID = "objectId";

    @Autowired
    public RedisServiceImpl() {
    }

    @Override
    public JSONObject getPlan(String key) {
        JSONObject object = new JSONObject();


        Set<String> keys = redisDao.getKeys("*" + key + "*");
        keys.remove(key);
        Map<Object, Object> map = redisDao.findMap(key);
        loadMap(object, map);

        for (String edgeKey: keys) {
            try {
                Set subObjs = redisDao.findSet(edgeKey);
                String attribute = edgeKey.split(SEP)[2];
                if (subObjs.size() == 1) {
                    String subKey = null;
                    for (Object str: subObjs) {
                        subKey = (String)str;
                    }
                    object.put(attribute, getPlan(subKey));
                } else {
                    JSONArray array = new JSONArray();
                    object.put(attribute, array);
                    for (Object str: subObjs) {
                        String subKey = (String)str;
                        array.put(getPlan(subKey));
                    }
                }
            } catch (Exception e){
//                keys.remove(edgeKey);
            }
        }

        return object;
    }

    //reconstruct the JSONObject
    private void loadMap(JSONObject object, Map<Object, Object> map) {
        for(Map.Entry<Object, Object> entry : map.entrySet()) {
            String val = (String) entry.getValue();
            try {
                object.put((String)entry.getKey(), Integer.parseInt(val));
            }
            catch(Exception e) {
                object.put((String)entry.getKey(), val);
            }

        }
    }

    @Override
    public void postPlan(JSONObject object) {
        //BFS
        Queue<JSONObject> queue = new LinkedList<>();
        queue.add(object);
        while (!queue.isEmpty()) {
            JSONObject cur = queue.poll();
            String objectKey = cur.getString(TYPE) + SEP + cur.getString(ID); //e.g.508_plan

            for (String attribute : cur.keySet()) {
                Object obj = cur.get(attribute);
                if (obj instanceof JSONObject) {
                    JSONObject subObj = (JSONObject)obj;
                    String edgeKey = objectKey + SEP + attribute;  //e.g.508_plan_planCostShares
                    String subObjKey = subObj.getString(TYPE) + SEP + subObj.getString(ID);
                    redisDao.insertSet(edgeKey, subObjKey);
                    queue.offer((JSONObject)obj);
                }
                else if (obj instanceof JSONArray) {
                    String edgeKey = objectKey + SEP + attribute;  //e.g.508_plan_linkedPlanServices
                    for (int i = 0; i < ((JSONArray)obj).length(); i++) {
                        JSONObject subObj = ((JSONArray)obj).getJSONObject(i);
                        String subObjKey = subObj.getString(TYPE) + SEP + subObj.getString(ID);
                        redisDao.insertSet(edgeKey, subObjKey);
                        queue.offer(subObj);
                    }
                }
                else {
                    redisDao.insertMap(objectKey, attribute, obj);
                }
            }

        }
    }

    @Override
    public void updatePlan(String key, String value) {

    }

    @Override
    public void patchPlan(String key, JSONObject newObject) {
        for (String attribute : newObject.keySet()) {
            Object obj = newObject.get(attribute);
            String edgeKey = key + SEP + attribute;
            if (obj instanceof JSONObject) {
                JSONObject subObj = (JSONObject)obj;
                String subObjKey = subObj.getString(TYPE) + SEP + subObj.getString(ID);

                Map<Object, Object> map = redisDao.findMap(subObjKey);
                if (map == null || map.size() == 0) {
                    redisDao.insertSet(edgeKey, subObjKey);
                    postPlan(subObj);
                } else {
                    deletePlan(subObjKey);
                    postPlan(subObj);
                }
            }
            else if (obj instanceof JSONArray) {
                for (int i = 0; i < ((JSONArray)obj).length(); i++) {
                    JSONObject subObj = ((JSONArray)obj).getJSONObject(i);
                    String subObjKey = subObj.getString(TYPE) + SEP + subObj.getString(ID);

                    Map<Object, Object> map = redisDao.findMap(subObjKey);
                    if (map == null || map.size() == 0) {
                        redisDao.insertSet(edgeKey, subObjKey);
                        postPlan(subObj);
                    } else {
                        deletePlan(subObjKey);
                        postPlan(subObj);
                    }
                }
            }
        }
    }

    @Override
    public JSONObject deletePlan(String key) {
        JSONObject object = new JSONObject();
        Set<String> keys = redisDao.getKeys("*" + key + "*");
        keys.remove(key);
        Map<Object, Object> map = redisDao.findMap(key);
        loadMap(object, map);
        redisDao.delete(key);

        for (String edgeKey: keys) {
            try {
                Set subObjs = redisDao.findSet(edgeKey);
                redisDao.delete(edgeKey);
                String attribute = edgeKey.split(SEP)[2];
                if (subObjs.size() == 1) {
                    String subKey = null;
                    for(Object str: subObjs) {
                        subKey = (String)str;
                    }
                    object.put(attribute, deletePlan(subKey));
                } else {
                    JSONArray array = new JSONArray();
                    object.put(attribute, array);
                    for (Object str: subObjs) {
                        String subKey = (String)str;
                        array.put(deletePlan(subKey));
                    }
                }
            } catch (Exception e) {
//                keys.remove(edgeKey);
            }
        }

        return object;
    }

    @Override
    public boolean validate(JSONObject jo) {
        return false;
    }

    @Override
    public boolean exist(String key) {
        Map map = redisDao.findMap(key);
        if(map == null || map.size() == 0)
            return false;
        return true;
    }

    @Override
    public void enqueue(String key, JSONObject jo, String requestType) {
        JSONObject baseObj = new JSONObject();
        String curKey;
        if(jo.has(ID))
            curKey = jo.getString(ID);
        else
            curKey = key;

        for(String attribute : jo.keySet()) {
            Object obj = jo.get(attribute);
            if(obj instanceof JSONObject) {
                JSONObject subObj = (JSONObject)obj;
                JSONObject joinObj = new JSONObject();
                JSONObject packet = new JSONObject();
                //change membercostshare to planservice_membercostshare
                if(attribute.equals("planserviceCostShares") && subObj.getString(TYPE).equals("membercostshare")) {
                    joinObj.put("name", "planservice_membercostshare");
                }
                else {
                    joinObj.put("name", subObj.getString(TYPE));
                }
                joinObj.put("parent", curKey);
                subObj.put("plan_service", joinObj);
                packet.put("parent_id", curKey);
                packet.put("document", subObj);
                packet.put("id", subObj.getString(ID));
                packet.put("request", requestType);
                redisDao.enqueue(packet.toString());
            }
            else if (obj instanceof JSONArray) {
                for(int i = 0; i < ((JSONArray)obj).length(); i++) {
                    JSONObject subObj = ((JSONArray)obj).getJSONObject(i);
                    //recursively enqueue
                    enqueue(curKey, subObj, requestType);
                }
            }
            else {
                baseObj.put(attribute, (String)obj);
            }
        }
        if(!baseObj.has(TYPE))
            return;
        JSONObject packet = new JSONObject();
        JSONObject joinObj = new JSONObject();
        joinObj.put("name", baseObj.getString(TYPE));
        //top level objects don't have parent
        if(!baseObj.getString(TYPE).equals("plan")) {
            joinObj.put("parent", key);
            packet.put("parent_id", key);
        }
        baseObj.put("plan_service", joinObj);
        /*
         * {
         *   "parent_id": "",
         *   "id" : "",
         *   "request" : "post",
         *   "document": {
         *     "objectType" : "",
         *     "objectId" : "",
         *     "plan_service" : {
         *         "name" : "",
         *         "parent" : ""
         *     }
         *   }
         * }
         * */
        packet.put("document", baseObj);
        packet.put("id", baseObj.getString(ID));
        packet.put("request", requestType);
        redisDao.enqueue(packet.toString());
    }

    @Override
    public void enqueue(String key) {
        JSONObject packet = new JSONObject();
        Set<String> keys = redisDao.getKeys("*" + key + "*");
        keys.remove(key);

        packet.put("id", key.split(SEP)[1]);
        packet.put("request", "delete");

        for(String edgeKey: keys) {
            try {
                Set subObjs = redisDao.findSet(edgeKey);
                for(Object str: subObjs) {
                    String subKey = (String)str;
                    enqueue(subKey);
                }
            } catch (Exception e) {}
        }
        redisDao.enqueue(packet.toString());
    }

}