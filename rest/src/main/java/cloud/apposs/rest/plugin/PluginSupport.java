package cloud.apposs.rest.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link Plugin}管理器
 */
public final class PluginSupport {
	/** 插件列表 */
    private static final List<Plugin> pluginList = new ArrayList<Plugin>();
    
    public PluginSupport() {
    }
    
    public void addPlugin(Plugin plugin) {
    	if (plugin != null) {
    		pluginList.add(plugin);
    	}
    }
    
    public void removePlugin(Plugin plugin) {
    	if (plugin != null) {
    		pluginList.remove(plugin);
    	}
    }
    
    /**
     * Web容器关闭时的插件销毁
     */
    public void destroy() {
    	for (int i = 0; i < pluginList.size(); i++) {
    		Plugin plugin = pluginList.get(i);
    		plugin.destroy();
    	}
    }
}
