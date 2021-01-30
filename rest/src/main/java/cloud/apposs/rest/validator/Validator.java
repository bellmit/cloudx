package cloud.apposs.rest.validator;

import cloud.apposs.rest.validator.checker.*;
import cloud.apposs.rest.validator.checker.Number;
import cloud.apposs.util.Param;
import cloud.apposs.util.ReflectUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * OOP验证器，将值映射成对象并进行对象内的字段合法性校验，主要用于
 * 1、restful请求中序列化对象并自动校验参数
 * 2、通过对象一眼便可以看出请求的是什么参数，参数要求格式是什么
 * 参考：https://blog.csdn.net/justry_deng/article/details/86571671
 */
public final class Validator {
    /**
     * 字段解析器列表，业务方也可以自定义并添加
     */
    private static final Map<Class<? extends Annotation>, IChecker> checkers =
            new HashMap<Class<? extends Annotation>, IChecker>();
    static {
        checkers.put(NotNull.class, new NotNullChecker());
        checkers.put(NotEmpty.class, new NotEmptyChecker());
        checkers.put(NotBlank.class, new NotBlankChecker());
        checkers.put(Digits.class, new DigitsChecker());
        checkers.put(Number.class, new NumberChecker());
        checkers.put(Bool.class, new BoolChecker());
        checkers.put(Length.class, new LengthChecker());
        checkers.put(Email.class, new EmailChecker());
        checkers.put(Mobile.class, new MobileChecker());
        checkers.put(Pattern.class, new PatternChecker());
    }

    /**
     * 将json数组解析为对象并校验对象参数合法性
     */
    public static <T> T deserialize(Class<T> clazzType, Param document) throws Exception {
        if (clazzType == null || document == null) {
            throw new IllegalArgumentException();
        }

        // 如果Bean类有继承，递归解析反射所有继承类方法
        T instance = clazzType.newInstance();
        Class<?> clazz = instance.getClass();
        do {
            Map<String, Method> methods = ReflectUtil.getDeclaredMethodMap(clazz);
            doParseOptional(document, methods, instance, clazz);
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return instance;
    }

    /**
     * 添加字段解析器
     */
    public static IChecker addChecker(Class<? extends Annotation> annotation, IChecker checker) {
        return checkers.put(annotation, checker);
    }

    /**
     * 移除字段解析器
     */
    public static IChecker removeChecker(Class<? extends Annotation> annotation) {
        return checkers.remove(annotation);
    }

    private static void doParseOptional(Param document, Map<String, Method> methods, Object object, Class<?> clazz) throws Exception {
        for (Map.Entry<String, Method> entry : methods.entrySet()) {
            String methodName = entry.getKey();
            Method method = entry.getValue();
            doParsePropertyNode(document, methodName, object, clazz, method);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean doParsePropertyNode(Param document, String methodName,
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
            // 检查对应的字段是否有参数检查器
            if (method.getName().startsWith("set")) {
                Field field = modelClazz.getDeclaredField(methodName);
                Annotation[] annotations = field.getAnnotations();
                for (int i = 0; i < annotations.length; i++) {
                    Annotation annotation = annotations[i];
                    IChecker checker = checkers.get(annotation.annotationType());
                    if (checker != null) {
                        nodeVal = checker.check(field, annotation, nodeVal);
                    }
                }
            }
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
                Annotation[] annotations = field.getAnnotations();
                for (int i = 0; i < annotations.length; i++) {
                    Annotation annotation = annotations[i];
                    IChecker checker = checkers.get(annotation.annotationType());
                    if (checker != null) {
                        nodeVal = checker.check(field, annotation, nodeVal);
                    }
                }
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
                Annotation[] annotations = field.getAnnotations();
                for (int i = 0; i < annotations.length; i++) {
                    Annotation annotation = annotations[i];
                    IChecker checker = checkers.get(annotation.annotationType());
                    if (checker != null) {
                        checker.check(field, annotation, childDocList);
                    }
                }
                if (childDocList == null) {
                    return false;
                }
                Map<String, Method> modelMethods = ReflectUtil.getDeclaredMethodMap(genericClazz);
                for (int i = 0; i < childDocList.size(); i++) {
                    Object fieldObject = genericClazz.newInstance();
                    Param childDoc = childDocList.get(i);
                    doParseOptional(childDoc, modelMethods, fieldObject, fieldObject.getClass());
                    fieldList.add(fieldObject);
                }
            }
        } else if (methodTypes[0].equals(Param.class)) {
            // 对象属性为Param对象
            Field field = modelClazz.getDeclaredField(methodName);
            field.setAccessible(true);
            Param fieldParam = (Param) field.get(model);
            if (fieldParam == null) {
                fieldParam = new Param();
                method.invoke(model, fieldParam);
            }
            Param param = document.getParam(methodName);
            Annotation[] annotations = field.getAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                Annotation annotation = annotations[i];
                IChecker checker = checkers.get(annotation.annotationType());
                if (checker != null) {
                    checker.check(field, annotation, param);
                }
            }
            if (param == null) {
                return false;
            }
            for (Map.Entry<String, Object> entry : param.entrySet()) {
                fieldParam.put(entry.getKey(), entry.getValue());
            }
        } else {
            Object childObj = document.getObject(methodName);
            if (childObj instanceof Param) {
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
                Map<String, Method> fieldMethods = ReflectUtil.getDeclaredMethodMap(fieldClazz);
                doParseOptional((Param) childObj, fieldMethods, fieldObject, fieldClazz);
            } else {
                // 对象属性为手动注入实例
                method.invoke(model, childObj);
            }
        }

        return true;
    }
}
