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
    private static final String USER_AGENT="LoadoutStudio/0.7.3 (https://github.com/Pa5cal1/runelite-loadout-studio; RuneLite)";
    interface Store
    {
        String load() throws IOException;
        void save(String data) throws IOException;
    }
    private final Store store;
    private boolean cacheLoaded;
    private volatile String cacheWarning = "";
    private static final class CacheEntry
    {
        final String text; final long fetched, until;
        CacheEntry(String text,long fetched,long until) { this.text=text;this.fetched=fetched;this.until=until; }
    }
    private static final class Reply
    {
        final String text; final long fetched; final boolean cached, stale;
        Reply(CacheEntry entry, boolean cached) { text=entry.text;fetched=entry.fetched;this.cached=cached;stale=System.currentTimeMillis()>entry.until; }
    }
    private static final class Job
    {
        final String url; final int limit; final long ttl; final boolean refresh;
        final java.util.List<BiConsumer<Reply,String>> callbacks=new java.util.ArrayList<>();
        Job(String url,int limit,long ttl,boolean refresh) { this.url=url;this.limit=limit;this.ttl=ttl;this.refresh=refresh; }
    }
    public WikiClient(OkHttpClient injected,com.google.gson.Gson gson) { this(injected,gson,2000,null); }
    WikiClient(OkHttpClient injected,com.google.gson.Gson gson,long interval) { this(injected,gson,interval,null); }
    WikiClient(OkHttpClient injected,com.google.gson.Gson gson,long interval,Store store)
    {
        this.gson=gson;this.interval=interval;this.store=store;
        http=injected.newBuilder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).callTimeout(25,TimeUnit.SECONDS).build();
    }
    public void search(String query,BiConsumer<java.util.List<String>,String> callback)
    {
        String term=query.trim();if(term.length()<2 || term.length()>120)throw new IllegalArgumentException("Enter 2–120 characters to search the Wiki.");
        String url="https://oldschool.runescape.wiki/api.php?action=opensearch&namespace=0&limit=10&format=json&maxlag=5&maxage=3600&smaxage=3600&search="+URLEncoder.encode(term,StandardCharsets.UTF_8);
        requestJson(url,100000,6*60*60*1000L,false,(reply,error)->{if(error!=null){callback.accept(null,error);return;}try{callback.accept(searchTitles(reply.text,gson),null);}catch(RuntimeException e){callback.accept(null,e.getMessage());}});
    }
    /** Suggestions from already cached responses only; never calls the HTTP client. */
    public void suggestLocal(String query,java.util.function.Consumer<java.util.List<String>> callback)
    {
        String term=query.trim().toLowerCase(java.util.Locale.ROOT);
        onWorker(()->
        {
            loadCache();java.util.Set<String> titles=new java.util.TreeSet<>();
            for(java.util.Map.Entry<String,CacheEntry> value:cache.entrySet())try
            {
                if(value.getKey().contains("action=opensearch"))titles.addAll(searchTitles(value.getValue().text,gson));
                else{Page page=pageResponse(value.getValue().text,gson);if(page!=null)titles.add(page.title);}
            }catch(RuntimeException ignored){ }
            callback.accept(titles.stream().filter(title->title.toLowerCase(java.util.Locale.ROOT).contains(term)).limit(10).collect(java.util.stream.Collectors.toList()));
        });
    }
    private void onWorker(Runnable action)
    {
        if(closed)return;
        try { scheduler.execute(()->{if(!closed)action.run();}); } catch(java.util.concurrent.RejectedExecutionException ignored) { }
    }
    private void loadCache()
    {
        if(cacheLoaded)return;cacheLoaded=true;if(store==null)return;
        try
        {
            String data=store.load();if(data.isEmpty())return;
            if(data.length()>8_000_000 || !data.startsWith("LOADOUT-WIKI-CACHE-1\n"))throw new IOException("Invalid Wiki cache.");
            int count=0;
            for(String line:data.substring(21).split("\n"))
            {
                if(line.isEmpty())continue;if(++count>64)throw new IOException("Too many cached pages.");
                try
                {
                    com.google.gson.JsonObject entry=new SetupCodec(gson).parse(line).getAsJsonObject();
                    String url=entry.get("url").getAsString(),text=entry.get("text").getAsString();
                    long fetched=entry.get("fetched").getAsLong(),until=entry.get("until").getAsLong(),now=System.currentTimeMillis();
                    if(!allowedUrl(url) || fetched<1 || fetched>now+60000 || until<fetched || until>fetched+7*24*60*60*1000L)continue;
                    validateResponse(url,text);cache.put(url,new CacheEntry(text,fetched,until));
                }
                catch(RuntimeException ignored) { cacheWarning="Some invalid cached Wiki entries were ignored."; }
            }
            trimCache();
        }
        catch(IOException|RuntimeException ex) { cacheWarning="Local Wiki cache could not be read; saved loadouts are unaffected.";cache.clear(); }
    }
    private static boolean allowedUrl(String url)
    {
        try { URI uri=URI.create(url);return "https".equals(uri.getScheme()) && "oldschool.runescape.wiki".equals(uri.getHost()) && uri.getPort()==-1 && uri.getUserInfo()==null && "/api.php".equals(uri.getPath()) && (url.contains("action=query&redirects=1&prop=revisions") || url.contains("action=opensearch&namespace=0")); }
        catch(RuntimeException ex){return false;}
    }
    private void validateResponse(String url,String text)
    {
        if(url.contains("action=opensearch")){if(text.length()>100000)throw new IllegalArgumentException();searchTitles(text,gson);}
        else pageResponse(text,gson);
    }
    private void trimCache()
    {
        int characters=0;for(CacheEntry entry:cache.values())characters+=entry.text.length();
        java.util.Iterator<CacheEntry> it=cache.values().iterator();while((characters>4_000_000 || cache.size()>64)&&it.hasNext()){characters-=it.next().text.length();it.remove();}
    }
    private void persistCache()
    {
        if(store==null)return;
        try
        {
            StringBuilder data=new StringBuilder("LOADOUT-WIKI-CACHE-1\n");
            for(java.util.Map.Entry<String,CacheEntry> value:cache.entrySet())
            {
                com.google.gson.JsonObject entry=new com.google.gson.JsonObject();entry.addProperty("url",value.getKey());entry.addProperty("text",value.getValue().text);entry.addProperty("fetched",value.getValue().fetched);entry.addProperty("until",value.getValue().until);
                String line=gson.toJson(entry).replace("\n", "").replace("\r", "");if(line.length()>SetupCodec.MAX_TEXT)continue;
                if(data.length()+line.length()>4_000_000)continue;data.append(line).append('\n');
            }
            store.save(data.toString());cacheWarning="";
        }
        catch(IOException|RuntimeException ex){cacheWarning="Wiki cache could not be saved to disk; this session still works.";}
    }
    public String cacheWarning() { return cacheWarning; }
    private void requestJson(String url,int limit,long ttl,boolean refresh,BiConsumer<Reply,String> callback)
    {
        onWorker(()->
        {
            loadCache();CacheEntry saved=cache.get(url);long now=System.currentTimeMillis();
            if(!refresh && saved!=null && (now<saved.until || url.contains("prop=revisions"))){callback.accept(new Reply(saved,true),null);return;}
            if(now<cooldown){if(!refresh && saved!=null)callback.accept(new Reply(saved,true),null);else callback.accept(null,"Wiki requests are paused. Use cached/saved setups or try later.");return;}
            Job existing=jobs.get(url);if(existing!=null){existing.callbacks.add(callback);return;}
            if(jobs.size()>=12){callback.accept(null,"Too many pending Wiki requests. Wait for the current search.");return;}
            Job job=new Job(url,limit,ttl,refresh);job.callbacks.add(callback);jobs.put(url,job);queue.add(job);schedule();
        });
    }
    private synchronized void schedule()
    {
        if(closed || busy || queue.isEmpty() || scheduled!=null && !scheduled.isDone())return;
        long now=System.currentTimeMillis();while(!starts.isEmpty() && now-starts.peekFirst()>=60000)starts.removeFirst();
        long due=Math.max(now,nextRequest);if(starts.size()>=10)due=Math.max(due,starts.peekFirst()+60000);
        scheduled=scheduler.schedule(()->{synchronized(WikiClient.this){scheduled=null;if(closed || busy || queue.isEmpty())return;busy=true;starts.addLast(System.currentTimeMillis());nextRequest=System.currentTimeMillis()+interval;}send();},Math.max(0,due-now),TimeUnit.MILLISECONDS);
    }
    private void send()
    {
        final Job job;synchronized(this){if(closed || queue.isEmpty()){busy=false;return;}job=queue.removeFirst();}
        Call call=http.newCall(new Request.Builder().url(job.url).header("User-Agent",USER_AGENT).header("Cache-Control",job.refresh?"no-cache":"max-age=3600").build());
        synchronized(this){if(closed)return;pending.add(call);call.enqueue(new Callback()
        {
            public void onFailure(Call c,IOException e){pending.remove(c);onWorker(()->finish(job,null,"Wiki request failed. No automatic retry was made.",false));}
            public void onResponse(Call c,Response response)
            {
                String text=null,error=null,retry=null;boolean paused=false;
                try(Response r=response)
                {
                    if(r.code()==429 || r.code()==503 || r.header("Retry-After")!=null){paused=true;retry=r.header("Retry-After");throw new IOException("Wiki is busy. Requests paused; no automatic retry will be made.");}
                    if(!r.isSuccessful() || r.body()==null)throw new IOException("Wiki API request failed (HTTP "+r.code()+").");
                    if(r.body().contentLength()>job.limit)throw new IOException("Wiki response is too large.");
                    Buffer buffer=new Buffer();while(r.body().source().read(buffer,8192)!=-1)if(buffer.size()>job.limit)throw new IOException("Wiki response is too large.");text=buffer.readUtf8();
                    com.google.gson.JsonElement parsed=new SetupCodec(gson).parse(text);
                    if(parsed.isJsonObject() && parsed.getAsJsonObject().has("error")){paused=true;retry=r.header("Retry-After");throw new IOException("Wiki API rejected this request. Requests paused; try later.");}
                    validateResponse(job.url,text);
                }
                catch(IOException|RuntimeException ex){error=ex.getMessage();text=null;}
                finally{pending.remove(c);}
                final String result=text,failure=error,after=retry;final boolean pause=paused;
                onWorker(()->{if(pause)pause(after);finish(job,result,failure,pause);});
            }
        });}
    }
    private void pause(String retry)
    {
        long delay=60000;
        if(retry!=null)try{long seconds=Long.parseLong(retry.trim());delay=Math.max(delay,seconds>86400?86400000:Math.max(0,seconds)*1000);}catch(NumberFormatException ex){try{delay=Math.max(delay,java.time.ZonedDateTime.parse(retry,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()-System.currentTimeMillis());}catch(RuntimeException ignored){}}
        cooldown=System.currentTimeMillis()+Math.min(86400000,delay);
    }
    private void finish(Job job,String text,String error,boolean paused)
    {
        busy=false;if(closed)return;jobs.remove(job.url);
        if(text!=null){long now=System.currentTimeMillis();cache.put(job.url,new CacheEntry(text,now,now+job.ttl));trimCache();persistCache();}
        deliver(job,text,error);
        if(paused){java.util.List<Job> waiting=new java.util.ArrayList<>(queue);queue.clear();jobs.clear();for(Job next:waiting)deliver(next,null,error);}
        schedule();
    }
    private void deliver(Job job,String text,String error)
    {
        CacheEntry entry=cache.get(job.url);Reply reply=entry!=null && (text!=null || !job.refresh)?new Reply(entry,text==null):null;
        for(BiConsumer<Reply,String> callback:job.callbacks)if(!closed)callback.accept(reply,reply==null?error:null);
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
        public final long fetchedAt; public final boolean cached, stale;
        public Page(String title,String text){this(title,text,0,false,false);}
        public Page(String title,String text,long fetchedAt,boolean cached,boolean stale){this.title=title;this.text=text;this.fetchedAt=fetchedAt;this.cached=cached;this.stale=stale;}
		public String url() { return articleUrl(title); }
	}
	public void fetch(String url, BiConsumer<String, String> callback)
	{
		fetchPage(url, false, (page, error) -> callback.accept(page == null ? null : page.text, error));
	}
	public void fetchPage(String url, boolean preferStrategies, BiConsumer<Page, String> callback)
	{
		fetchPage(url,preferStrategies,false,callback);
    }
    public void fetchPage(String url,boolean preferStrategies,boolean refresh,BiConsumer<Page,String> callback)
    {
        String requested = title(url);
		requestPage(requested, refresh, (page, error) ->
		{
			if (error != null) { callback.accept(null, error); return; }
			if (page == null && requested.toLowerCase(java.util.Locale.ROOT).endsWith("/strategies"))
			{
				String base = requested.substring(0, requested.length() - "/Strategies".length());
				requestPage(base, refresh, (fallback, failure) ->
				{
					if (failure != null) callback.accept(null, failure);
					else if (fallback == null) callback.accept(null, "Wiki page not found: " + requested);
					else preferredPage(fallback, preferStrategies, refresh, callback);
				});
			}
			else if (page == null) callback.accept(null, "Wiki page not found: " + requested);
			else preferredPage(page, preferStrategies, refresh, callback);
		});
	}
	private void preferredPage(Page page, boolean preferStrategies, boolean refresh, BiConsumer<Page, String> callback)
	{
		if (closed) return;
		if (!preferStrategies || page.title.toLowerCase(java.util.Locale.ROOT).endsWith("/strategies")) { callback.accept(page, null); return; }
		requestPage(page.title + "/Strategies", refresh, (strategy, error) -> callback.accept(error == null ? strategy == null ? page : strategy : null, error));
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
	private void requestPage(String title, boolean refresh, BiConsumer<Page, String> callback)
	{
		if (closed) return;
		if (title.trim().isEmpty() || title.length() > 250 || title.indexOf('|') >= 0) { callback.accept(null, "Enter a single valid Wiki page title."); return; }
		String url = "https://oldschool.runescape.wiki/api.php?action=query&redirects=1&prop=revisions&rvprop=content&rvslots=main&format=json&formatversion=2&titles=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
        url += "&maxlag=5&maxage=3600&smaxage=3600";
        requestJson(url,1_500_000,7*24*60*60*1000L,refresh,(reply,error)->{if(error!=null){callback.accept(null,error);return;}try{Page page=pageResponse(reply.text,gson);callback.accept(page==null?null:new Page(page.title,page.text,reply.fetched,reply.cached,reply.stale),null);}catch(RuntimeException ex){callback.accept(null,ex.getMessage());}});
    }

	public synchronized void close() { closed=true;if(scheduled!=null)scheduled.cancel(false);scheduler.shutdownNow();for(Call c:pending)c.cancel();pending.clear(); }
}
