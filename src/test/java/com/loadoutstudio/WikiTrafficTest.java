/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class WikiTrafficTest
{
    @Test public void startupIsSilentAndRepeatedConcurrentSearchesShareOneRequest() throws Exception
    {
        AtomicInteger count=new AtomicInteger();CompletableFuture<Void> release=new CompletableFuture<>();
        OkHttpClient http=new OkHttpClient.Builder().addInterceptor(chain->{count.incrementAndGet();try{release.get(3,TimeUnit.SECONDS);}catch(Exception ex){throw new java.io.IOException(ex);}assertTrue(chain.request().header("User-Agent").contains("github.com/Pa5cal1"));return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(ResponseBody.create(MediaType.parse("application/json"),"[\"Vorkath\",[\"Vorkath\"],[],[]]")).build();}).build();
        WikiClient wiki=new WikiClient(http,RuneLiteAPI.GSON,0);try{assertEquals(0,count.get());CompletableFuture<List<String>> a=new CompletableFuture<>(),b=new CompletableFuture<>(),c=new CompletableFuture<>();wiki.search("Vorkath",(v,e)->{if(e!=null)a.completeExceptionally(new RuntimeException(e));else a.complete(v);});wiki.search("Vorkath",(v,e)->b.complete(v));release.complete(null);assertEquals("Vorkath",a.get(4,TimeUnit.SECONDS).get(0));b.get(4,TimeUnit.SECONDS);wiki.search("Vorkath",(v,e)->c.complete(v));c.get(1,TimeUnit.SECONDS);assertEquals(1,count.get());}finally{wiki.close();http.dispatcher().executorService().shutdownNow();http.connectionPool().evictAll();}
    }
    @Test public void busyResponseHonorsCooldownWithoutAutomaticRetry() throws Exception
    {
        AtomicInteger count=new AtomicInteger();OkHttpClient http=new OkHttpClient.Builder().addInterceptor(chain->{count.incrementAndGet();return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(429).message("Busy").header("Retry-After","120").body(ResponseBody.create(MediaType.parse("text/plain"),"busy")).build();}).build();WikiClient wiki=new WikiClient(http,RuneLiteAPI.GSON,0);
        try{CompletableFuture<String> first=new CompletableFuture<>(),second=new CompletableFuture<>();wiki.search("Vorkath",(v,e)->first.complete(e));assertNotNull(first.get(3,TimeUnit.SECONDS));wiki.search("Zulrah",(v,e)->second.complete(e));assertTrue(second.get(1,TimeUnit.SECONDS).contains("paused"));assertEquals(1,count.get());}finally{wiki.close();http.dispatcher().executorService().shutdownNow();http.connectionPool().evictAll();}
    }
    @Test public void productionSpacingAppliesAcrossDifferentExplicitSearches() throws Exception
    {
        List<Long> times=Collections.synchronizedList(new ArrayList<>());OkHttpClient http=new OkHttpClient.Builder().addInterceptor(chain->{times.add(System.nanoTime());return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(ResponseBody.create(MediaType.parse("application/json"),"[\"query\",[],[],[]]")).build();}).build();WikiClient wiki=new WikiClient(http,RuneLiteAPI.GSON);
        try{CompletableFuture<Void> a=new CompletableFuture<>(),b=new CompletableFuture<>();wiki.search("Vorkath",(v,e)->a.complete(null));wiki.search("Zulrah",(v,e)->b.complete(null));a.get(3,TimeUnit.SECONDS);b.get(5,TimeUnit.SECONDS);assertEquals(2,times.size());assertTrue(TimeUnit.NANOSECONDS.toMillis(times.get(1)-times.get(0))>=1800);}finally{wiki.close();http.dispatcher().executorService().shutdownNow();http.connectionPool().evictAll();}
    }
}
