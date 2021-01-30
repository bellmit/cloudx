package cloud.apposs.ioc;

import cloud.apposs.ioc.annotation.Component;
import cloud.apposs.ioc.annotation.Autowired;
import cloud.apposs.ioc.annotation.Prototype;
import cloud.apposs.util.AntPathMatcher;
import cloud.apposs.util.CharsetUtil;
import cloud.apposs.util.ClassUtil;
import cloud.apposs.util.ReflectUtil;
import cloud.apposs.util.ResourceUtil;
import cloud.apposs.util.StrUtil;
import cloud.apposs.util.SysUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Bean工厂，集中管理Bean对象和对应Bean对象方法，IOC容器
 * 凡是使用{@link Component}注解的对象均会创建并添加到IOC容器中
 */
public final class BeanFactory {
    private static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 类定义集
     */
    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<String, BeanDefinition>();

    /**
     * 对象集
     */
    private final Map<Class<?>, Object> beans = new ConcurrentHashMap<Class<?>, Object>();

    /**
     * 编码
     */
    private String charset = CharsetUtil.UTF_8.name();

    /**
     * 包扫描类过滤器，匹配的注入到IOC容器中，不匹配不注入到IOC容器
     */
    private final List<TypeFilter> includeFilters = new LinkedList<TypeFilter>();
    private final List<TypeFilter> excludeFilters = new LinkedList<TypeFilter>();

    public BeanFactory() {
        this.includeFilters.add(new AnnotationTypeFilter(Component.class));
    }

    /**
     * 扫描包类并自动装载要注入的Class对象到容器中
     */
    public void load(String... basePackages) throws BeansException {
        if (basePackages == null || basePackages.length <= 0) {
            throw new IllegalArgumentException("basePackages");
        }
        loadBeanDefinition(basePackages);
        finishBeanInitialization();
    }

    /**
     * 获取Bean实例，不存在该实例则自动创建对象
     */
    public <T> T getBean(Class<T> beanClass) throws BeansException {
        SysUtil.checkNotNull(beanClass, "beanClass");
        if (!beanDefinitions.containsKey(beanClass.getName())) {
            throw new BeanDefinitionNotFoundException("No bean named '" + beanClass + "'");
        }

        // 非单例对象直接创建返回出去，不用查找
        BeanDefinition beanDefinition = beanDefinitions.get(beanClass.getName());
        if (beanDefinition.isPrototype()) {
            return doCreateBean(beanClass);
        }

        // 单例对象看是否有在IOC容器中，没有则创建一个
        return doGetSingletonBean(beanClass);
    }

    /**
     * 根据父类类型获取最近一个实现的子类对象
     */
    public <T> T getBeanHierarchy(Class<T> beanType) throws BeansException {
        List<T> beanList = getBeanHierarchyList(beanType);
        if (beanList == null || beanList.isEmpty()) {
            return null;
        }
        return beanList.get(beanList.size() - 1);
    }

    /**
     * 根据父类类型获取所有实现的子类对象
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeanHierarchyList(Class<T> beanType) throws BeansException {
        List<T> beanList = new LinkedList<T>();
        for (BeanDefinition beanDefinition : beanDefinitions.values()) {
            Class<?> beanClass = beanDefinition.getBeanClass();
            if (beanType.isAssignableFrom(beanClass) && !beanType.equals(beanClass)) {
                beanList.add((T) getBean(beanClass));
            }
        }
        return beanList;
    }

    /**
     * 根据类注解获取第一个配置类注解的对象
     */
    public <T> T getBeanAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
        List<T> beanList = getBeanAnnotationList(annotationType);
        if (beanList == null || beanList.isEmpty()) {
            return null;
        }
        return beanList.get(0);
    }

    /**
     * 根据类注解获取所有配置类注解的对象
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeanAnnotationList(Class<? extends Annotation> annotationType) throws BeansException {
        List<T> beanList = new LinkedList<T>();
        for (BeanDefinition beanDefinition : beanDefinitions.values()) {
            Class<?> beanClass = beanDefinition.getBeanClass();
            if (beanClass.isAnnotationPresent(annotationType)) {
                beanList.add((T) getBean(beanClass));
            }
        }
        return beanList;
    }

    /**
     * 根据父类类型获取所有实现的子类Class
     */
    public List<Class<?>> getClassHierarchyList(Class<?> beanType) throws BeansException {
        List<Class<?>> beanList = new LinkedList<Class<?>>();
        for (BeanDefinition beanDefinition : beanDefinitions.values()) {
            Class<?> beanClass = beanDefinition.getBeanClass();
            if (beanType.isAssignableFrom(beanClass) && !beanType.equals(beanClass)) {
                beanList.add(beanClass);
            }
        }
        return beanList;
    }

    /**
     * 根据类注解获取所有配置类注解的Class
     */
    public List<Class<?>> getClassAnnotationList(Class<? extends Annotation> annotationType) throws BeansException {
        List<Class<?>> beanList = new LinkedList<Class<?>>();
        for (BeanDefinition beanDefinition : beanDefinitions.values()) {
            Class<?> beanClass = beanDefinition.getBeanClass();
            if (beanClass.isAnnotationPresent(annotationType)) {
                beanList.add(beanClass);
            }
        }
        return beanList;
    }

    /**
     * 手动添加Bean对象到IOC容器中管理
     */
    public boolean addBean(Object bean) {
        return addBean(bean, true);
    }

    /**
     * 手动添加Bean对象到IOC容器中管理
     */
    public boolean addBean(Object bean, boolean ignoreFilter) {
        if (doInitialBeanDefinition(bean.getClass().getName(), ignoreFilter)) {
            beans.put(bean.getClass(), bean);
            return true;
        }
        return false;
    }

    public boolean addIncludeFilter(TypeFilter filter) {
        if (filter == null || includeFilters.contains(filter)) {
            return false;
        }
        includeFilters.add(filter);
        return true;
    }

    public boolean addExcludeFilter(TypeFilter filter) {
        if (filter == null || excludeFilters.contains(filter)) {
            return false;
        }
        excludeFilters.add(filter);
        return true;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * 加载扫描包下的所有Class类，路径Ant正则规则匹配
     */
    private void loadBeanDefinition(String... basePackages) throws BeansException {
        try {
            for (String basePackage : basePackages) {
                if (StrUtil.isEmpty(basePackage)) {
                    continue;
                }
                String packageSearchPath = ResourceUtil.convertResourcePath(basePackage) + '/' + DEFAULT_RESOURCE_PATTERN;
                String rootDirPath = determineRootDir(packageSearchPath);
                String subPattern = packageSearchPath.substring(rootDirPath.length());
                Enumeration<URL> resources = ResourceUtil.getResources(rootDirPath);
                while (resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    if (ResourceUtil.isJarURL(resource)) {
                        addJarResourceBeanDefinition(resource, basePackage, subPattern);
                    } else {
                        addFileResourceBeanDefinition(resource, basePackage, subPattern);
                    }
                }
            }
        } catch (Exception e) {
            throw new BeanDefinitionStoreException("bean definition failure during classpath scanning", e);
        }
    }

    /**
     * Bean对象初始化
     */
    private void finishBeanInitialization() throws BeansException {
        for (BeanDefinition beanDefinition : beanDefinitions.values()) {
            getBean(beanDefinition.getBeanClass());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T doGetSingletonBean(Class<T> beanClass) throws BeansException {
        synchronized (beans) {
            T bean = (T) beans.get(beanClass);
            if (bean != null) {
                return bean;
            }
            bean = doCreateBean(beanClass);
            beans.put(beanClass, bean);
            return bean;
        }
    }

    /**
     * 根据Bean名称创建Bean对象
     *
     * @param beanClass Bean类名
     * @return 成功创建Bean对象
     * @throws BeansException 创建对象失败则抛出此异常
     */
    private <T> T doCreateBean(Class<T> beanClass) throws BeansException {
        try {
            // 构造方法注入
            T beanObject = doCreateContructor(beanClass);
            // 字段注入
            doInjectField(beanObject, beanClass);
            // 方法注入
            doInjectMethod(beanObject, beanClass);
            // 如果Bean类实现了{@link Initializable}接口则进行接口初始化操作
            if (beanObject instanceof Initializable) {
                ((Initializable) beanObject).initialize();
            }
            return beanObject;
        } catch (InstantiationException e) {
            throw new BeanCreationException("Bean[" + beanClass + "] initialize fail", e);
        }
    }

    /**
     * IOC容器中类初始化，
     * 默认用缺省构造方法，没有是判断构造方法是否是{@link Autowired}注解进行构造方法反向注入
     * 参考：https://blog.csdn.net/qq_41737716/article/details/85596817
     * 注意：
     * 1. {@link Autowired}的构造方法参数所在的实现类必须要添加{@link Component}注解，已经由IOC容器管理了，
     * 2. IOC容器是无法解决类循环依赖问题的，底层通过之前扫描的{@link BeanDefinition}来初始化实例只会触发两个循环依赖的类彼此都找不到
     */
    @SuppressWarnings("unchecked")
    private <T> T doCreateContructor(Class<T> beanClass) throws BeansException {
        Constructor<T>[] constructors = (Constructor<T>[]) beanClass.getDeclaredConstructors();
        Constructor<T> matchedConstructor = null;
        if (constructors.length == 1) {
            matchedConstructor = constructors[0];
        } else {
            for (int i = 0; i < constructors.length; i++) {
                Constructor<T> constructor = constructors[i];
                if (constructor.getParameterCount() == 0) {
                    // 缺省无参构造方法
                    matchedConstructor = constructor;
                    break;
                }
                if (constructor.isAnnotationPresent(Autowired.class)) {
                    matchedConstructor = constructor;
                }
            }
        }
        if (matchedConstructor == null) {
            throw new BeanCreationException("No matched bean[" + beanClass + "] constructor");
        }
        try {
            Class<?>[] parameterTypes = matchedConstructor.getParameterTypes();
            if (parameterTypes == null) {
                return beanClass.newInstance();
            }
            Object[] parameterArgs = new Object[parameterTypes.length];
            for (int j = 0; j < parameterTypes.length; j++) {
                Class<?> parameterType = parameterTypes[j];
                Object implementInstance = null;
                // 获取 Bean 构造方法参数对应的注入实现类
                if (Modifier.isInterface(parameterType.getModifiers())) {
                    implementInstance = getBeanHierarchy(parameterType);
                } else {
                    implementInstance = getBean(parameterType);
                }
                if (implementInstance == null) {
                    throw new BeanCreationException("Autowired method[" + parameterType + "] dependency not found");
                }
                if (implementInstance == null) {
                    throw new BeanCreationException("Autowired method[" + parameterType + "] dependency not found");
                }
                parameterArgs[j] = implementInstance;
            }
            return matchedConstructor.newInstance(parameterArgs);
        } catch (InstantiationException e) {
            throw new BeanCreationException("Bean[" + beanClass + "] constructor initialize fail", e);
        } catch (IllegalAccessException e) {
            throw new BeanCreationException("Bean[" + beanClass + "] constructor access fail", e);
        } catch (InvocationTargetException e) {
            throw new BeanCreationException("Bean[" + beanClass + "] constructor invocation fail", e);
        }
    }

    /**
     * 获取Bean对象中有{@link Autowired}注解的字段并注入依赖的实现对象
     */
    private void doInjectField(Object beanObject, Class<?> beanClass) throws BeansException {
        // 获取 Bean 类中所有的字段（不包括父类中的方法）
        Field[] beanFields = beanClass.getDeclaredFields();
        for (int i = 0; i < beanFields.length; i++) {
            Field beanField = beanFields[i];
            if (!beanField.isAnnotationPresent(Autowired.class)) {
                continue;
            }
            // 获取 Bean 字段对应的接口
            Class<?> fieldType = beanField.getType();
            Object implementInstance = null;
            // 获取 Bean 字段对应的实现类
            if (Modifier.isInterface(fieldType.getModifiers())) {
                implementInstance = getBeanHierarchy(fieldType);
            } else {
                implementInstance = getBean(fieldType);
            }
            if (implementInstance == null) {
                throw new BeanCreationException("Autowired field[" + beanField + "] dependency not found");
            }
            ReflectUtil.makeAccessible(beanField);
            try {
                beanField.set(beanObject, implementInstance);
            } catch (IllegalArgumentException e) {
                throw new BeanCreationException("Autowired field[" + beanField + "] dependency invalid argument", e);
            } catch (IllegalAccessException e) {
                throw new BeanCreationException("Autowired field[" + beanField + "] dependency access error", e);
            }
        }
    }

    /**
     * 获取Bean对象中有{@link Autowired}注解的方法并注入依赖的实现对象
     */
    private void doInjectMethod(Object beanObject, Class<?> beanClass) throws BeansException {
        // 获取 Bean 类中所有的字段（不包括父类中的方法）
        Method[] beanMethods = beanClass.getDeclaredMethods();
        for (int i = 0; i < beanMethods.length; i++) {
            Method beanMethod = beanMethods[i];
            if (!beanMethod.isAnnotationPresent(Autowired.class)) {
                continue;
            }
            // 获取 Bean 字段对应的接口
            Class<?>[] parameterTypes = beanMethod.getParameterTypes();
            if (parameterTypes == null) {
                continue;
            }
            Object[] parameterArgs = new Object[parameterTypes.length];
            for (int j = 0; j < parameterTypes.length; j++) {
                Class<?> parameterType = parameterTypes[j];
                Object implementInstance = null;
                // 获取 Bean 方法参数对应的实现类
                if (Modifier.isInterface(parameterType.getModifiers())) {
                    implementInstance = getBeanHierarchy(parameterType);
                } else {
                    implementInstance = getBean(parameterType);
                }
                if (implementInstance == null) {
                    throw new BeanCreationException("Autowired method[" + parameterType + "] dependency not found");
                }
                if (implementInstance == null) {
                    throw new BeanCreationException("Autowired method[" + parameterType + "] dependency not found");
                }
                parameterArgs[j] = implementInstance;
            }

            ReflectUtil.makeAccessible(beanMethod);
            try {
                beanMethod.invoke(beanObject, parameterArgs);
            } catch (IllegalAccessException e) {
                throw new BeanCreationException("Autowired method[" + beanMethod + "] dependency access error", e);
            } catch (InvocationTargetException e) {
                throw new BeanCreationException("Autowired method[" + beanMethod + "] dependency access error", e);
            }
        }
    }

    private void addJarResourceBeanDefinition(URL resource,
            String packageName, String pathPattern) throws IOException, BeansException {
        JarURLConnection jarURLConnection = (JarURLConnection) resource.openConnection();
        JarFile jarFile = jarURLConnection.getJarFile();
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            if (jarEntry.isDirectory()) {
                continue;
            }
            String jarEntryName = jarEntry.getName();
            // 过滤不是class文件和不在basePack包名下的类
            String fullPattern = StrUtil.replace(packageName, ".", "/");
            if (!pathPattern.startsWith("/")) {
                fullPattern += "/";
            }
            fullPattern = fullPattern + StrUtil.replace(pathPattern, File.separator, "/");
            if (pathMatcher.matchStart(fullPattern, jarEntryName)) {
                String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                doInitialBeanDefinition(className);
            }
        }
    }

    private void addFileResourceBeanDefinition(URL resource,
            String packageName, String pathPattern) throws Exception {
        // 解决路径空格问题
        String packagePath = resource.getPath().replaceAll("%20", " ");
        // 解决中文路径乱码问题
        packagePath = java.net.URLDecoder.decode(packagePath, charset);
        File packageRootDir = new File(packagePath);
        if (!packageRootDir.exists()) {
            throw new BeansException("base package dir '" + packageRootDir.getAbsolutePath() + "' not exists");
        }
        String fullPattern = StrUtil.replace(packageRootDir.getAbsolutePath(), File.separator, "/");
        if (!pathPattern.startsWith("/")) {
            fullPattern += "/";
        }
        fullPattern = fullPattern + StrUtil.replace(pathPattern, File.separator, "/");
        doAddFileResourceBeanDefinition(packageRootDir, packageName, fullPattern);
    }

    private void doAddFileResourceBeanDefinition(final File packageFile,
            final String packageName, final String fullPattern) throws BeansException {
        // 获取包名路径下的Class文件或目录
        File[] files = packageFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String currentPath = StrUtil.replace(file.getAbsolutePath(), File.separator, "/");
                return (file.isFile() && pathMatcher.matchStart(fullPattern, currentPath + "/") || file.isDirectory());
            }
        });
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (file.isFile()) {
                    // 获取类名
                    String className = determinePackageName(packageName) + "." + fileName.substring(0, fileName.lastIndexOf("."));
                    doInitialBeanDefinition(className);
                } else {
                    // 获取子包名
                    String subPackageName = determinePackageName(packageName) + "." + fileName;
                    doAddFileResourceBeanDefinition(file, subPackageName, fullPattern);
                }
            }
        }
    }


    private boolean doInitialBeanDefinition(String className) throws BeansException {
        return doInitialBeanDefinition(className, false);
    }

    /**
     * 将匹配到的要注入的类对象添加到Ioc容器管理
     *
     * @param  className 类名
     * @param  ignoreFilter 是否忽略类校验，为true时只用于内部系统调用
     * @return 命中过滤器返回true
     */
    private boolean doInitialBeanDefinition(String className, boolean ignoreFilter) throws BeansException {
        try {
            Class<?> clazz = ClassUtil.loadClass(className);

            if (ignoreFilter) {
                boolean prototype = false;
                if (clazz.isAnnotationPresent(Prototype.class)) {
                    prototype = true;
                }
                BeanDefinition beanDefinition = new BeanDefinition(clazz, prototype);
                beanDefinitions.put(className, beanDefinition);
                return true;
            }

            for (TypeFilter filter : this.excludeFilters) {
                if (filter.match(clazz)) {
                    return false;
                }
            }
            for (TypeFilter filter : includeFilters) {
                if (filter.match(clazz)) {
                    boolean prototype = false;
                    if (clazz.isAnnotationPresent(Prototype.class)) {
                        prototype = true;
                    }
                    BeanDefinition beanDefinition = new BeanDefinition(clazz, prototype);
                    beanDefinitions.put(className, beanDefinition);
                    return true;
                }
            }

            return false;
        } catch (ClassNotFoundException e) {
            throw new BeanDefinitionNotFoundException(className, e);
        }
    }

    private String determinePackageName(String location) {
        int prefixEnd = location.indexOf(":") + 1;
        int rootDirEnd = location.length();
        while (rootDirEnd > prefixEnd && pathMatcher.isPattern(location.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = location.lastIndexOf('.', rootDirEnd - 2);
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd);
    }

    private String determineRootDir(String location) {
        int prefixEnd = location.indexOf(":") + 1;
        int rootDirEnd = location.length();
        while (rootDirEnd > prefixEnd && pathMatcher.isPattern(location.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd);
    }

    /**
     * 触发释放容器内实例资源
     */
    public void destroy() {
        for (Object bean : beans.values()) {
            if (bean instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) bean).close();
                } catch (Exception e) {
                }
            }
        }
    }
}
