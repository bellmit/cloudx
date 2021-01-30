package cloud.apposs.rest.view;

import cloud.apposs.util.SysUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * 视图解析管理器
 */
public final class ViewResolverSupport<R, P> {
    /**
     * 视图解析器列表
     */
    private final List<ViewResolver<R, P>> viewResolverList = new LinkedList<ViewResolver<R, P>>();

    public void addResolver(ViewResolver<R, P> viewResolver) {
        SysUtil.checkNotNull(viewResolver, "viewResolver");
        viewResolverList.add(viewResolver);
    }

    public void removeResolver(ViewResolver<R, P> viewResolver) {
        SysUtil.checkNotNull(viewResolver, "viewResolver");
        viewResolverList.remove(viewResolver);
    }

    /**
     * 根据视图类型获取视图解析器
     */
    @SuppressWarnings("unchecked")
    public ViewResolver<R, P> getViewResolver(R request, P response, Object result) {
        // 返回结果本身就是业务自己实现的视图渲染，则直接退出不用做匹配查找
        if (result instanceof ViewResolver) {
            return (ViewResolver) result;
        }

        // 遍历匹配逻辑处理返回的结果哪个视图处理合适
        for (ViewResolver<R, P> viewResolver : viewResolverList) {
			if (viewResolver.supports(request, response, result)) {
			    return viewResolver;
            }
        }
        return null;
    }

    public int getViewResolverSize() {
        return viewResolverList.size();
    }
}
