package com.smart.retry.core.serializer;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import com.smart.retry.common.serializer.SerializerObject;
import com.smart.retry.common.serializer.SmartSerializer;
import com.smart.retry.common.utils.GsonTool;
import org.springframework.core.DefaultParameterNameDiscoverer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @Author xiaoqiang
 * @Version serializer.java, v 0.1 2025年02月13日 18:48 xiaoqiang
 * @Description: TODO
 */
public class JsonSerializer implements SmartSerializer {

    @Override
    public String serializer(Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        List<SerializerObject> objectList = Lists.newArrayList();

        if (parameters == null || parameters.length == 0) {
            return GsonTool.toJsonString(objectList);
        }
        DefaultParameterNameDiscoverer defaultParameterNameDiscoverer = new DefaultParameterNameDiscoverer();
        String[] names = defaultParameterNameDiscoverer.getParameterNames(method);
        for (int i = 0; i < args.length; i++) {
            SerializerObject serializerObject = new SerializerObject();
            serializerObject.setIndex(i);
            serializerObject.setParamName(names[i]);
            serializerObject.setParamVal(GsonTool.toJsonStringIgnoreNull(args[i]));
            serializerObject.setClassName(parameters[i].getParameterizedType().getTypeName());
            objectList.add(serializerObject);
        }

        return GsonTool.toJsonStringIgnoreNull(objectList);
    }

    @Override
    public Object[] deSerializer(Method method, String serivlizerVal) {

        Parameter[] parameters = method.getParameters();
        if (parameters == null || parameters.length == 0) {
            return new Object[0];
        }

        List<SerializerObject> paramValList = GsonTool.fromJson(serivlizerVal, new TypeToken<List<SerializerObject>>() {
        }.getType());
        Object[] args = new Object[parameters.length];
        for (SerializerObject serializerObject : paramValList) {
            Integer index = serializerObject.getIndex();
            Parameter parameter = parameters[index];
            Type type = TypeToken.get(parameter.getParameterizedType()).getType();
            Object objectVal = GsonTool.fromJson(serializerObject.getParamVal(), type);
            args[index] = objectVal;
        }
        return args;
    }
}
