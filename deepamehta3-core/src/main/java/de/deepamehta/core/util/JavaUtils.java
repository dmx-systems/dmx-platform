package de.deepamehta.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.net.URLEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Map;
import java.util.Scanner;



public class JavaUtils {

    private static FileNameMap fileTypeMap = URLConnection.getFileNameMap();

    public static String getFileType(String fileName) {
        String fileType = fileTypeMap.getContentTypeFor(fileName);
        if (fileType != null) {
            return fileType;
        }
        // fallback
        String extension = getExtension(fileName);
        if (extension.equals("mp3")) {
            return "audio/mpeg";
        }
        //
        return null;
    }

    public static String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    // ---

    public static String readTextFile(File file) {
        try {
            return readTextFile(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error wile reading text file " + file, e);
        }
    }

    public static String readTextFile(InputStream in) {
        StringBuilder text = new StringBuilder();
        Scanner scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            text.append(scanner.nextLine() + "\n");
        }
        return text.toString();
    }

    // ---

    public static String createTempDirectory(String prefix) {
        try {
            File f = File.createTempFile(prefix, ".dir");
            String n = f.getAbsolutePath();
            f.delete();
            new File(n).mkdir();
            return n;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // ---

    public static String encodeURIComponent(String uriComp) throws UnsupportedEncodingException {
        return URLEncoder.encode(uriComp, "UTF-8").replaceAll("\\+", "%20");
    }

    // ---

    /* static {
        for (Provider p : Security.getProviders()) {
            System.out.println("### Security Provider " + p);
            for (Provider.Service s : p.getServices()) {
                System.out.println("        " + s);
            }
        }
    } */

    public static String encodeSHA256(String data) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return new String(encodeHex(sha256.digest(data.getBytes())));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error while SHA256 encoding", e);
        }
    }

    private static char[] encodeHex(byte[] data) {
        final String DIGITS = "0123456789abcdef";
        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS.charAt((0xF0 & data[i]) >>> 4);
            out[j++] = DIGITS.charAt(0x0F & data[i]);
        }
        return out;
    }
}
