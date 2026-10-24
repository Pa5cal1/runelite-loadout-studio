/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Versioned text transport for Loadout Studio only, never an external-plugin code. */
final class SetupExportText {
    static final String PREFIX="loadoutstudio:gz1:";
    private SetupExportText() { }
    static String compress(String json) {
        byte[] data=json.getBytes(StandardCharsets.UTF_8);
        if(data.length>SetupCodec.MAX_TEXT)throw new IllegalArgumentException("Export exceeds the supported size.");
        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(GZIPOutputStream gzip=new GZIPOutputStream(out)){gzip.write(data);}
            String result=PREFIX+Base64.getEncoder().encodeToString(out.toByteArray());
            if(result.length()>SetupCodec.MAX_TEXT)throw new IllegalArgumentException("Compressed export exceeds the supported size.");
            return result;
        } catch(IOException ex){throw new IllegalArgumentException("Could not compress this setup.",ex);}
    }
    static String unpack(String text) {
        if(text==null||text.length()>SetupCodec.MAX_TEXT)throw new IllegalArgumentException("Import must be smaller than 2 MB.");
        String value=text.trim();
        if(!value.startsWith("loadoutstudio:"))return value;
        if(!value.startsWith(PREFIX))throw new IllegalArgumentException("Unsupported Loadout Studio compressed format.");
        try {
            byte[] bytes=Base64.getDecoder().decode(value.substring(PREFIX.length()));
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(GZIPInputStream gzip=new GZIPInputStream(new ByteArrayInputStream(bytes))){
                byte[] buffer=new byte[4096];int count;
                while((count=gzip.read(buffer))!=-1){if(out.size()>SetupCodec.MAX_TEXT-count)throw new IllegalArgumentException("Expanded setup exceeds the 2 MB import limit.");out.write(buffer,0,count);}
            }
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(out.toByteArray())).toString();
        } catch(CharacterCodingException ex){throw new IllegalArgumentException("Compressed setup must contain UTF-8 text.",ex);}
        catch(IOException|IllegalArgumentException ex){if(ex instanceof IllegalArgumentException&&ex.getMessage()!=null&&ex.getMessage().startsWith("Expanded setup"))throw (IllegalArgumentException)ex;throw new IllegalArgumentException("Invalid or damaged compressed Loadout Studio setup.",ex);}
    }
}
