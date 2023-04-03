package cn.twelvet.websocket.netty.standard;

import cn.twelvet.websocket.netty.annotation.WebSocketEndpoint;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

/**
 * @author twelvet
 * @WebSite www.twelvet.cn
 * @Description: 根据路径扫描端点
 */
public class EndpointClassPathScanner extends ClassPathBeanDefinitionScanner {

    public EndpointClassPathScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
        super(registry, useDefaultFilters);
    }

    /**
     * add scan endpoint
     *
     * @param basePackages package
     * @return Set<BeanDefinitionHolder>
     */
    @Override
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        // add scan
        addIncludeFilter(new AnnotationTypeFilter(WebSocketEndpoint.class));
        return super.doScan(basePackages);
    }
}
