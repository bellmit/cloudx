package cloud.apposs.configure;

import cloud.apposs.util.JsonUtil;
import cloud.apposs.util.Param;
import cloud.apposs.util.ReflectUtil;
import cloud.apposs.util.ResourceUtil;
import cloud.apposs.util.StrUtil;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class JsonConfigParser implements ConfigurationParser {
    @Override
    public final void parse(Object object, String filename) throws Exception {
        parse(object, ResourceUtil.getResource(filename));
    }

    @Override
    public final void parse(Object object, InputStream filestream) throws Exception {
        StringBuilder json = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = filestream.read(b)) != -1; ) {
            json.append(new String(b, 0, n));
        }

        // 如果Bean类有继承，递归解析反射所有继承类方法
        Param document = JsonUtil.parseJsonParam(json.toString());
        Class<?> clazz = object.getClass();
        do {
            Map<String, Method> methods = ReflectUtil.getDeclaredMethodMap(clazz);
            doParseOptional(document, methods, object, clazz);
            clazz = clazz.getSuperclass();
        } while (clazz != null);
    }

    private void doParseOptional(Param document, Map<String, Method> methods, Object object, Class<?> clazz) throws Exception {
        // 是否有指定要读取的JSON节点，默认从根节点开始解析
        if (document == null || document.size() <= 0) {
            return;
        }

        for (Entry<String, Method> entry : methods.entrySet()) {
            String methodName = entry.getKey();
            if (document.containsKey(methodName)) {
                doParsePropertyNode(document, methodName, object, clazz, entry.getValue());
            } else {
                // 通过读取注解方式在JSON中的配置递归解析JSON节点
                Field field = clazz.getDeclaredField(methodName);
                Class<?> fieldType = field.getType();
                Optional fieldOptional = field.getAnnotation(Optional.class);
                Optional typeOptional = fieldType.getAnnotation(Optional.class);
                // 优先属性上的注解
                String annotationName = null;
                if (fieldOptional != null) {
                    annotationName = fieldOptional.value();
                } else if (typeOptional != null) {
                    annotationName = typeOptional.value();
                }
                if (!StrUtil.isEmpty(annotationName)) {
                    // 先获取属性上的值，没有则new一个对象，注意属性对象必须提供空构造函数
                    field.setAccessible(true);
                    Object fieldObject = field.get(object);
                    if (fieldObject == null) {
                        fieldObject = fieldType.newInstance();
                        methods.get(field.getName()).invoke(object, fieldObject);
                    }

                    Param childDoc = document.getParam(annotationName);
                    Map<String, Method> modelMethods = ReflectUtil.getDeclaredMethodMap(fieldType);
                    doParseOptional(childDoc, modelMethods, fieldObject, fieldObject.getClass());
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private boolean doParsePropertyNode(Param document, String methodName,
                Object model, Class<?> modelClazz, Method method) throws Exception {
        // 解析XML PROPERTY属性值并反射调用到类中
        Class<?>[] methodTypes = method.getParameterTypes();
        // setXXX(Object obj)方法必须要有参数
        if (methodTypes.length != 1) {
            return false;
        }

        if (ReflectUtil.isGenericType(methodTypes[0])) {
            // 对象属性为普通数据类型
            Object nodeVal = document.getObject(methodName);
            if (nodeVal != null) {
                method.invoke(model, nodeVal);
            }
        } else if (methodTypes[0].equals(List.class)) {
            // 对象属性为List对象
            Field field = modelClazz.getDeclaredField(methodName);
            field.setAccessible(true);
            // 获取List泛型类型
            ParameterizedType pt = (ParameterizedType)field.getGenericType();
            Class<?> genericClazz = (Class<?>)pt.getActualTypeArguments()[0];
            if (ReflectUtil.isGenericType(genericClazz)) {
                // List值为普通数据类型，直接赋值List
                Object nodeVal = document.getObject(methodName);
                if (nodeVal != null) {
                    method.invoke(model, nodeVal);
                }
            } else {
                // List值为自定义对象类型，递归解析对象并添加到List中
                List<Object> fieldList = (List<Object>) field.get(model);
                if (fieldList == null) {
                    fieldList = new LinkedList<Object>();
                    method.invoke(model, fieldList);
                }
                List<Param> childDocList = document.getList(methodName);
                Map<String, Method> modelMethods = ReflectUtil.getDeclaredMethodMap(genericClazz);
                for (int i = 0; i < childDocList.size(); i++) {
                    Object fieldObject = genericClazz.newInstance();
                    Param childDoc = childDocList.get(i);
                    doParseOptional(childDoc, modelMethods, fieldObject, fieldObject.getClass());
                    fieldList.add(fieldObject);
                }
            }
        } else if (methodTypes[0].equals(Map.class)) {
            // 对象属性为Map对象
            Field field = modelClazz.getDeclaredField(methodName);
            field.setAccessible(true);
            Map<Object, Object> fieldMap = (Map<Object, Object>) field.get(model);
            if (fieldMap == null) {
                fieldMap = new HashMap<Object, Object>();
                method.invoke(model, fieldMap);
            }
            // 获取Map泛型类型
            Type mapType = field.getGenericType();
            if (!ParameterizedType.class.isAssignableFrom(mapType.getClass()) &&
                    ((ParameterizedType) mapType).getActualTypeArguments().length != 2) {
                return false;
            }
            Type[] mapTypes = ((ParameterizedType) mapType).getActualTypeArguments();
            Class<?> valGenericClazz = (Class<?>)mapTypes[1];
            if (ReflectUtil.isGenericType(valGenericClazz)) {
                // Map值为普通数据类型，继续解析Map其下的键值对
                Param param = document.getParam(methodName);
                for (String key : param.keySet()) {
                    fieldMap.put(key, param.getObject(key));
                }
            } else {
                // Map值为自定义对象类型，递归解析对象并添加到Map中
                Param param = document.getParam(methodName);
                Map<String, Method> modelMethods = ReflectUtil.getDeclaredMethodMap(valGenericClazz);
                for (String key : param.keySet()) {
                    Object fieldObject = valGenericClazz.newInstance();
                    Param childDoc = param.getParam(key);
                    doParseOptional(childDoc, modelMethods, fieldObject, fieldObject.getClass());
                    fieldMap.put(key, fieldObject);
                }
            }
        } else {
            // 对象属性为自定义对象
            // 通过读取方法名在JSON中的配置递归解析JSON节点
            Field field = modelClazz.getDeclaredField(methodName);
            // 先获取属性上的值，没有则new一个对象，注意属性对象必须提供空构造函数
            field.setAccessible(true);
            Object fieldObject = field.get(model);
            // 属性值为空并且没有setXXX方法则不走反射
            if (fieldObject == null) {
                fieldObject = field.getType().newInstance();
                method.invoke(model, fieldObject);
            }
            // 有可能属性类继承新增了方法，需要重新获取
            Class<?> fieldClazz = fieldObject.getClass();

            Param childDoc = document.getParam(methodName);
            Map<String, Method> fieldMethods = ReflectUtil.getDeclaredMethodMap(fieldClazz);
            doParseOptional(childDoc, fieldMethods, fieldObject, fieldClazz);
        }

        return true;
    }
}
