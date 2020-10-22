package cloud.apposs.util;

import cloud.apposs.util.JvmInformation.PrimitiveType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static cloud.apposs.util.JvmInformation.CURRENT_JVM_INFORMATION;

public class ReflectUtil {
    public static Object parseObject(String rawval, Class<?> clazz) {
        if (clazz.equals(String.class)) {
            return rawval;
        } else if (clazz.equals(long.class)) {
            try {
                return Long.parseLong(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(Long.class)) {
            try {
                return Long.parseLong(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(Integer.class)) {
            try {
                return Integer.parseInt(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(int.class)) {
            try {
                return Integer.parseInt(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(Short.class)) {
            try {
                return Short.parseShort(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(short.class)) {
            try {
                return Short.parseShort(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(Float.class)) {
            try {
                return Float.parseFloat(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(float.class)) {
            try {
                return Float.parseFloat(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(Double.class)) {
            try {
                return Double.parseDouble(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(double.class)) {
            try {
                return Double.parseDouble(rawval);
            } catch (NumberFormatException e) {
            }
        } else if (clazz.equals(Boolean.class)) {
            return Boolean.parseBoolean(rawval);
        } else if (clazz.equals(boolean.class)) {
            return Boolean.parseBoolean(rawval);
        }

        return null;
    }

    public static Map<String, Method> getDeclaredMethodMap(Class<?> clazz) {
        Map<String, Method> methods = new ConcurrentHashMap<String, Method>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().startsWith("set")) {
                methods.put(StrUtil.lowerFirst(method.getName().substring(3)), method);
            }
        }
        return methods;
    }

    public static List<Method> getDeclaredMethodList(Class<?> clazz) {
        List<Method> methods = new CopyOnWriteArrayList<Method>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().startsWith("set")) {
                methods.add(method);
            }
        }
        return methods;
    }

    public static boolean isFieldExist(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            return field != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static void makeAccessible(Field field) {
        if ((!Modifier.isPublic(field.getModifiers()) ||
                !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
                Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    public static void makeAccessible(Method method) {
        if ((!Modifier.isPublic(method.getModifiers()) ||
                !Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
            method.setAccessible(true);
        }
    }

    public static Object invokeMethod(Object target, Method method, Object... args)
            throws InvocationTargetException, IllegalAccessException {
        makeAccessible(method);
        return method.invoke(target, args);
    }

    /**
     * 获取类所在的包名
     */
    public static String getPackage(Class<?> clazz) {
        if (clazz == null) {
            return  null;
        }
        return clazz.getPackage().getName();
    }

    /**
     * 判断是否为普通类型，而非对象
     */
    public static boolean isGenericType(Class<?> type) {
        return type.equals(String.class) ||
                type.equals(long.class) || type.equals(Long.class) ||
                type.equals(Integer.class) || type.equals(int.class) ||
                type.equals(Short.class) || type.equals(short.class) ||
                type.equals(Float.class) || type.equals(float.class) ||
                type.equals(Double.class) || type.equals(double.class) ||
                type.equals(Boolean.class) || type.equals(boolean.class);
    }

    /**
     * 利用反射获取对象字节大小
     * 参考：
     * http://www.blogjava.net/DLevin/archive/2013/11/01/405822.html
     */
    public static long sizeOf(Object instance) {
        if (instance == null) {
            return 0;
        }

        Class<?> instanceClass = instance.getClass();
        if (instanceClass.isArray()) {
            return getArraySize(instance);
        } else {
            return getObjectSize(instanceClass);
        }
    }

    /**
     * 利用反射递归获取对象字节大小
     * 参考：
     * http://www.blogjava.net/DLevin/archive/2013/11/01/405822.html
     */
    public static long deepSizeOf(Object instance) {
        if (instance == null) {
            return 0;
        }

        Class<?> instanceClass = instance.getClass();
        Field[] fields = instanceClass.getDeclaredFields();
        long size = sizeOf(instance);
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(instance);
                Class<?> fieldType = field.getType();
                if (instanceClass.isArray()) {
                    size += getArraySize(fieldValue);
                } else {
                    size += getObjectSize(fieldType);
                }
            } catch (IllegalAccessException e) {
            }
        }
        return size;
    }

    private static long getArraySize(Object instance) {
        long size = PrimitiveType.getArraySize();
        int length = Array.getLength(instance);
        if (length != 0) {
            Class<?> arrayElementClazz = instance.getClass().getComponentType();
            if (arrayElementClazz.isPrimitive()) {
                size += length * PrimitiveType.forType(arrayElementClazz).getSize();
            } else {
                size += length * PrimitiveType.getReferenceSize();
            }
        }
        if ((size % CURRENT_JVM_INFORMATION.getObjectAlignment()) != 0) {
            size += CURRENT_JVM_INFORMATION.getObjectAlignment() - (size % CURRENT_JVM_INFORMATION.getObjectAlignment());
        }
        return Math.max(size, CURRENT_JVM_INFORMATION.getMinimumObjectSize());
    }

    private static long getObjectSize(Class<?> instanceClass) {
        long size = CURRENT_JVM_INFORMATION.getObjectHeaderSize();
        Stack<Class<?>> classStack = new Stack<Class<?>>();
        for (Class<?> klazz = instanceClass; klazz != null; klazz = klazz.getSuperclass()) {
            classStack.push(klazz);
        }
        while (!classStack.isEmpty()) {
            Class<?> klazz = classStack.pop();

            //assuming default class layout
            int oops = 0;
            int doubles = 0;
            int words = 0;
            int shorts = 0;
            int bytes = 0;
            for (Field f : klazz.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (f.getType().isPrimitive()) {
                    switch (PrimitiveType.forType(f.getType())) {
                        case BOOLEAN:
                        case BYTE:
                            bytes++;
                            break;
                        case SHORT:
                        case CHAR:
                            shorts++;
                            break;
                        case INT:
                        case FLOAT:
                            words++;
                            break;
                        case DOUBLE:
                        case LONG:
                            doubles++;
                            break;
                        default:
                            throw new AssertionError();
                    }
                } else {
                    oops++;
                }
            }
            if (doubles > 0 && (size % PrimitiveType.LONG.getSize()) != 0) {
                long length = PrimitiveType.LONG.getSize() - (size % PrimitiveType.LONG.getSize());
                size += PrimitiveType.LONG.getSize() - (size % PrimitiveType.LONG.getSize());

                while (length >= PrimitiveType.INT.getSize() && words > 0) {
                    length -= PrimitiveType.INT.getSize();
                    words--;
                }
                while (length >= PrimitiveType.SHORT.getSize() && shorts > 0) {
                    length -= PrimitiveType.SHORT.getSize();
                    shorts--;
                }
                while (length >= PrimitiveType.BYTE.getSize() && bytes > 0) {
                    length -= PrimitiveType.BYTE.getSize();
                    bytes--;
                }
                while (length >= PrimitiveType.getReferenceSize() && oops > 0) {
                    length -= PrimitiveType.getReferenceSize();
                    oops--;
                }
            }
            size += PrimitiveType.DOUBLE.getSize() * doubles;
            size += PrimitiveType.INT.getSize() * words;
            size += PrimitiveType.SHORT.getSize() * shorts;
            size += PrimitiveType.BYTE.getSize() * bytes;

            if (oops > 0) {
                if ((size % PrimitiveType.getReferenceSize()) != 0) {
                    size += PrimitiveType.getReferenceSize() - (size % PrimitiveType.getReferenceSize());
                }
                size += oops * PrimitiveType.getReferenceSize();
            }

            if ((doubles + words + shorts + bytes + oops) > 0 && (size % CURRENT_JVM_INFORMATION.getPointerSize()) != 0) {
                size += CURRENT_JVM_INFORMATION.getPointerSize() - (size % CURRENT_JVM_INFORMATION.getPointerSize());
            }
        }
        if ((size % CURRENT_JVM_INFORMATION.getObjectAlignment()) != 0) {
            size += CURRENT_JVM_INFORMATION.getObjectAlignment() - (size % CURRENT_JVM_INFORMATION.getObjectAlignment());
        }
        return Math.max(size, CURRENT_JVM_INFORMATION.getMinimumObjectSize());
    }

    /**
     * 判断注解是否相同
     */
    public static boolean isAnnotationEquals(Annotation a, Class<? extends Annotation> b) {
        if (a == null || b == null) {
            return false;
        }
        return a.annotationType().isAssignableFrom(b);
    }
}
