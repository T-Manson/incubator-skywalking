package org.apache.skywalking.apm.plugin.dingding;

import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"sun.security.*", "javax.net.*"})
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class DingDingInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    private DingDingServletExceptionInterceptor interceptor;

    @Mock
    private EnhancedInstance enhancedInstance;

    private Object[] arguments;
    private Class[] argumentTypes;

    @Before
    public void setUp() throws Exception {
        interceptor = new DingDingServletExceptionInterceptor();

        Config.Plugin.Dingding.WEB_HOOK = "https://oapi.dingtalk.com/robot/send?access_token=04872c69f58a898149dea16f89bcaedc0c843d9d0483e509e451798ec5e272f0";

        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn(Config.Plugin.Dingding.WEB_HOOK);

        BigDecimal num = new BigDecimal(3.14);

        arguments = new Object[]{num};
        argumentTypes = new Class[]{num.getClass()};
    }

    @Test
    public void testIntercept() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, getExecuteMethod(), arguments, argumentTypes, null);
        interceptor.afterMethod(enhancedInstance, getExecuteMethod(), arguments, argumentTypes, null);
    }

    @Test
    public void testInterceptWithException() throws Throwable {
        interceptor.beforeMethod(enhancedInstance, getExecuteMethod(), arguments, argumentTypes, null);

        try {
            BigDecimal num = new BigDecimal(Double.valueOf(arguments[0].toString()));
            BigDecimal den = new BigDecimal(0);
            BigDecimal result = num.divide(den);
        } catch (ArithmeticException e) {
            interceptor.handleMethodException(enhancedInstance, getExecuteMethod(), arguments, argumentTypes, e);
        }

        interceptor.afterMethod(enhancedInstance, getExecuteMethod(), arguments, argumentTypes, null);
    }

    private Method getExecuteMethod() {
        try {
            return BigDecimal.class.getMethod("divide", BigDecimal.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }
}
