package org.jboss.netty.util;

//这个公共接口表明，该类依赖了外部的资源，需要显示的释放或关闭
public interface ExternalResourceReleasable {

    /**
     * Releases the external resources that this object depends on.  You should
     * not call this method if the external resources (e.g. thread pool) are
     * in use by other objects.
     */
    void releaseExternalResources();
}
