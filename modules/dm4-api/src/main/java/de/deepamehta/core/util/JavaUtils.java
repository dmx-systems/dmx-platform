package de.deepamehta.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.math.BigInteger;

import java.net.FileNameMap;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Scanner;



/**
 * Generic Java utilities.
 */
public class JavaUtils {



    // === Text ===

    public static String stripHTML(String html) {
        return html.replaceAll("<.*?>", "");    // *? is the reluctant version of the * quantifier (which is greedy)
    }

    public static String times(String str, int times) {
        StringBuilder sb = new StringBuilder(times * str.length());
        for (int i = 0; i < times; i++) {
            sb.append(str);
        }
        return sb.toString();
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

    public static String getFilename(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static String getBasename(String fileName) {
        int i = fileName.lastIndexOf(".");
        if (i == -1) {
            return fileName;
        }
        return fileName.substring(0, i);
    }

    public static String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    public static String stripDriveLetter(String path) {
        return path.replaceFirst("^[A-Z]:", "");
    }

    // ---

    public static File findUnusedFile(File file) {
        String parent = file.getParent();
        String fileName = file.getName();
        String basename = getBasename(fileName);
        String extension = getExtension(fileName);
        int nr = 1;
        while (file.exists()) {
            nr++;
            file = new File(parent, basename + "-" + nr + "." + extension);
        }
        return file;
    }

    // ---

    public static String readTextFile(File file) {
        try {
            return readText(new FileInputStream(file));
        } catch (Exception e) {
            throw new RuntimeException("Reading text file \"" + file + "\" failed", e);
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

    public static String readTextURL(URL url) {
        try {
            return readText(url.openStream());
        } catch (Exception e) {
            throw new RuntimeException("Reading from URL \"" + url + "\" failed", e);
        }
    }

    // ---

    public static String encodeURIComponent(String uriComp) {
        try {
            return URLEncoder.encode(uriComp, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding URI component \"" + uriComp + "\" failed", e);
        }
    }

    public static String decodeURIComponent(String uriComp) {
        try {
            return URLDecoder.decode(uriComp, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Decoding URI component \"" + uriComp + "\" failed", e);
        }
    }



    // === Networking ===

    /**
     * @param   inetAddress     IPv4 or IPv6 address or a machine name, e.g. "172.68.8.12"
     * @param   range           IPv4 or IPv6 address range in CIDR notation, e.g. "172.68.8.0/24"
     */
    public static boolean isInRange(String inetAddress, String range) {
        try {
            String[] r = range.split("/");
            BigInteger networkAddr = inetAddress(r[0]);
            int maskNumber = Integer.parseInt(r[1]);
            InetAddress addr = InetAddress.getByName(inetAddress);
            BigInteger networkMask = networkMask(addr, maskNumber);
            //
            return inetAddress(addr).xor(networkAddr).and(networkMask).equals(BigInteger.ZERO);
        } catch (Exception e) {
            throw new RuntimeException("Checking IP range failed (inetAddress=\"" + inetAddress +
                "\", range=\"" + range + "\"", e);
        }
    }

    // ---

    public static BigInteger inetAddress(String inetAddress) {
        try {
            return inetAddress(InetAddress.getByName(inetAddress));
        } catch (Exception e) {
            throw new RuntimeException("Parsing inet address \"" + inetAddress + "\" failed", e);
        }
    }

    public static BigInteger inetAddress(InetAddress inetAddress) {
        return new BigInteger(1, inetAddress.getAddress());     // signum=1 (positive)
    }

    // ---

    public static BigInteger networkMask(InetAddress addr, int maskNumber) {
        if (addr instanceof Inet4Address) {
            return networkMask(maskNumber, 32);
        } else if (addr instanceof Inet6Address) {
            return networkMask(maskNumber, 128);
        } else {
            throw new RuntimeException("Unexpected InetAddress object: " + addr.getClass().getName());
        }
    }

    public static BigInteger networkMask(int maskNumber, int size) {
        String networkMask = times("1", maskNumber) + times("0", size - maskNumber);
        return new BigInteger(networkMask, 2);      // radix=2 (binary)
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
            throw new RuntimeException("SHA256 encoding failed", e);
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
