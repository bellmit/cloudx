package cloud.apposs.registry;

import cloud.apposs.logger.Logger;
import cloud.apposs.util.CharsetUtil;
import cloud.apposs.util.FileUtil;
import cloud.apposs.util.IoUtil;
import cloud.apposs.util.Parser;
import cloud.apposs.util.SysUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 服务实例注册到文件中，以文件形式实现多进程间的服务注册，
 * 非线程、进程安全，仅用于单机调试
 */
public class FileRegistry implements IRegistry {
    private final File resource;

    private final Charset charset;

    public FileRegistry(String resource) {
        this(new File(resource), CharsetUtil.UTF_8);
    }

    public FileRegistry(String resource, Charset charset) {
        this(new File(resource), charset);
    }

    public FileRegistry(File resource) {
        this(resource, CharsetUtil.UTF_8);
    }

    public FileRegistry(File resource, Charset charset) {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        if (!resource.isFile()) {
            throw new IllegalArgumentException("resource is not file");
        }
        this.resource = resource;
        this.charset = charset;
    }

    @Override
    public boolean registInstance(ServiceInstance serviceInstance) {
        SysUtil.checkNotNull(serviceInstance, "serviceInstance");

        // 从文件中读取注册实例列表，并更新最新的实例列表
        Map<String, List<ServiceInstance>> services = doReadServicesFromResource(resource, charset);
        String serviceId = serviceInstance.getId();
        if (!services.containsKey(serviceId)) {
            services.put(serviceId, new LinkedList<ServiceInstance>());
        }
        List<ServiceInstance> serviceList = services.get(serviceId);
        if (!serviceList.contains(serviceInstance)) {
            serviceList.add(serviceInstance);
        }

        // 将注册实例写入到文件中
        return doUpdateServicesToResource(resource, charset, services);
    }

    @Override
    public boolean deregistInstance(ServiceInstance serviceInstance) {
        SysUtil.checkNotNull(serviceInstance, "serviceInstance");

        // 从文件中读取注册实例列表，并更新最新的实例列表
        Map<String, List<ServiceInstance>> services = doReadServicesFromResource(resource, charset);
        String serviceId = serviceInstance.getId();
        List<ServiceInstance> serviceList = services.get(serviceId);
        if (serviceList != null) {
            for (Iterator<ServiceInstance> iterator = serviceList.iterator(); iterator.hasNext();) {
                ServiceInstance instance = iterator.next();
                // 已经存在对应的实例，不再重复添加
                if (instance.equals(serviceInstance)) {
                    iterator.remove();
                }
            }
        }

        // 将注册实例写入到文件中
        return doUpdateServicesToResource(resource, charset, services);
    }

    @Override
    public void release() {
    }

    private static boolean doUpdateServicesToResource(File resource, Charset charset, Map<String, List<ServiceInstance>> services) {
        StringBuilder resourceContent = new StringBuilder(64);
        for (Map.Entry<String, List<ServiceInstance>> entry : services.entrySet()) {
            String entryId = entry.getKey();
            List<ServiceInstance> entryList = entry.getValue();
            for (ServiceInstance instance : entryList) {
                resourceContent.append(entryId).append(":").
                        append(instance.getHost()).append(":").append(instance.getPort()).append("\r\n");
            }
        }
        FileUtil.write(resourceContent.toString(), resource, false);
        return true;
    }

    private static Map<String, List<ServiceInstance>> doReadServicesFromResource(File resource, Charset charset) {
        return FileUtil.readLines(resource, charset, new IoUtil.LineProcessor<HashMap<String, List<ServiceInstance>>>() {
            final HashMap<String, List<ServiceInstance>> services = new HashMap<String, List<ServiceInstance>>();

            @Override
            public boolean processLine(String line) throws IOException {
                String[] rawstr = line.split(":");
                if (rawstr.length >= 3) {
                    String serviceId = rawstr[0];
                    String serviceHost = rawstr[1];
                    int servicePort = Parser.parseInt(rawstr[2]);
                    if (!services.containsKey(serviceId)) {
                        services.put(serviceId, new LinkedList<ServiceInstance>());
                    }
                    List<ServiceInstance> serviceList = services.get(serviceId);
                    ServiceInstance instance = new ServiceInstance(serviceId, serviceHost, servicePort);
                    serviceList.add(instance);
                }
                return true;
            }

            @Override
            public void onError(IOException error) {
                Logger.error(error, "initialize %s file registry fail", resource);
            }

            @Override
            public HashMap<String, List<ServiceInstance>> getResult() {
                return services;
            }
        });
    }
}
