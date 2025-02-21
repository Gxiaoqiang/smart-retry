package com.smart.retry.common.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author gao.gwq
 * @version 1.0
 * @date 2022/4/18  13:52
 * @Description TODO
 */
public class GsonTool {
    private static Gson GSON = null;
    private static final Gson GSON_NULL; // 不过滤空值

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.enableComplexMapKeySerialization() //当Map的key为复杂对象时,需要开启该方法
                //.serializeNulls() //当字段值为空或null时，依然对该字段进行转换
                //.excludeFieldsWithoutExposeAnnotation()//打开Export注解，但打开了这个注解,副作用，要转换和不转换都要加注解
                .setDateFormat("yyyy-MM-dd HH:mm:ss")//序列化日期格式  "yyyy-MM-dd"
                //.setPrettyPrinting() //自动格式化换行
                .disableHtmlEscaping(); //防止特殊字符出现乱码

        GSON = gsonBuilder.create();
        // Get the date adapter
        TypeAdapter<Date> dateTypeAdapter = GSON.getAdapter(Date.class);

        // Ensure the DateTypeAdapter is null safe
        TypeAdapter<Date> safeDateTypeAdapter = dateTypeAdapter.nullSafe();
        GSON = new GsonBuilder()
                .registerTypeAdapter(Date.class, safeDateTypeAdapter)
                .create();
        GsonBuilder gsonBuilder1 = new GsonBuilder();

        gsonBuilder1.enableComplexMapKeySerialization() //当Map的key为复杂对象时,需要开启该方法
                .serializeNulls() //当字段值为空或null时，依然对该字段进行转换
                //.excludeFieldsWithoutExposeAnnotation()//打开Export注解，但打开了这个注解,副作用，要转换和不转换都要加注解
                .setDateFormat("yyyy-MM-dd HH:mm:ss")//序列化日期格式  "yyyy-MM-dd"
                //.setPrettyPrinting() //自动格式化换行
                .disableHtmlEscaping(); //防止特殊字符出现乱码

        GSON_NULL = new GsonBuilder()
                .registerTypeAdapter(Date.class, safeDateTypeAdapter)
                .create();
    }

    //获取gson解析器
    public static Gson getGson() {
        return GSON;
    }

    //获取gson解析器 有空值 解析
    public static Gson getWriteNullGson() {
        return GSON_NULL;
    }

    /**
     * 根据对象返回json  过滤空值字段
     */
    public static String toJsonStringIgnoreNull(Object object) {
        return GSON.toJson(object);
    }

    /**
     * 根据对象返回json  不过滤空值字段
     */
    public static String toJsonString(Object object) {
        return GSON_NULL.toJson(object);
    }

    /**
     * 将字符串转化对象
     *
     * @param json     源字符串
     * @param classOfT 目标对象类型
     * @param <T>
     * @return
     */
    public static <T> T strToJavaBean(String json, Class<T> classOfT) {
        return GSON.fromJson(json, classOfT);
    }

    /**
     * 将json转化为对应的实体对象
     * new TypeToken<List<T>>() {}.getType()
     * new TypeToken<Map<String, T>>() {}.getType()
     * new TypeToken<List<Map<String, T>>>() {}.getType()
     */
    public static <T> T fromJson(String json, Type typeOfT) {


        return GSON.fromJson(json, typeOfT);
    }

    /**
     * 转成list
     *
     * @param gsonString
     * @param cls
     * @return
     */
    public static <T> List<T> strToList(String gsonString, Class<T> cls) {
        return GSON.fromJson(gsonString, new TypeToken<List<T>>() {
        }.getType());
    }

    /**
     * 转成list中有map的
     *
     * @param gsonString
     * @return
     */
    public static <T> List<Map<String, T>> strToListMaps(String gsonString) {
        return GSON.fromJson(gsonString, new TypeToken<List<Map<String, String>>>() {
        }.getType());
    }

    /**
     * 转成map
     *
     * @param gsonString
     * @return
     */
    public static <T> Map<String, T> strToMaps(String gsonString) {
        return GSON.fromJson(gsonString, new TypeToken<Map<String, T>>() {
        }.getType());
    }

    /**
     * json 转成 特定的 rawClass<classOfT> 的Object
     *
     * @param json
     * @param classOfT
     * @param argClassOfT
     * @return
     */
    public static <T> T fromJson(String json, Class<T> classOfT, Class argClassOfT) {

        Type type = new ParameterizedType4ReturnT(classOfT, new Class[]{argClassOfT});
        return GSON.fromJson(json, type);
    }

    public static class ParameterizedType4ReturnT implements ParameterizedType {
        private final Class raw;
        private final Type[] args;

        public ParameterizedType4ReturnT(Class raw, Type[] args) {
            this.raw = raw;
            this.args = args != null ? args : new Type[0];
        }

        @Override
        public Type[] getActualTypeArguments() {
            return args;
        }

        @Override
        public Type getRawType() {
            return raw;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    /**
     * json 转成 特定的cls的list
     *
     * @param json
     * @param classOfT
     * @return
     */
    public static <T> List<T> fromJsonList(String json, Class<T> classOfT) {
        return GSON.fromJson(
                json,
                new TypeToken<List<T>>() {
                }.getType()
        );
    }


}
