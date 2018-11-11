/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package org.apache.skywalking.apm.plugin.dingding;

import com.google.gson.Gson;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.util.ThrowableTransformer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.dingding.model.AtModel;
import org.apache.skywalking.apm.plugin.dingding.model.DingDingTextRequest;
import org.apache.skywalking.apm.plugin.dingding.model.TextModel;

import java.lang.reflect.Method;
import java.text.MessageFormat;

/**
 * @author T-Manson
 */
public class DingDingInterceptor implements InstanceMethodsAroundInterceptor {

    private static final String DINGDING_OP_PREFIX = "Dingding/";

    private static final String ERROR_MESSAGE = "应用: {0} , ClassName: {1} , Message: {2} , Stack: {3}";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        String remotePeer = (String) objInst.getSkyWalkingDynamicField();
        if (remotePeer == null || remotePeer.isEmpty()) {
            remotePeer = method.getName();
        }

        AbstractSpan span = ContextManager.createExitSpan(DINGDING_OP_PREFIX + getParamInfo(allArguments, argumentsTypes),
                new ContextCarrier(), remotePeer);
        span.setComponent("dingding-moniter");
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        AbstractSpan activeSpan = ContextManager.activeSpan();
        activeSpan.errorOccurred();
        activeSpan.log(t);
        String webHook = Config.Plugin.Dingding.WEB_HOOK;
        if (webHook != null && !webHook.isEmpty()) {
            pushMsgToDingding(webHook, t);
        }
    }

    private String getParamInfo(Object[] allArguments, Class<?>[] argumentsTypes) {
        StringBuilder paramInfo = new StringBuilder();

        for (int i = 0; i < allArguments.length; ++i) {
            paramInfo.append(argumentsTypes[i] + "@ : @" + allArguments[i] + "| : |");
        }

        return paramInfo.toString();
    }

    private void pushMsgToDingding(String webHook, Throwable t) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(webHook);

            DingDingTextRequest dingDingTextRequest = new DingDingTextRequest();
            dingDingTextRequest.setAt(new AtModel());
            TextModel text = new TextModel();
            text.setContent(MessageFormat.format(ERROR_MESSAGE,
                    Config.Agent.APPLICATION_CODE,
                    t.getClass().getName(),
                    t.getMessage(),
                    ThrowableTransformer.INSTANCE.convert2String(t, 4000)));
            dingDingTextRequest.setText(text);

            Gson gson = new Gson();
            httpPost.setHeader("Content-Type", "application/json;charset=utf-8");
            httpPost.setEntity(new StringEntity(gson.toJson(dingDingTextRequest)));
            httpClient.execute(httpPost);
        } catch (Exception e) {
            AbstractSpan activeSpan = ContextManager.activeSpan();
            activeSpan.errorOccurred();
            activeSpan.log(e);
        }
    }
}
