package cloud.apposs.rest.view;

import cloud.apposs.rest.RestConfig;

public abstract class AbstractViewResolver<R, P> implements ViewResolver<R, P> {
    protected RestConfig config;

    @Override
    public ViewResolver build(RestConfig config) {
        this.config = config;
        return this;
    }
}
