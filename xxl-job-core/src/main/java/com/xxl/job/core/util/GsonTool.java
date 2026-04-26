package com.xxl.job.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xxl.job.core.biz.model.ReturnT;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * @author xuxueli 2020-04-11 20:56:31
 */
public class GsonTool {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .disableHtmlEscaping()
            .create();

    /**
     * Object 转成 json
     *
     * <pre>
     *     String json = GsonTool.toJson(new Demo());
     * </pre>
     *
     * @param src
     * @return String
     */
    public static String toJson(Object src) {
        return GSON.toJson(src);
    }

    /**
     * json 转成 特定的cls的Object
     *
     * <pre>
     *     Demo demo = GsonTool.fromJson(json, Demo.class);
     * </pre>
     *
     * @param json
     * @param classOfT
     * @return
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return GSON.fromJson(json, classOfT);
    }

    public static <T> ReturnT<T> fromReturnJson(String json, Class<T> dataType) {
        Type returnType = TypeToken.getParameterized(ReturnT.class, dataType).getType();
        return GSON.fromJson(json, returnType);
    }

    /**
     * json 转成 特定的cls的 ArrayList
     *
     * <pre>
     *     List<Demo> demoList = GsonTool.fromJsonList(json, Demo.class);
     * </pre>
     *
     * @param json
     * @param classOfT
     * @return
     */
    public static <T> ArrayList<T> fromJsonList(String json, Class<T> classOfT) {
        Type type = TypeToken.getParameterized(ArrayList.class, classOfT).getType();
        return GSON.fromJson(json, type);
    }
}
