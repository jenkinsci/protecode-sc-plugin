/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.synopsys.protecode.sc.jenkins.types;
// TODO order
import com.google.gson.*;
import com.synopsys.protecode.sc.jenkins.types.Types.Component;
import com.synopsys.protecode.sc.jenkins.types.Types.Details;

import com.synopsys.protecode.sc.jenkins.types.Types.Results;
import com.synopsys.protecode.sc.jenkins.types.Types.Status;
import com.synopsys.protecode.sc.jenkins.types.Types.Summary;

import java.util.Map;
import java.util.Collection;
import java.lang.reflect.Type;

/**
 *
 * @author pajunen
 */
public class ResultConverter /*implements JsonDeserializer<ScanResult> */{
    /*
    private Integer id;
    private String sha1sum;
    private Summary summary;
    private Collection<Component> components;
    private Status status;
    private String report_url;
    private Details details;
    */
  /*  @Override
    public ScanResult deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Gson gson = new Gson();
        JsonObject jsonObject = json.getAsJsonObject();
        
        int id = jsonObject.get("id").getAsInt();
        Sha1Sum sha1sum = new Sha1Sum(jsonObject.get("sha1sum").getAsString());
        
        //gson.fromJson(json, classOfT)
        Summary summary = (Summary)jsonObject.get("summary").getAsJsonPrimitive();
        Collection<Component> components;
        Status status;
        String report_url;
        Details details;
        
        
// RouteList wardsRoutes = gson.fromJson(elementJson.getValue().getAsJsonArray(), RouteList.class);
        /*for (Map.Entry<String,JsonElement> elementJson : jsonObject.entrySet()){
            JsonArray jsonArray = elementJson.getValue().getAsJsonArray();
            //jsonArray.
        }
        return null;
    }*/
}
