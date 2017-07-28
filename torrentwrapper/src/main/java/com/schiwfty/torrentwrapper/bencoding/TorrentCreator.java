package com.schiwfty.torrentwrapper.bencoding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

/**
 * Created by arran on 7/02/2017.
 */

public class TorrentCreator {
    private void encodeObject(Object o, OutputStream out) throws IOException {
        if (o instanceof String)
            encodeString((String) o, out);
        else if (o instanceof Map)
            encodeMap((Map) o, out);
        else if (o instanceof byte[])
            encodeBytes((byte[]) o, out);
        else if (o instanceof Number)
            encodeLong(((Number) o).longValue(), out);
        else if (o instanceof String[])
            encodeList((String[]) o, out);
        else
            throw new Error("Unencodable type");
    }

    private void encodeLong(long value, OutputStream out) throws IOException {
        out.write('i');
        out.write(Long.toString(value).getBytes("US-ASCII"));
        out.write('e');
    }


    private void encodeList(String[] list, OutputStream out) throws IOException {
        out.write('l');
        out.write('l');
        for (String aList : list) {
            encodeString(aList, out);
        }
        out.write('e');
        out.write('e');
    }

    private void encodeBytes(byte[] bytes, OutputStream out) throws IOException {
        out.write(Integer.toString(bytes.length).getBytes("US-ASCII"));
        out.write(':');
        out.write(bytes);
    }

    private void encodeString(String str, OutputStream out) throws IOException {
        encodeBytes(str.getBytes("UTF-8"), out);
    }

    public void encodeMap(Map<String, Object> map, OutputStream out) throws IOException {
        // Sort the map. A generic encoder should sort by key bytes
        SortedMap<String, Object> sortedMap = new TreeMap<>(map);
        out.write('d');
        for (Map.Entry<String, Object> e : sortedMap.entrySet()) {
            encodeString(e.getKey(), out);
            encodeObject(e.getValue(), out);
        }
        out.write('e');
    }

    public byte[] hashPieces(File file, int pieceLength) throws IOException {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new Error("SHA1 not supported");
        }
        InputStream in = new FileInputStream(file);
        ByteArrayOutputStream pieces = new ByteArrayOutputStream();
        byte[] bytes = new byte[pieceLength];
        int pieceByteCount = 0, readCount = in.read(bytes, 0, pieceLength);
        while (readCount != -1) {
            pieceByteCount += readCount;
            sha1.update(bytes, 0, readCount);
            if (pieceByteCount == pieceLength) {
                pieceByteCount = 0;
                pieces.write(sha1.digest());
            }
            readCount = in.read(bytes, 0, pieceLength - pieceByteCount);
        }
        in.close();
        if (pieceByteCount > 0)
            pieces.write(sha1.digest());
        return pieces.toByteArray();
    }
}
