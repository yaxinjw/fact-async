package com.woter.fact.async.proxy;


/**
 * <p>
 *
 *
 *
 * </p>
 *
 * @author woter
 * @date 2016-3-23 上午10:58:31
 */
public interface AsyncProxy {

    public Object buildProxy(Object target, boolean all);

    /**
     *
     * @param target
     * @param timeout
     * @param all 是否所有的方法都异步
     * @return
     */
    public Object buildProxy(Object target, long timeout, boolean all);

}
 