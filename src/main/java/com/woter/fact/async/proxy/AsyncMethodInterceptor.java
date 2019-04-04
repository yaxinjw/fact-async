package com.woter.fact.async.proxy;

import com.woter.fact.async.bean.AsyncMethod;
import com.woter.fact.async.cache.AsyncProxyCache;
import com.woter.fact.async.core.AsyncExecutor;
import com.woter.fact.async.core.AsyncFutureCallable;
import com.woter.fact.async.core.AsyncFutureTask;
import com.woter.fact.async.exception.AsyncException;
import com.woter.fact.async.util.CommonUtil;
import com.woter.fact.async.util.ReflectionHelper;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 *
 *
 *
 * </p>
 *
 * @author woter
 * @date 2016-3-23 下午6:13:58
 */
public class AsyncMethodInterceptor implements MethodInterceptor {

	private Object targetObject;

	public AsyncMethodInterceptor(Object targetObject) {

		this.targetObject = targetObject;
	}

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {

		final String cacheKey = CommonUtil.buildkey(targetObject, method);

		final AsyncMethod asyncMethod = AsyncProxyCache.getAsyncMethod(cacheKey);

		if (asyncMethod == null || !ReflectionHelper.canProxyInvoke(method)) {
			return ReflectionHelper.invoke(targetObject, args, method);
		}
		if (AsyncExecutor.isDestroyed()) {
			return ReflectionHelper.invoke(asyncMethod.getObject(), args, method);
		}

		final Object[] finArgs = args;

		AsyncFutureTask<Object> future = AsyncExecutor.submit(new AsyncFutureCallable<Object>() {

			/**
			 * 这里是异步调用的实现方法
			 * @return
			 * @throws Exception
			 */
			@Override
			public Object call() throws Exception {
				try {
					return ReflectionHelper.invoke(asyncMethod.getObject(), finArgs, asyncMethod.getMethod());
				} catch (Throwable e) {
					throw new AsyncException(e);
				}
			}

			@Override
			public int maxAttemps() {
				return asyncMethod.getRetry().getMaxAttemps();
			}

			@Override
			public long timeout() {
				return asyncMethod.getTimeout();
			}

			@Override
			@SuppressWarnings("unchecked")
			public Class<? extends Throwable>[] exceptions() {
				return new Class[]{TimeoutException.class};
			}

			@Override
			public String cacheKey() {
				return cacheKey;
			}
		});

		// 如果没有返回值
		if (asyncMethod.isVoid()) {
			return null;
		}

		// 如果有返回值就返回结果
		return new AsyncResultProxy(future).buildProxy(method.getReturnType(), asyncMethod.getTimeout(), true);

	}
}
