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
	private volatile boolean closed;
	public WikiClient(OkHttpClient injected, com.google.gson.Gson gson)
	{
		this.gson = gson;
		http = injected.newBuilder().followRedirects(false).followSslRedirects(false).callTimeout(25, TimeUnit.SECONDS).build();
	}
	public void search(String query, BiConsumer<java.util.List<String>, String> callback)
	{
		String term = query.trim();
		if (term.length() < 2 || term.length() > 120) throw new IllegalArgumentException("Enter 2–120 characters to search the Wiki.");
		if (closed) return;
		String url = "https://oldschool.runescape.wiki/api.php?action=opensearch&namespace=0&limit=10&format=json&search=" + URLEncoder.encode(term, StandardCharsets.UTF_8);
		Call call = http.newCall(new Request.Builder().url(url).header("User-Agent", "LoadoutStudio/0.1 (RuneLite local test plugin)").build()); pending.add(call);
		call.enqueue(new Callback()
		{
			@Override public void onFailure(Call c, IOException e) { pending.remove(c); if (!closed) callback.accept(null, "Wiki search failed. Try an article URL or pasted wikitext."); }
			@Override public void onResponse(Call c, Response response)
			{
				pending.remove(c);
				try (Response r = response)
				{
					if (!r.isSuccessful() || r.body() == null) throw new IOException("Wiki search HTTP " + r.code());
					Buffer buffer = new Buffer(); while (r.body().source().read(buffer, 8192) != -1) if (buffer.size() > 100000) throw new IOException("Wiki search response is too large.");
					java.util.List<String> titles = searchTitles(buffer.readUtf8(), gson); if (!closed) callback.accept(titles, null);
				}
				catch (IOException | RuntimeException e) { if (!closed) callback.accept(null, e.getMessage()); }
			}
		});
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
	public void fetch(String url, BiConsumer<String, String> callback)
	{
		String title = title(url); request(title, 0, callback);
	}
	private void request(String title, int depth, BiConsumer<String, String> callback)
	{
		if (closed) return;
		String url = "https://oldschool.runescape.wiki/w/" + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8).replace("%2F", "/") + "?action=raw";
		Request request = new Request.Builder().url(url).header("User-Agent", "LoadoutStudio/0.1 (RuneLite local test plugin)").build();
		Call call = http.newCall(request); pending.add(call);
		call.enqueue(new Callback()
		{
			@Override public void onFailure(Call c, IOException e) { pending.remove(c); if (!closed) callback.accept(null, "Wiki request failed. Paste wikitext or retry. " + e.getClass().getSimpleName()); }
			@Override public void onResponse(Call c, Response response)
			{
				pending.remove(c);
				try (Response r = response)
				{
					if (!r.isSuccessful() || r.body() == null) throw new IOException("Wiki HTTP " + r.code() + ". Redirects are not followed.");
					if (r.body().contentLength() > 1_500_000) throw new IOException("Wiki response is too large.");
					Buffer buffer = new Buffer(); long read;
					while ((read = r.body().source().read(buffer, 8192)) != -1) if (buffer.size() > 1_500_000) throw new IOException("Wiki response is too large.");
					String text = buffer.readUtf8(); java.util.regex.Matcher redirect = java.util.regex.Pattern.compile("(?is)^\\s*#redirect\\s*\\[\\[([^]#]+)").matcher(text);
					if (redirect.find()) { if (depth >= 3) throw new IOException("Too many Wiki redirects."); request(redirect.group(1), depth + 1, callback); }
					else if (!closed) callback.accept(text, null);
				}
				catch (IOException | RuntimeException e) { if (!closed) callback.accept(null, e.getMessage()); }
			}
		});
	}
	public void close() { closed = true; for (Call c : pending) c.cancel(); pending.clear(); }
}
