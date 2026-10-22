/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.nio.file.*;
import net.runelite.client.util.Filepath;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PersistentWikiTest
{
    static final String URL="https://oldschool.runescape.wiki/w/Vorkath";
    static final String BODY="{\"query\":{\"pages\":[{\"title\":\"Vorkath\",\"ns\":0,\"revisions\":[{\"slots\":{\"main\":{\"content\":\"{{Inventory|1=Anglerfish}}\"}}}]}]}}";
    static class Store implements WikiClient.Store {String data="";int reads,writes;public String load(){reads++;return data;}public void save(String data){writes++;this.data=data;}}
    static WikiClient.Page page(WikiClient client,boolean refresh) throws Exception
    {
        CompletableFuture<WikiClient.Page> future=new CompletableFuture<>();client.fetchPage(URL,false,refresh,(v,e)->{if(e!=null)future.completeExceptionally(new RuntimeException(e));else future.complete(v);});return future.get(4,TimeUnit.SECONDS);
    }
    @Test public void restartUsesDiskCacheWithoutHttpAndExplicitRefreshBypassesIt() throws Exception
    {
        AtomicInteger requests=new AtomicInteger();Store store=new Store();OkHttpClient http=new OkHttpClient.Builder().addInterceptor(c->{requests.incrementAndGet();return new Response.Builder().request(c.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(ResponseBody.create(MediaType.parse("application/json"),BODY)).build();}).build();
        WikiClient first=new WikiClient(http,RuneLiteAPI.GSON,0,store);
        try{assertEquals(0,requests.get());assertEquals(0,store.reads);assertFalse(page(first,false).cached);assertEquals(1,store.writes);}finally{first.close();}
        WikiClient second=new WikiClient(http,RuneLiteAPI.GSON,0,store);
        try{assertEquals(1,store.reads);WikiClient.Page cached=page(second,false);assertTrue(cached.cached);assertTrue(cached.fetchedAt>0);assertFalse(cached.stale);assertEquals(1,requests.get());assertFalse(page(second,true).cached);assertEquals(2,requests.get());}
        finally{second.close();http.dispatcher().executorService().shutdownNow();http.connectionPool().evictAll();}
    }
    @Test public void expiredSourceFallsBackOnBusyResponseAndForcedRefreshReportsFailure() throws Exception
    {
        long fetched=System.currentTimeMillis()-8*24*60*60*1000L;JsonObject entry=new JsonObject();entry.addProperty("url","https://oldschool.runescape.wiki/api.php?action=query&redirects=1&prop=revisions&rvprop=content&rvslots=main&format=json&formatversion=2&titles=Vorkath&maxlag=5&maxage=3600&smaxage=3600");entry.addProperty("text",BODY);entry.addProperty("fetched",fetched);entry.addProperty("until",fetched+7*24*60*60*1000L);Store store=new Store();store.data="LOADOUT-WIKI-CACHE-1\n"+RuneLiteAPI.GSON.toJson(entry)+"\n";
        AtomicInteger requests=new AtomicInteger();OkHttpClient http=new OkHttpClient.Builder().addInterceptor(c->{requests.incrementAndGet();return new Response.Builder().request(c.request()).protocol(Protocol.HTTP_1_1).code(503).message("Busy").header("Retry-After","120").body(ResponseBody.create(MediaType.parse("text/plain"),"busy")).build();}).build();WikiClient client=new WikiClient(http,RuneLiteAPI.GSON,0,store);
        try{WikiClient.Page cached=page(client,false);assertTrue(cached.cached);assertTrue(cached.stale);assertEquals(0,requests.get());try{page(client,true);fail();}catch(ExecutionException expected){assertTrue(expected.getCause().getMessage().contains("paused"));}assertEquals(1,requests.get());assertEquals(0,store.writes);assertTrue(page(client,false).stale);}
        finally{client.close();http.dispatcher().executorService().shutdownNow();http.connectionPool().evictAll();}
    }
    @Test public void cacheUsesPluginFilepathAndReplacesAtomically() throws Exception
    {
        Path dir=Files.createTempDirectory("studio-wiki-cache");WikiCacheFile store=new WikiCacheFile(Filepath.Unchecked.getRooted(dir));assertEquals("",store.load());store.save("first");assertEquals("first",store.load());store.save("second");assertEquals("second",store.load());assertFalse(Files.exists(dir.resolve("wiki-cache-v1.tmp")));Files.delete(dir.resolve("wiki-cache-v1.jsonl"));Files.delete(dir);
    }
    @Test public void malformedCacheNeverBlocksAnExplicitSearch() throws Exception
    {
        Store store=new Store();store.data="bad cache";AtomicInteger requests=new AtomicInteger();OkHttpClient http=new OkHttpClient.Builder().addInterceptor(c->{requests.incrementAndGet();return new Response.Builder().request(c.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(ResponseBody.create(MediaType.parse("application/json"),"[\"Vorkath\",[\"Vorkath\"],[],[]]")).build();}).build();WikiClient client=new WikiClient(http,RuneLiteAPI.GSON,0,store);
        try{CompletableFuture<String> future=new CompletableFuture<>();client.search("Vorkath",(v,e)->future.complete(e==null?v.get(0):e));assertEquals("Vorkath",future.get(3,TimeUnit.SECONDS));assertEquals(1,requests.get());assertTrue(store.data.startsWith("LOADOUT-WIKI-CACHE-1"));}
        finally{client.close();http.dispatcher().executorService().shutdownNow();http.connectionPool().evictAll();}
    }
    @Test public void localSuggestionsReadSavedSourceWithoutHttp() throws Exception
    {
        Store store=new Store();AtomicInteger calls=new AtomicInteger();OkHttpClient http=new OkHttpClient.Builder().addInterceptor(c->{calls.incrementAndGet();return new Response.Builder().request(c.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(ResponseBody.create(MediaType.parse("application/json"),BODY)).build();}).build();WikiClient first=new WikiClient(http,RuneLiteAPI.GSON,0,store);page(first,false);first.close();WikiClient second=new WikiClient(http,RuneLiteAPI.GSON,0,store);
        try{CompletableFuture<java.util.List<String>> result=new CompletableFuture<>();second.suggestLocal("vork",result::complete);assertEquals(java.util.Collections.singletonList("Vorkath"),result.get(2,TimeUnit.SECONDS));assertEquals(1,calls.get());}finally{second.close();http.dispatcher().executorService().shutdownNow();http.connectionPool().evictAll();}
    }

}
