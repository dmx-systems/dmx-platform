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



/**
 * Generic Java utilities.
 */
public class JavaUtils {



    // === Text ===

    public static String stripHTML(String html) {
        return html.replaceAll("<.*?>", "");    // *? is the reluctant version of the * quantifier (which is greedy)
    }



    // === Files ===

    private static FileNameMap fileTypeMap = URLConnection.getFileNameMap();

    public static String getFileType(String fileName) {
        String extension = getExtension(fileName);
        if (!extension.equals("avi")) {
            // Note: for .avi Sun's file type map returns strange media type "application/x-troff-msvideo"
            String fileType = fileTypeMap.getContentTypeFor(fileName);
            if (fileType != null) {
                return fileType;
            }
        }
        // fallback
        if (extension.equals("mp3")) {
            return "audio/mpeg";
        } else if (extension.equals("mp4")) {
            return "video/mp4";
        } else if (extension.equals("avi")) {
            return "video/avi";
        } else if (extension.equals("wmv")) {
            return "video/x-ms-wmv";
        } else if (extension.equals("flv")) {
            return "video/x-flv";
        } else if (extension.equals("svg")) {
            return "image/svg+xml";
        }
        // TODO: use a system property instead a hardcoded list
        //
        return null;
    }

    public static String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    // ---

    public static String readTextFile(File file) {
        try {
            return readText(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error wile reading text file " + file, e);
        }
    }

    public static String readText(InputStream in) {
        StringBuilder text = new StringBuilder();
        Scanner scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            text.append(scanner.nextLine() + "\n");
        }
        return text.toString();
    }

    // ---

    public static File createTempDirectory(String prefix) {
        try {
            File f = File.createTempFile(prefix, ".dir");
            String n = f.getAbsolutePath();
            f.delete();
            f = new File(n);
            f.mkdir();
            return f;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }



    // === URLs ===

    public static String encodeURIComponent(String uriComp) {
        try {
            return URLEncoder.encode(uriComp, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding of URI component \"" + uriComp + "\" failed", e);
        }
    }



    // === Networking ===

    /**
     * @param   inetAddress     e.g. "172.68.8.12"
     * @param   range           e.g. "172.68.8.0/24"
     */
    public static boolean isInRange(String inetAddress, String range) {
        String[] r = range.split("/");
        int networkAddr = inetAddress(r[0]);
        int networkMask = networkMask(Integer.parseInt(r[1]));
        //
        return ((inetAddress(inetAddress) ^ networkAddr) & networkMask) == 0;
    }

    public static int inetAddress(String inetAddress) {
        String[] a = inetAddress.split("\\.");
        return (Integer.parseInt(a[0]) << 24) +
               (Integer.parseInt(a[1]) << 16) +
               (Integer.parseInt(a[2]) << 8) +
                Integer.parseInt(a[3]);
    }

    public static int networkMask(int maskNr) {
        return -1 << 32 - maskNr;
    }



    // === Encryption ===

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
