/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.io.IOException;
import java.util.*;

/** Bounded session-only diagnostics. No free text, identifiers, files or network transport. */
final class StudioDiagnostics {
    static final String VERSION="0.9.1-rc.7";
    static final String ISSUE_URL="https://github.com/Pa5cal1/runelite-loadout-studio/issues/new?template=bug_report.yml";
    enum Operation { UI_ACTION,SAVE,IMPORT,EXPORT_PREVIEW,EXPORT_COPY,ITEM_EDIT,ITEM_SEARCH,WIKI_SEARCH,WIKI_PAGE,WIKI_HTTP,WIKI_CACHE_HIT,WIKI_CACHE_FALLBACK,WIKI_CACHE_IO,WIKI_COALESCED,BANK_FILTER,BANK_LAYOUT,ALTERNATIVES }
    enum Outcome { SUCCESS,VALIDATION,EXPECTED_STATE,IO_FAILURE,SERVICE_FAILURE,UNEXPECTED }
    private static final class Metric { final long[] outcomes=new long[Outcome.values().length];long totalMs,maxMs; }
    private final EnumMap<Operation,Metric> metrics=new EnumMap<>(Operation.class);
    private final ArrayDeque<String> recent=new ArrayDeque<>();
    private boolean enabled=true;
    synchronized void enabled(boolean value){if(enabled!=value){clear();enabled=value;}}
    synchronized boolean enabled(){return enabled;}
    synchronized void clear(){metrics.clear();recent.clear();}
    synchronized void completed(Operation op,Outcome outcome,long startedNanos){
        if(!enabled)return;long elapsed=startedNanos<=0?0:Math.max(0,(System.nanoTime()-startedNanos)/1_000_000L);elapsed=Math.min(elapsed,86_400_000L);
        Metric m=metrics.computeIfAbsent(op,key->new Metric());m.outcomes[outcome.ordinal()]++;m.totalMs+=elapsed;m.maxMs=Math.max(m.maxMs,elapsed);
        if(outcome!=Outcome.SUCCESS)addRecent(op+" / "+outcome);
    }
    synchronized void failure(Operation op,Throwable error,long startedNanos){
        if(!enabled)return;Outcome kind=error instanceof IllegalArgumentException||error instanceof com.google.gson.JsonParseException?Outcome.VALIDATION:error instanceof IllegalStateException?Outcome.EXPECTED_STATE:error instanceof IOException||error instanceof java.io.UncheckedIOException?Outcome.IO_FAILURE:Outcome.UNEXPECTED;
        completed(op,kind,startedNanos);
        if(kind==Outcome.UNEXPECTED){StringBuilder frames=new StringBuilder(op+" / UNEXPECTED");int count=0;for(StackTraceElement frame:error.getStackTrace())if(frame.getClassName().startsWith("com.loadoutstudio.")&&count++<5){String type=frame.getClassName().substring("com.loadoutstudio.".length()),method=frame.getMethodName();if(type.matches("[A-Za-z0-9_$]{1,100}")&&method.matches("[A-Za-z0-9_$<>]{1,100}"))frames.append(" | ").append(type).append('.').append(method).append(':').append(Math.max(0,frame.getLineNumber()));}addRecent(frames.toString());}
    }
    private void addRecent(String event){if(recent.size()>=24)recent.removeFirst();recent.addLast(event);}
    synchronized String report(String runeLiteVersion){
        String rl=runeLiteVersion!=null&&runeLiteVersion.matches("[0-9]+\\.[0-9]+(?:\\.[0-9]+)?(?:-SNAPSHOT)?")?runeLiteVersion:"unknown";
        String os=System.getProperty("os.name","").toLowerCase(Locale.ROOT);os=os.contains("windows")?"Windows":os.contains("mac")?"macOS":os.contains("linux")?"Linux":"Other";
        String java=System.getProperty("java.specification.version","");if(!java.matches("[0-9.]{1,12}"))java="unknown";
        StringBuilder out=new StringBuilder("Loadout Studio diagnostic report v1\nPlugin: "+VERSION+"\nRuneLite: "+rl+"\nOS family: "+os+"\nJava specification: "+java+"\nRecording: "+(enabled?"enabled":"disabled")+"\nScope: current plugin session; resettable, not persisted\nNo automatic upload. No setup contents, item IDs, player names, search text, URLs, raw exception messages or file paths.\n\nOperation counters (calls, not unique users):\n");
        if(metrics.isEmpty())out.append("No samples.\n");
        for(Map.Entry<Operation,Metric> entry:metrics.entrySet()){Metric m=entry.getValue();long total=0;for(long n:m.outcomes)total+=n;out.append(entry.getKey()).append(": calls=").append(total);for(Outcome outcome:Outcome.values())out.append(", ").append(outcome).append('=').append(m.outcomes[outcome.ordinal()]);out.append(", total_ms=").append(m.totalMs).append(", max_ms=").append(m.maxMs).append('\n');}
        out.append("\nRecent technical outcomes (at most 24; oldest first):\n");for(String event:recent)out.append(event).append('\n');
        return out.toString();
    }
}
