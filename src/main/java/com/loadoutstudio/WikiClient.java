/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;

/** Public Wiki text only; no player state is transmitted. */
public final class WikiClient
{
    private final com.google.gson.Gson gson;
    private final OkHttpClient http;
    private final Set<Call> pending = ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> { Thread t=new Thread(r,"loadout-wiki-on-demand"); t.setDaemon(true); return t; });
    private final java.util.Map<String,CacheEntry> cache = new java.util.LinkedHashMap<>(16,0.75f,true);
    private final java.util.Map<String,Job> jobs = new java.util.HashMap<>();
    private final java.util.ArrayDeque<Job> queue = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Long> starts = new java.util.ArrayDeque<>();
    private java.util.concurrent.ScheduledFuture<?> scheduled;
    private final long interval;
    private long nextRequest, cooldown;
    private boolean busy;
    private volatile boolean closed;
    private static final String USER_AGENT="LoadoutStudio/0.5.0 (https://github.com/Pa5cal1/runelite-loadout-studio; RuneLite)";
    private static final class CacheEntry { final String text; final long until; CacheEntry(String text,long until){this.text=text;this.until=until;} }
    private static final class Job
    {
        final String url; final int limit; final long ttl; final java.util.List<BiConsumer<String,String>> callbacks=new java.util.ArrayList<>();
        Job(String url,int limit,long ttl){this.url=url;this.limit=limit;this.ttl=ttl;}
    }
    public WikiClient(OkHttpClient injected,com.google.gson.Gson gson) { this(injected,gson,2000); }
    WikiClient(OkHttpClient injected,com.google.gson.Gson gson,long interval)
    {
        this.gson=gson; this.interval=interval;
        http=injected.newBuilder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).callTimeout(25,TimeUnit.SECONDS).build();
    }
    public void search(String query,BiConsumer<java.util.List<String>,String> callback)
    {
        String term=query.trim(); if(term.length()<2 || term.length()>120) throw new IllegalArgumentException("Enter 2–120 characters to search the Wiki.");
        String url="https://oldschool.runescape.wiki/api.php?action=opensearch&namespace=0&limit=10&format=json&maxlag=5&maxage=3600&smaxage=3600&search="+URLEncoder.encode(term,StandardCharsets.UTF_8);
        requestJson(url,100000,6*60*60*1000L,(text,error)->{ if(error!=null){callback.accept(null,error);return;} try{callback.accept(searchTitles(text,gson),null);}catch(RuntimeException e){callback.accept(null,e.getMessage());} });
    }
    private void requestJson(String url,int limit,long ttl,BiConsumer<String,String> callback)
    {
        String hit=null,error=null;
        synchronized(this)
        {
            if(closed)return; long now=System.currentTimeMillis(); CacheEntry saved=cache.get(url);
            if(saved!=null && now<saved.until) hit=saved.text;
            else if(now<cooldown) error="Wiki requests are paused after a busy response. Use saved setups or pasted wikitext, or try again later.";
            else { Job existing=jobs.get(url); if(existing!=null){existing.callbacks.add(callback);return;}
                if(jobs.size()>=12) error="Too many pending Wiki requests. Wait for the current search.";
                else {Job job=new Job(url,limit,ttl);job.callbacks.add(callback);jobs.put(url,job);queue.add(job);schedule();return;}
            }
        }
        callback.accept(hit,error);
    }
    private synchronized void schedule()
    {
        if(closed || busy || queue.isEmpty() || scheduled!=null && !scheduled.isDone())return;
        long now=System.currentTimeMillis(); while(!starts.isEmpty() && now-starts.peekFirst()>=60000)starts.removeFirst();
        long due=Math.max(now,nextRequest); if(starts.size()>=10)due=Math.max(due,starts.peekFirst()+60000);
        scheduled=scheduler.schedule(()->{ synchronized(WikiClient.this){scheduled=null;if(closed || busy || queue.isEmpty())return;busy=true;starts.addLast(System.currentTimeMillis());nextRequest=System.currentTimeMillis()+interval;} send(); },Math.max(0,due-now),TimeUnit.MILLISECONDS);
    }
    private void send()
    {
        final Job job; synchronized(this){if(closed || queue.isEmpty()){busy=false;return;}job=queue.removeFirst();}
        Call call=http.newCall(new Request.Builder().url(job.url).header("User-Agent",USER_AGENT).header("Cache-Control","max-age=3600").build()); pending.add(call);
        call.enqueue(new Callback()
        {
            @Override public void onFailure(Call c,IOException e){pending.remove(c);finish(job,null,"Wiki request failed. No automatic retry was made; use cached setups or pasted wikitext.",false);}
            @Override public void onResponse(Call c,Response response)
            {
                String text=null,error=null; boolean paused=false;
                try(Response r=response)
                {
                    if(r.code()==429 || r.code()==503 || r.header("Retry-After")!=null){ paused=true;pause(r.header("Retry-After"));throw new IOException("Wiki is busy. Requests paused; no automatic retry will be made."); }
                    if(!r.isSuccessful() || r.body()==null)throw new IOException("Wiki API request failed (HTTP "+r.code()+").");
                    if(r.body().contentLength()>job.limit)throw new IOException("Wiki response is too large.");
                    Buffer buffer=new Buffer();while(r.body().source().read(buffer,8192)!=-1)if(buffer.size()>job.limit)throw new IOException("Wiki response is too large.");text=buffer.readUtf8();
                    com.google.gson.JsonElement parsed=new SetupCodec(gson).parse(text);
                    if(parsed.isJsonObject() && parsed.getAsJsonObject().has("error")){paused=true;pause(r.header("Retry-After"));text=null;throw new IOException("Wiki API is busy or rejected this request. Requests paused; try later.");}
                }
                catch(IOException|RuntimeException ex){error=ex.getMessage();text=null;}
                finally{pending.remove(c);}
                finish(job,text,error,paused);
            }
        });
    }
    private synchronized void pause(String retry)
    {
        long delay=60000; if(retry!=null)try{delay=Math.max(delay,Long.parseLong(retry.trim())*1000);}catch(NumberFormatException ex){try{delay=Math.max(delay,java.time.ZonedDateTime.parse(retry,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-System.currentTimeMillis());}catch(RuntimeException ignored){}}
        cooldown=System.currentTimeMillis()+Math.min(86400000,delay);
    }
    private void finish(Job job,String text,String error,boolean paused)
    {
        java.util.List<BiConsumer<String,String>> callbacks;
        synchronized(this)
        {
            busy=false;if(closed)return;jobs.remove(job.url);callbacks=new java.util.ArrayList<>(job.callbacks);
            if(text!=null){cache.put(job.url,new CacheEntry(text,System.currentTimeMillis()+job.ttl));int bytes=0;for(CacheEntry entry:cache.values())bytes+=entry.text.length();java.util.Iterator<CacheEntry> it=cache.values().iterator();while((bytes>4000000 || cache.size()>64)&&it.hasNext()){bytes-=it.next().text.length();it.remove();}}
            if(paused){for(Job waiting:queue)callbacks.addAll(waiting.callbacks);queue.clear();jobs.clear();}
            schedule();
        }
        for(BiConsumer<String,String> callback:callbacks) if(!closed) callback.accept(text,error);
    }
	static java.util.List<String> searchTitles(String text, com.google.gson.Gson gson)
	{
		com.google.gson.JsonElement parsed = new SetupCodec(gson).parse(text);
		if (parsed == null || !parsed.isJsonArray() || parsed.getAsJsonArray().size() != 4 || !parsed.getAsJsonArray().get(1).isJsonArray()) throw new IllegalArgumentException("Unexpected Wiki search response.");
		java.util.List<String> result = new java.util.ArrayList<>();
		for (com.google.gson.JsonElement item : parsed.getAsJsonArray().get(1).getAsJsonArray())
		{
			if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Invalid Wiki search title.");
			String title = item.getAsString(); if (!title.isEmpty() && title.length() <= 250 && result.size() < 10) result.add(title);
		}
		return result;
	}
	public static String articleUrl(String title) { return "https://oldschool.runescape.wiki/w/" + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8).replace("%2F", "/"); }
	public static String title(String url)
	{
		URI uri = URI.create(url.trim());
		if (!"https".equals(uri.getScheme()) || !"oldschool.runescape.wiki".equals(uri.getHost()) || uri.getPort() != -1 || uri.getUserInfo() != null || !uri.getRawPath().startsWith("/w/")) throw new IllegalArgumentException("Enter an HTTPS article URL on oldschool.runescape.wiki/w/.");
		String title = URLDecoder.decode(uri.getRawPath().substring(3).replace("+", "%2B"), StandardCharsets.UTF_8).replace('_', ' ');
		if (title.trim().isEmpty() || title.length() > 250) throw new IllegalArgumentException("Invalid Wiki page title."); return title;
	}
	public static final class Page
	{
		public final String title, text;
		public Page(String title, String text) { this.title = title; this.text = text; }
		public String url() { return articleUrl(title); }
	}
	public void fetch(String url, BiConsumer<String, String> callback)
	{
		fetchPage(url, false, (page, error) -> callback.accept(page == null ? null : page.text, error));
	}
	public void fetchPage(String url, boolean preferStrategies, BiConsumer<Page, String> callback)
	{
		String requested = title(url);
		requestPage(requested, (page, error) ->
		{
			if (error != null) { callback.accept(null, error); return; }
			if (page == null && requested.toLowerCase(java.util.Locale.ROOT).endsWith("/strategies"))
			{
				String base = requested.substring(0, requested.length() - "/Strategies".length());
				requestPage(base, (fallback, failure) ->
				{
					if (failure != null) callback.accept(null, failure);
					else if (fallback == null) callback.accept(null, "Wiki page not found: " + requested);
					else preferredPage(fallback, preferStrategies, callback);
				});
			}
			else if (page == null) callback.accept(null, "Wiki page not found: " + requested);
			else preferredPage(page, preferStrategies, callback);
		});
	}
	private void preferredPage(Page page, boolean preferStrategies, BiConsumer<Page, String> callback)
	{
		if (closed) return;
		if (!preferStrategies || page.title.toLowerCase(java.util.Locale.ROOT).endsWith("/strategies")) { callback.accept(page, null); return; }
		requestPage(page.title + "/Strategies", (strategy, error) -> callback.accept(error == null ? strategy == null ? page : strategy : null, error));
	}
	static Page pageResponse(String text, com.google.gson.Gson gson)
	{
		com.google.gson.JsonElement parsed = new SetupCodec(gson).parse(text);
		if (parsed == null || !parsed.isJsonObject()) throw new IllegalArgumentException("Unexpected Wiki page response.");
		com.google.gson.JsonObject root = parsed.getAsJsonObject();
		if (root.has("error")) throw new IllegalArgumentException("The Wiki API could not resolve this page.");
		if (!root.has("query") || !root.get("query").isJsonObject()) throw new IllegalArgumentException("Missing Wiki page information.");
		com.google.gson.JsonObject query = root.getAsJsonObject("query");
		if (query.has("interwiki") || !query.has("pages") || !query.get("pages").isJsonArray() || query.getAsJsonArray("pages").size() != 1) throw new IllegalArgumentException("Choose a single article on OSRS Wiki.");
		com.google.gson.JsonObject page = query.getAsJsonArray("pages").get(0).getAsJsonObject();
		if (page.has("invalid")) throw new IllegalArgumentException("Invalid Wiki page title.");
		if (page.has("missing") && page.get("missing").getAsBoolean()) return null;
		if (page.has("ns") && page.get("ns").getAsInt() != 0) throw new IllegalArgumentException("Choose an OSRS Wiki article, rather than a special page or template.");
		String title = page.get("title").getAsString();
		if (title.trim().isEmpty() || title.length() > 250 || title.indexOf('|') >= 0) throw new IllegalArgumentException("Invalid canonical Wiki title.");
		if (page.has("redirect") && page.get("redirect").getAsBoolean()) throw new IllegalArgumentException("The Wiki redirect could not be resolved.");
		com.google.gson.JsonArray revisions = page.getAsJsonArray("revisions");
		if (revisions == null || revisions.size() != 1) throw new IllegalArgumentException("Wiki page source is unavailable.");
		String source = revisions.get(0).getAsJsonObject().getAsJsonObject("slots").getAsJsonObject("main").get("content").getAsString();
		if (source.length() > 1_500_000) throw new IllegalArgumentException("Wiki page source is too large.");
		return new Page(title, source);
	}
	private void requestPage(String title, BiConsumer<Page, String> callback)
	{
		if (closed) return;
		if (title.trim().isEmpty() || title.length() > 250 || title.indexOf('|') >= 0) { callback.accept(null, "Enter a single valid Wiki page title."); return; }
		String url = "https://oldschool.runescape.wiki/api.php?action=query&redirects=1&prop=revisions&rvprop=content&rvslots=main&format=json&formatversion=2&titles=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
        url += "&maxlag=5&maxage=3600&smaxage=3600";
        requestJson(url,1_500_000,7*24*60*60*1000L,(text,error)->{if(error!=null){callback.accept(null,error);return;}try{callback.accept(pageResponse(text,gson),null);}catch(RuntimeException ex){callback.accept(null,ex.getMessage());}});
    }

	public synchronized void close() { closed=true;if(scheduled!=null)scheduled.cancel(false);scheduler.shutdownNow();for(Call c:pending)c.cancel();pending.clear();queue.clear();jobs.clear();cache.clear(); }
}
