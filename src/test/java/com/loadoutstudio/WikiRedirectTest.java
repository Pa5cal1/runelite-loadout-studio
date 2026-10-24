/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.*;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WikiRedirectTest
{
	private static String page(String title, String text)
	{
		JsonObject main = new JsonObject(); main.addProperty("content", text); JsonObject slots = new JsonObject(); slots.add("main", main); JsonObject revision = new JsonObject(); revision.add("slots", slots); JsonArray revisions = new JsonArray(); revisions.add(revision);
		JsonObject page = new JsonObject(); page.addProperty("title", title); page.addProperty("ns", 0); page.add("revisions", revisions); JsonArray pages = new JsonArray(); pages.add(page); JsonObject query = new JsonObject(); query.add("pages", pages); JsonObject root = new JsonObject(); root.add("query", query); return root.toString();
	}
	private static final String MISSING = "{\"query\":{\"pages\":[{\"ns\":0,\"title\":\"Missing\",\"missing\":true}]}}";
	private static final class Fixture implements AutoCloseable
	{
		final Map<String, String> responses = new HashMap<>();
		final List<String> titles = Collections.synchronizedList(new ArrayList<>());
		final OkHttpClient http;
		final WikiClient wiki;
		Fixture()
		{
			http = new OkHttpClient.Builder().addInterceptor(chain ->
			{
				Request request = chain.request(); assertEquals("oldschool.runescape.wiki", request.url().host()); assertEquals("1", request.url().queryParameter("redirects"));
				String title = request.url().queryParameter("titles"); titles.add(title);
				Response.Builder response = new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("Fixture").body(ResponseBody.create(MediaType.parse("application/json"), responses.getOrDefault(title, MISSING)));
				return response.build();
			}).build();
			wiki = new WikiClient(http, RuneLiteAPI.GSON, 0);
		}
		WikiClient.Page fetch(String title, boolean strategies) throws Exception
		{
			CompletableFuture<WikiClient.Page> result = new CompletableFuture<>();
			wiki.fetchPage(WikiClient.articleUrl(title), strategies, (page, error) -> { if (error != null) result.completeExceptionally(new IllegalArgumentException(error)); else result.complete(page); });
			return result.get(4, TimeUnit.SECONDS);
		}
		public void close() { wiki.close(); http.dispatcher().executorService().shutdownNow(); http.connectionPool().evictAll(); }
	}
	@Test public void strategiesAliasResolvesWithoutAppendingStrategiesAgain() throws Exception
	{
		try (Fixture f = new Fixture())
		{
			f.responses.put("Fractured archive strategies", page("The Fractured Archive/Strategies", "{{Inventory|1=Shark}}"));
			WikiClient.Page p = f.fetch("Fractured archive strategies", true);
			assertEquals("The Fractured Archive/Strategies", p.title); assertEquals(1, f.titles.size());
			assertEquals("The Fractured Archive (Max gear)", SetupCodec.name(new WikiParser().parse(p.text, p.title, p.url()).get(0)));
		}
	}
	@Test public void baseAliasUsesCanonicalNameBeforeFindingStrategies() throws Exception
	{
		try (Fixture f = new Fixture())
		{
			f.responses.put("Fractured archive", page("The Fractured Archive", "Article")); f.responses.put("The Fractured Archive/Strategies", page("The Fractured Archive/Strategies", "Strategies"));
			assertEquals("Strategies", f.fetch("Fractured archive", true).text);
			assertEquals(Arrays.asList("Fractured archive", "The Fractured Archive/Strategies"), f.titles);
		}
	}
	@Test public void missingStrategiesFallsBackToExistingArticle() throws Exception
	{
		try (Fixture f = new Fixture())
		{
			f.responses.put("Example", page("Example", "{{Inventory|1=Shark}}"));
			assertEquals("Example", f.fetch("Example", true).title); assertEquals(Arrays.asList("Example", "Example/Strategies"), f.titles);
		}
	}
	@Test public void disabledPreferenceDoesNotRequestSubpage() throws Exception
	{
		try (Fixture f = new Fixture()) { f.responses.put("Fractured archive", page("The Fractured Archive", "Article")); assertEquals("The Fractured Archive", f.fetch("Fractured archive", false).title); assertEquals(1, f.titles.size()); }
	}
	@Test public void previouslyBrokenAliasUrlCanBeRecovered() throws Exception
	{
		try (Fixture f = new Fixture())
		{
			f.responses.put("Fractured archive strategies", page("The Fractured Archive/Strategies", "Strategies"));
			assertEquals("The Fractured Archive/Strategies", f.fetch("Fractured archive strategies/Strategies", true).title); assertEquals(2, f.titles.size());
		}
	}
	@Test public void httpRedirectIsNotFollowedAndErrorIsAccurate() throws Exception
	{
		java.util.concurrent.atomic.AtomicInteger requests = new java.util.concurrent.atomic.AtomicInteger();
		com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> { requests.incrementAndGet(); exchange.getResponseHeaders().add("Location", "/redirect-target"); exchange.sendResponseHeaders(302, -1); exchange.close(); }); server.start();
		String localUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/query";
		OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> chain.proceed(chain.request().newBuilder().url(localUrl).build())).build(); WikiClient wiki = new WikiClient(http, RuneLiteAPI.GSON, 0);
		try
		{
			CompletableFuture<String> result = new CompletableFuture<>(); wiki.fetchPage(WikiClient.articleUrl("Example"), true, (page, error) -> result.complete(error));
			assertTrue(result.get(4, TimeUnit.SECONDS).contains("HTTP 302")); assertEquals(1, requests.get());
		}
		finally { wiki.close(); http.dispatcher().executorService().shutdownNow(); http.connectionPool().evictAll(); server.stop(0); }
	}
	@Test public void missingPageHasUsefulMessage() throws Exception
	{
		try (Fixture f = new Fixture())
		{
			try { f.fetch("Missing", true); fail("Missing page must be reported"); }
			catch (ExecutionException e) { assertEquals("Wiki page not found: Missing", e.getCause().getMessage()); }
		}
	}
}
